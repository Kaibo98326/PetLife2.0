package com.petlife.repository;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record RescheduleAppointmentRequest(
        @NotNull LocalDate appointDate,
        @NotNull Integer startSlotId) {
}
