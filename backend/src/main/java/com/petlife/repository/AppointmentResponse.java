package com.petlife.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AppointmentResponse(
        Integer appointmentId, Integer memberId, Integer petId, String petName, Integer groomerId, String groomerName,
        LocalDate appointDate, Integer startSlotId, String startSlotName, String petSizeSnapshot,
        Integer totalSlots, BigDecimal totalAmount, String appointmentStatus, String contactNote, String cancelReason,
        LocalDateTime createdAt, List<AppointmentDetailLineResponse> details) {
}
