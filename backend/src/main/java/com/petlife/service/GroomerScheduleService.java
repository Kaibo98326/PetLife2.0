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
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class GroomerScheduleService {

    private static final String SCHEDULE_NOT_SET = "未排班";

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
            int blockedCount = countWorkSlots(workSlots, BeautyConstants.WORK_SLOT_BLOCKED);
            int availableCount = BeautyConstants.SCHEDULE_WORK.equals(scheduleStatus)
                    ? Math.max(0, bookableSlotCount - bookedCount - blockedCount)
                    : 0;

            days.add(new GroomerMonthlyScheduleDayResponse(date, scheduleStatus, bookedCount, blockedCount,
                    availableCount));
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

    private int countWorkSlots(List<GroomerWorkSlot> workSlots, String status) {
        return (int) workSlots.stream()
                .filter(workSlot -> status.equals(workSlot.getWorkSlotStatus()))
                .count();
    }
}
