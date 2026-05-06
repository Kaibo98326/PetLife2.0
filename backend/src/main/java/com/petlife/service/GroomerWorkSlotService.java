package com.petlife.service;

import com.petlife.config.ApiException;
import com.petlife.config.BeautyConstants;
import com.petlife.repository.BlockWorkSlotRequest;
import com.petlife.model.GroomerWorkSlot;
import com.petlife.repository.GroomerScheduleRepository;
import com.petlife.repository.GroomerWorkSlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class GroomerWorkSlotService {

    private final GroomerWorkSlotRepository workSlotRepository;
    private final GroomerScheduleRepository scheduleRepository;

    public GroomerWorkSlotService(GroomerWorkSlotRepository workSlotRepository,
            GroomerScheduleRepository scheduleRepository) {
        this.workSlotRepository = workSlotRepository;
        this.scheduleRepository = scheduleRepository;
    }

    public List<GroomerWorkSlot> findByGroomerAndDate(Integer groomerId, LocalDate date) {
        return workSlotRepository.findByGroomerIdAndWorkDate(groomerId, date);
    }

    @Transactional
    public List<GroomerWorkSlot> blockSlots(BlockWorkSlotRequest req) {
        scheduleRepository.findByGroomerIdAndWorkDate(req.groomerId(), req.workDate())
                .orElseThrow(() -> ApiException.badRequest("該美容師當日未排班"));

        if (!workSlotRepository.findByGroomerIdAndWorkDateAndSlotIdIn(req.groomerId(), req.workDate(), req.slotIds())
                .isEmpty()) {
            throw ApiException.badRequest("部分時段已被占用");
        }

        return workSlotRepository.saveAll(req.slotIds().stream().distinct().map(slotId -> {
            GroomerWorkSlot workSlot = new GroomerWorkSlot();
            workSlot.setGroomerId(req.groomerId());
            workSlot.setWorkDate(req.workDate());
            workSlot.setSlotId(slotId);
            workSlot.setAppointmentId(null);
            workSlot.setWorkSlotStatus(BeautyConstants.WORK_SLOT_BLOCKED);
            workSlot.setNote(req.note());
            return workSlot;
        }).toList());
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
}
