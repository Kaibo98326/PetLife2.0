package com.petlife.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AppointmentResponse(
        Integer appointmentId, Integer memberId, Integer petId, String petName, Integer groomerId, String groomerName,
        LocalDate appointDate, Integer startSlotId, String startSlotName, String petSizeSnapshot,
        Integer totalSlots, BigDecimal totalAmount, String appointmentStatus, String contactNote, String cancelReason,
        Boolean canCancel, String cancelUnavailableReason, Boolean canReschedule, String rescheduleUnavailableReason,
        LocalDateTime createdAt,
        List<AppointmentDetailLineResponse> details) {
}
