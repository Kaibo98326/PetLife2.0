package com.petlife.service;

import com.petlife.config.ApiException;
import com.petlife.config.BeautyConstants;
import com.petlife.repository.GroomerScheduleRequest;
import com.petlife.repository.GroomerScheduleResponse;
import com.petlife.model.GroomerSchedule;
import com.petlife.repository.GroomerProfileRepository;
import com.petlife.repository.GroomerScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class GroomerScheduleService {

    private final GroomerScheduleRepository scheduleRepository;
    private final GroomerProfileRepository groomerRepository;

    public GroomerScheduleService(GroomerScheduleRepository scheduleRepository,
            GroomerProfileRepository groomerRepository) {
        this.scheduleRepository = scheduleRepository;
        this.groomerRepository = groomerRepository;
    }

    public List<GroomerScheduleResponse> findSchedules(LocalDate startDate, LocalDate endDate) {
        return scheduleRepository.findByWorkDateBetweenOrderByWorkDateAscGroomerIdAsc(startDate, endDate)
                .stream()
                .map(BeautyMapper::schedule)
                .toList();
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
}
