package com.petlife.repository;

import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record GroomerManageRequest(
        @NotNull Integer groomerId,
        String displayName,
        String intro,
        @Min(0) Integer seniorityYears,
        Boolean isBookable,
        @NotEmpty List<Integer> beautyIds) {
}
