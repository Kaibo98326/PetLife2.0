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
        return findAppointments(startDate, endDate, null, null);
    }

    public List<AppointmentResponse> findAppointments(LocalDate startDate, LocalDate endDate, String status,
            Integer groomerId) {
        validateSearchRequest(startDate, endDate, status);

        return appointmentRepository.searchAdminAppointments(startDate, endDate, normalizeStatus(status), groomerId)
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
        if (req == null || req.status() == null || req.status().isBlank()) {
            throw ApiException.badRequest("預約狀態不可為空");
        }

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

    private void validateSearchRequest(LocalDate startDate, LocalDate endDate, String status) {
        if ((startDate == null) != (endDate == null)) {
            throw ApiException.badRequest("開始日期與結束日期必須同時提供");
        }
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw ApiException.badRequest("開始日期不可晚於結束日期");
        }
        if (normalizeStatus(status) != null && !isKnownStatus(normalizeStatus(status))) {
            throw ApiException.badRequest("不合法的預約狀態");
        }
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return status.trim();
    }

    private boolean isKnownStatus(String status) {
        return BeautyConstants.APPOINTMENT_PENDING.equals(status)
                || BeautyConstants.APPOINTMENT_CONFIRMED.equals(status)
                || BeautyConstants.APPOINTMENT_DONE.equals(status)
                || BeautyConstants.APPOINTMENT_CANCELLED.equals(status)
                || BeautyConstants.APPOINTMENT_NO_SHOW.equals(status);
    }
}
