package com.petlife.repository;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record BeautyItemManageRequest(
        @NotBlank String itemName,
        String itemDescription,
        @NotNull @Min(1) Integer durationSlots,
        Boolean isActive,
        @Valid @NotEmpty List<BeautyPriceLineRequest> prices) {
}
