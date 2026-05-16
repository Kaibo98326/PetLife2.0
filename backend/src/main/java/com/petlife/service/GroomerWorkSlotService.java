package com.petlife.service;

import com.petlife.config.ApiException;
import com.petlife.config.BeautyConstants;
import com.petlife.repository.GroomerDaySlotLineResponse;
import com.petlife.repository.GroomerDaySlotResponse;
import com.petlife.repository.GroomerWorkSlotResponse;
import com.petlife.model.BeautyTimeSlot;
import com.petlife.model.GroomerSchedule;
import com.petlife.model.GroomerWorkSlot;
import com.petlife.repository.GroomerScheduleRepository;
import com.petlife.repository.BeautyTimeSlotRepository;
import com.petlife.repository.GroomerWorkSlotRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class GroomerWorkSlotService {

    private static final String SCHEDULE_NOT_SET = "未排班";
    private static final String SLOT_AVAILABLE = "可預約";
    private static final String SLOT_UNAVAILABLE = "不可預約";
    private static final String LEGACY_WORK_SLOT_BLOCKED = "手動封鎖";

    private final GroomerWorkSlotRepository workSlotRepository;
    private final GroomerScheduleRepository scheduleRepository;
    private final BeautyTimeSlotRepository slotRepository;

    public GroomerWorkSlotService(GroomerWorkSlotRepository workSlotRepository,
            GroomerScheduleRepository scheduleRepository,
            BeautyTimeSlotRepository slotRepository) {
        this.workSlotRepository = workSlotRepository;
        this.scheduleRepository = scheduleRepository;
        this.slotRepository = slotRepository;
    }

    public List<GroomerWorkSlotResponse> findByGroomerAndDate(Integer groomerId, LocalDate date) {
        return workSlotRepository.findByGroomerIdAndWorkDate(groomerId, date)
                .stream()
                .map(BeautyMapper::workSlot)
                .toList();
    }

    public GroomerDaySlotResponse getDaySlotStatus(Integer groomerId, LocalDate workDate) {
        if (groomerId == null) {
            throw ApiException.badRequest("美容師編號不可為空");
        }
        if (workDate == null) {
            throw ApiException.badRequest("日期不可為空");
        }

        GroomerSchedule schedule = scheduleRepository.findByGroomerIdAndWorkDate(groomerId, workDate).orElse(null);
        String scheduleStatus = schedule == null ? SCHEDULE_NOT_SET : schedule.getScheduleStatus();
        String scheduleNote = schedule == null ? null : schedule.getNote();

        Map<Integer, GroomerWorkSlot> workSlotBySlotId = workSlotRepository.findByGroomerIdAndWorkDate(groomerId,
                workDate)
                .stream()
                .collect(Collectors.toMap(GroomerWorkSlot::getSlotId, Function.identity()));

        List<GroomerDaySlotLineResponse> slots = slotRepository.findAllByOrderBySortOrderAsc()
                .stream()
                .map(slot -> toDaySlotLine(slot, scheduleStatus, workSlotBySlotId.get(slot.getSlotId())))
                .toList();

        return new GroomerDaySlotResponse(groomerId, workDate, scheduleStatus, scheduleNote, slots);
    }

    private GroomerDaySlotLineResponse toDaySlotLine(BeautyTimeSlot slot, String scheduleStatus,
            GroomerWorkSlot workSlot) {
        String slotStatus = resolveSlotStatus(slot, scheduleStatus, workSlot);

        return new GroomerDaySlotLineResponse(
                slot.getSlotId(),
                slot.getSlotName(),
                slot.getStartTime().toString(),
                slot.getEndTime().toString(),
                slot.getSortOrder(),
                slot.getIsBookable(),
                slotStatus,
                workSlot == null ? null : workSlot.getAppointmentId(),
                workSlot == null ? null : workSlot.getWorkSlotId(),
                workSlot == null ? null : workSlot.getNote());
    }

    private String resolveSlotStatus(BeautyTimeSlot slot, String scheduleStatus, GroomerWorkSlot workSlot) {
        if (!Boolean.TRUE.equals(slot.getIsBookable())) {
            return SLOT_UNAVAILABLE;
        }
        if (workSlot != null) {
            return LEGACY_WORK_SLOT_BLOCKED.equals(workSlot.getWorkSlotStatus())
                    ? BeautyConstants.WORK_SLOT_SCHEDULE_CLOSED
                    : workSlot.getWorkSlotStatus();
        }
        if (!BeautyConstants.SCHEDULE_WORK.equals(scheduleStatus)) {
            return SLOT_UNAVAILABLE;
        }
        return SLOT_AVAILABLE;
    }
}
