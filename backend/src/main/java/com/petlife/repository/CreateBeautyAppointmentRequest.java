package com.petlife.repository;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

public record CreateBeautyAppointmentRequest(
        @NotNull Integer petId,
        @NotNull Integer groomerId,
        @NotNull LocalDate appointDate,
        @NotNull Integer startSlotId,
        @NotEmpty List<Integer> beautyIds,
        String contactNote) {
}
