package com.petlife.service;

import com.petlife.config.ApiException;
import com.petlife.config.BeautyConstants;
import com.petlife.model.BeautyTimeSlot;
import com.petlife.model.GroomerWorkSlot;
import com.petlife.repository.BeautyTimeSlotRepository;
import com.petlife.repository.GroomerMonthlyScheduleDayResponse;
import com.petlife.repository.GroomerMonthlyScheduleResponse;
import com.petlife.repository.GroomerScheduleRequest;
import com.petlife.repository.GroomerScheduleResponse;
import com.petlife.repository.UpdateDayScheduleSlotsRequest;
import com.petlife.model.GroomerSchedule;
import com.petlife.repository.GroomerProfileRepository;
import com.petlife.repository.GroomerScheduleRepository;
import com.petlife.repository.GroomerWorkSlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class GroomerScheduleService {

    private static final String SCHEDULE_NOT_SET = "未排班";
    private static final String LEGACY_WORK_SLOT_BLOCKED = "手動封鎖";

    private final GroomerScheduleRepository scheduleRepository;
    private final GroomerProfileRepository groomerRepository;
    private final GroomerWorkSlotRepository workSlotRepository;
    private final BeautyTimeSlotRepository slotRepository;

    public GroomerScheduleService(GroomerScheduleRepository scheduleRepository,
            GroomerProfileRepository groomerRepository,
            GroomerWorkSlotRepository workSlotRepository,
            BeautyTimeSlotRepository slotRepository) {
        this.scheduleRepository = scheduleRepository;
        this.groomerRepository = groomerRepository;
        this.workSlotRepository = workSlotRepository;
        this.slotRepository = slotRepository;
    }

    public List<GroomerScheduleResponse> findSchedules(LocalDate startDate, LocalDate endDate) {
        return scheduleRepository.findByWorkDateBetweenOrderByWorkDateAscGroomerIdAsc(startDate, endDate)
                .stream()
                .map(BeautyMapper::schedule)
                .toList();
    }

    public GroomerMonthlyScheduleResponse getMonthlySchedule(Integer groomerId, YearMonth yearMonth) {
        if (groomerId == null) {
            throw ApiException.badRequest("美容師編號不可為空");
        }
        if (yearMonth == null) {
            throw ApiException.badRequest("年月不可為空");
        }

        groomerRepository.findById(groomerId)
                .orElseThrow(() -> ApiException.notFound("找不到美容師"));

        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        int bookableSlotCount = (int) slotRepository.findAllByOrderBySortOrderAsc().stream()
                .filter(BeautyTimeSlot::getIsBookable)
                .count();

        Map<LocalDate, GroomerSchedule> scheduleByDate = scheduleRepository
                .findByGroomerIdAndWorkDateBetweenOrderByWorkDateAsc(groomerId, startDate, endDate)
                .stream()
                .collect(Collectors.toMap(GroomerSchedule::getWorkDate, Function.identity()));

        Map<LocalDate, List<GroomerWorkSlot>> workSlotsByDate = workSlotRepository
                .findByGroomerIdAndWorkDateBetween(groomerId, startDate, endDate)
                .stream()
                .collect(Collectors.groupingBy(GroomerWorkSlot::getWorkDate));

        List<GroomerMonthlyScheduleDayResponse> days = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            GroomerSchedule schedule = scheduleByDate.get(date);
            String scheduleStatus = schedule == null ? SCHEDULE_NOT_SET : schedule.getScheduleStatus();
            List<GroomerWorkSlot> workSlots = workSlotsByDate.getOrDefault(date, List.of());
            int bookedCount = countWorkSlots(workSlots, BeautyConstants.WORK_SLOT_APPOINTMENT);
            int scheduleClosedCount = countWorkSlots(workSlots, BeautyConstants.WORK_SLOT_SCHEDULE_CLOSED);
            int availableCount = BeautyConstants.SCHEDULE_WORK.equals(scheduleStatus)
                    ? Math.max(0, bookableSlotCount - countDistinctSlotIds(workSlots))
                    : 0;

            days.add(new GroomerMonthlyScheduleDayResponse(date, scheduleStatus, bookedCount,
                    scheduleClosedCount, availableCount));
        }

        return new GroomerMonthlyScheduleResponse(groomerId, yearMonth.toString(), days);
    }

    @Transactional
    public GroomerScheduleResponse upsertSchedule(GroomerScheduleRequest req) {
        if (req == null || req.groomerId() == null || req.workDate() == null) {
            throw ApiException.badRequest("排班資料不可為空");
        }

        groomerRepository.findById(req.groomerId())
                .orElseThrow(() -> ApiException.notFound("找不到美容師"));

        String status = req.scheduleStatus() == null ? BeautyConstants.SCHEDULE_WORK : req.scheduleStatus();

        if (!BeautyConstants.SCHEDULE_WORK.equals(status) && !BeautyConstants.SCHEDULE_OFF.equals(status)) {
            throw ApiException.badRequest("排班狀態只能是 上班 或 休假");
        }

        GroomerSchedule schedule = scheduleRepository.findByGroomerIdAndWorkDate(req.groomerId(), req.workDate())
                .orElseGet(GroomerSchedule::new);

        schedule.setGroomerId(req.groomerId());
        schedule.setWorkDate(req.workDate());
        schedule.setScheduleStatus(status);
        schedule.setNote(req.note());
        return BeautyMapper.schedule(scheduleRepository.save(schedule));
    }

    @Transactional
    public GroomerScheduleResponse updateDayScheduleSlots(UpdateDayScheduleSlotsRequest req) {
        if (req == null || req.groomerId() == null || req.workDate() == null) {
            throw ApiException.badRequest("排班資料不可為空");
        }

        groomerRepository.findById(req.groomerId())
                .orElseThrow(() -> ApiException.notFound("找不到美容師"));

        String status = req.scheduleStatus() == null ? BeautyConstants.SCHEDULE_WORK : req.scheduleStatus();
        if (!BeautyConstants.SCHEDULE_WORK.equals(status) && !BeautyConstants.SCHEDULE_OFF.equals(status)) {
            throw ApiException.badRequest("排班狀態只能是 上班 或 休假");
        }

        List<GroomerWorkSlot> workSlots = workSlotRepository.findByGroomerIdAndWorkDate(req.groomerId(),
                req.workDate());

        if (BeautyConstants.SCHEDULE_OFF.equals(status) && hasAppointmentSlot(workSlots)) {
            throw ApiException.badRequest("當日已有預約，不可改為休假");
        }

        GroomerSchedule schedule = scheduleRepository.findByGroomerIdAndWorkDate(req.groomerId(), req.workDate())
                .orElseGet(GroomerSchedule::new);
        schedule.setGroomerId(req.groomerId());
        schedule.setWorkDate(req.workDate());
        schedule.setScheduleStatus(status);
        schedule.setNote(req.note());
        schedule = scheduleRepository.save(schedule);

        if (BeautyConstants.SCHEDULE_OFF.equals(status)) {
            deleteScheduleClosedSlots(workSlots);
            return BeautyMapper.schedule(schedule);
        }

        syncScheduleClosedSlots(req, workSlots);
        return BeautyMapper.schedule(schedule);
    }

    private void syncScheduleClosedSlots(UpdateDayScheduleSlotsRequest req, List<GroomerWorkSlot> workSlots) {
        List<BeautyTimeSlot> bookableSlots = slotRepository.findByIsBookableTrueOrderBySortOrderAsc();
        Set<Integer> allBookableSlotIds = bookableSlots.stream()
                .map(BeautyTimeSlot::getSlotId)
                .collect(Collectors.toSet());
        Set<Integer> openSlotIds = req.bookableSlotIds() == null
                ? allBookableSlotIds
                : req.bookableSlotIds().stream().collect(Collectors.toSet());

        if (!allBookableSlotIds.containsAll(openSlotIds)) {
            throw ApiException.badRequest("排班時段包含不可預約或不存在的時段");
        }

        Map<Integer, GroomerWorkSlot> workSlotBySlotId = workSlots.stream()
                .collect(Collectors.toMap(GroomerWorkSlot::getSlotId, Function.identity()));
        List<GroomerWorkSlot> closingSlots = new ArrayList<>();

        for (BeautyTimeSlot slot : bookableSlots) {
            GroomerWorkSlot workSlot = workSlotBySlotId.get(slot.getSlotId());
            boolean shouldOpen = openSlotIds.contains(slot.getSlotId());

            if (shouldOpen) {
                if (workSlot != null && isScheduleClosedOrLegacyBlocked(workSlot)) {
                    workSlotRepository.delete(workSlot);
                }
                continue;
            }

            if (workSlot == null) {
                GroomerWorkSlot closingSlot = new GroomerWorkSlot();
                closingSlot.setGroomerId(req.groomerId());
                closingSlot.setWorkDate(req.workDate());
                closingSlot.setSlotId(slot.getSlotId());
                closingSlot.setAppointmentId(null);
                closingSlot.setWorkSlotStatus(BeautyConstants.WORK_SLOT_SCHEDULE_CLOSED);
                closingSlot.setNote("排班未開放");
                closingSlots.add(closingSlot);
                continue;
            }

            if (BeautyConstants.WORK_SLOT_APPOINTMENT.equals(workSlot.getWorkSlotStatus())) {
                throw ApiException.badRequest("已有預約的時段不可從排班中關閉");
            }
            if (LEGACY_WORK_SLOT_BLOCKED.equals(workSlot.getWorkSlotStatus())) {
                workSlot.setWorkSlotStatus(BeautyConstants.WORK_SLOT_SCHEDULE_CLOSED);
                workSlot.setNote("排班關閉");
                closingSlots.add(workSlot);
            }
        }

        if (!closingSlots.isEmpty()) {
            workSlotRepository.saveAll(closingSlots);
        }
    }

    private void deleteScheduleClosedSlots(List<GroomerWorkSlot> workSlots) {
        List<GroomerWorkSlot> scheduleClosedSlots = workSlots.stream()
                .filter(workSlot -> BeautyConstants.WORK_SLOT_SCHEDULE_CLOSED.equals(workSlot.getWorkSlotStatus()))
                .toList();
        if (!scheduleClosedSlots.isEmpty()) {
            workSlotRepository.deleteAll(scheduleClosedSlots);
        }
    }

    private boolean hasAppointmentSlot(List<GroomerWorkSlot> workSlots) {
        return workSlots.stream()
                .anyMatch(workSlot -> BeautyConstants.WORK_SLOT_APPOINTMENT.equals(workSlot.getWorkSlotStatus()));
    }

    private int countWorkSlots(List<GroomerWorkSlot> workSlots, String status) {
        return (int) workSlots.stream()
                .filter(workSlot -> status.equals(workSlot.getWorkSlotStatus()))
                .count();
    }

    private boolean isScheduleClosedOrLegacyBlocked(GroomerWorkSlot workSlot) {
        return BeautyConstants.WORK_SLOT_SCHEDULE_CLOSED.equals(workSlot.getWorkSlotStatus())
                || LEGACY_WORK_SLOT_BLOCKED.equals(workSlot.getWorkSlotStatus());
    }

    private int countDistinctSlotIds(List<GroomerWorkSlot> workSlots) {
        return (int) workSlots.stream()
                .map(GroomerWorkSlot::getSlotId)
                .distinct()
                .count();
    }
}
