package com.petlife.service;

import com.petlife.config.ApiException;
import com.petlife.config.BeautyConstants;
import com.petlife.repository.UpdateAppointmentStatusRequest;
import com.petlife.repository.AppointmentResponse;
import com.petlife.model.BeautyAppointment;
import com.petlife.repository.BeautyAppointmentRepository;
import com.petlife.repository.GroomerWorkSlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class AdminBeautyAppointmentService {

    private final BeautyAppointmentRepository appointmentRepository;
    private final GroomerWorkSlotRepository workSlotRepository;
    private final BeautyAppointmentService appointmentService;

    public AdminBeautyAppointmentService(
            BeautyAppointmentRepository appointmentRepository,
            GroomerWorkSlotRepository workSlotRepository,
            BeautyAppointmentService appointmentService) {
        this.appointmentRepository = appointmentRepository;
        this.workSlotRepository = workSlotRepository;
        this.appointmentService = appointmentService;
    }

    public List<AppointmentResponse> findAppointments(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null) {
            return appointmentRepository
                    .findByAppointDateBetweenOrderByAppointDateDescAppointmentIdDesc(startDate, endDate)
                    .stream()
                    .map(appointmentService::toResponse)
                    .toList();
        }

        return appointmentRepository.findAllByOrderByAppointDateDescAppointmentIdDesc()
                .stream()
                .map(appointmentService::toResponse)
                .toList();
    }

    public AppointmentResponse getAppointment(Integer id) {
        BeautyAppointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("找不到預約單"));
        return appointmentService.toResponse(appointment);
    }

    @Transactional
    public AppointmentResponse updateStatus(Integer id, UpdateAppointmentStatusRequest req) {
        BeautyAppointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("找不到預約單"));

        String currentStatus = appointment.getAppointmentStatus();
        String targetStatus = req.status();

        if (!isValid(currentStatus, targetStatus)) {
            throw ApiException.badRequest("不合法的狀態異動：" + currentStatus + " -> " + targetStatus);
        }

        appointment.setAppointmentStatus(targetStatus);

        if (BeautyConstants.APPOINTMENT_CANCELLED.equals(targetStatus)) {
            appointment.setCancelReason(req.cancelReason());
            workSlotRepository.deleteByAppointmentIdAndWorkSlotStatus(appointment.getAppointmentId(),
                    BeautyConstants.WORK_SLOT_APPOINTMENT);
        }

        appointmentRepository.save(appointment);
        return appointmentService.toResponse(appointment);
    }

    private boolean isValid(String from, String to) {
        if (BeautyConstants.APPOINTMENT_PENDING.equals(from)) {
            return BeautyConstants.APPOINTMENT_CONFIRMED.equals(to)
                    || BeautyConstants.APPOINTMENT_CANCELLED.equals(to);
        }

        if (BeautyConstants.APPOINTMENT_CONFIRMED.equals(from)) {
            return BeautyConstants.APPOINTMENT_DONE.equals(to)
                    || BeautyConstants.APPOINTMENT_CANCELLED.equals(to)
                    || BeautyConstants.APPOINTMENT_NO_SHOW.equals(to);
        }

        return false;
    }
}
