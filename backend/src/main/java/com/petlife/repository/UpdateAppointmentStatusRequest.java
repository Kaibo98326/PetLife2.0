package com.petlife.repository;

import jakarta.validation.constraints.NotBlank;

public record UpdateAppointmentStatusRequest(@NotBlank String status, String cancelReason) {
}
