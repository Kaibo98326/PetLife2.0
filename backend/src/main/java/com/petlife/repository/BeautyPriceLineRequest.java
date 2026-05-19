package com.petlife.repository;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BeautyPriceLineRequest(
        @NotBlank String petSize,
        @NotNull @DecimalMin("0.0") BigDecimal itemPrice,
        Boolean isActive) {
}
