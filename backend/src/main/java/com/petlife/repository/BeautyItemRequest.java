package com.petlife.repository;

import jakarta.validation.constraints.*;

public record BeautyItemRequest(@NotBlank String itemName, String itemDescription, String imageUrl,
        @NotNull @Min(1) Integer durationSlots, Boolean isActive) {
}
