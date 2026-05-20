package com.petlife.repository;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record GroomerServiceRequest(@NotNull List<Integer> beautyIds) {
}
