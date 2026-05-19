package com.petlife.repository;

import jakarta.validation.constraints.*;

public record GroomerProfileRequest(@NotNull Integer groomerId, String displayName, String intro,
        @Min(0) Integer seniorityYears, Boolean isBookable) {
}
