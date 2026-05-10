package com.petlife.service;

import com.petlife.config.ApiException;
import com.petlife.config.BeautyConstants;
import com.petlife.repository.BlockWorkSlotRequest;
import com.petlife.repository.GroomerWorkSlotResponse;
import com.petlife.model.BeautyTimeSlot;
import com.petlife.model.GroomerSchedule;
import com.petlife.model.GroomerWorkSlot;
import com.petlife.repository.GroomerScheduleRepository;
import com.petlife.repository.BeautyTimeSlotRepository;
import com.petlife.repository.GroomerWorkSlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
public class GroomerWorkSlotService {

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

    @Transactional
    public List<GroomerWorkSlotResponse> blockSlots(BlockWorkSlotRequest req) {
        validateBlockRequest(req);

        GroomerSchedule schedule = scheduleRepository.findByGroomerIdAndWorkDate(req.groomerId(), req.workDate())
                .orElseThrow(() -> ApiException.badRequest("該美容師當日未排班"));

        if (!BeautyConstants.SCHEDULE_WORK.equals(schedule.getScheduleStatus())) {
            throw ApiException.badRequest("該美容師當日不是上班狀態");
        }

        List<Integer> slotIds = req.slotIds().stream().distinct().toList();
        List<BeautyTimeSlot> slots = slotRepository.findAllById(slotIds);

        if (slots.size() != slotIds.size()) {
            throw ApiException.badRequest("部分時段不存在");
        }

        if (slots.stream().anyMatch(slot -> !Boolean.TRUE.equals(slot.getIsBookable()))) {
            throw ApiException.badRequest("不可封鎖不可預約時段");
        }

        if (!workSlotRepository.findByGroomerIdAndWorkDateAndSlotIdIn(req.groomerId(), req.workDate(), slotIds)
                .isEmpty()) {
            throw ApiException.badRequest("部分時段已被占用");
        }

        return workSlotRepository.saveAll(slotIds.stream().map(slotId -> {
                    GroomerWorkSlot workSlot = new GroomerWorkSlot();
                    workSlot.setGroomerId(req.groomerId());
                    workSlot.setWorkDate(req.workDate());
                    workSlot.setSlotId(slotId);
                    workSlot.setAppointmentId(null);
                    workSlot.setWorkSlotStatus(BeautyConstants.WORK_SLOT_BLOCKED);
                    workSlot.setNote(req.note());
                    return workSlot;
                }).toList())
                .stream()
                .map(BeautyMapper::workSlot)
                .toList();
    }

    @Transactional
    public void deleteBlock(Integer id) {
        GroomerWorkSlot workSlot = workSlotRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("找不到時段占用資料"));

        if (!BeautyConstants.WORK_SLOT_BLOCKED.equals(workSlot.getWorkSlotStatus())) {
            throw ApiException.badRequest("預約占用不可從此 API 刪除");
        }

        workSlotRepository.delete(workSlot);
    }

    private void validateBlockRequest(BlockWorkSlotRequest req) {
        if (req == null) {
            throw ApiException.badRequest("封鎖時段資料不可為空");
        }
        if (req.groomerId() == null) {
            throw ApiException.badRequest("美容師編號不可為空");
        }
        if (req.workDate() == null) {
            throw ApiException.badRequest("日期不可為空");
        }
        if (req.slotIds() == null || req.slotIds().isEmpty()) {
            throw ApiException.badRequest("至少需選擇一個時段");
        }
        if (req.slotIds().stream().anyMatch(Objects::isNull)) {
            throw ApiException.badRequest("時段編號不可為空");
        }
    }
}
