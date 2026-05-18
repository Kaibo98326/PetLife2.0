package com.petlife.repository;

import jakarta.validation.constraints.NotNull;

public record UpdateBeautyItemStatusRequest(@NotNull Boolean isActive) {
}
