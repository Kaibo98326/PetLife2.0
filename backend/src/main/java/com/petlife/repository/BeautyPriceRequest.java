package com.petlife.repository;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record BeautyPriceRequest(@NotBlank String petSize, @NotNull @DecimalMin("0.0") BigDecimal itemPrice,
        Boolean isActive) {
}
