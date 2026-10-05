package com.lifeforce.payer.request.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.Date;

public record RequestedService(
        @NotBlank String code,
        String codeSystem,
        String description,
        @NotNull Date requestedDate,
        @NotNull @Positive Integer quantity
) {
}
