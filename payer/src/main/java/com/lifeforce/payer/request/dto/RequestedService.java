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
    public com.lifeforce.payer.request.domain.RequestedService toDomain() {
        return new com.lifeforce.payer.request.domain.RequestedService(code, codeSystem, description, requestedDate, quantity);
    }

    public static RequestedService from(com.lifeforce.payer.request.domain.RequestedService requestedService) {
        return new RequestedService(requestedService.code(), requestedService.codeSystem(), requestedService.description(), requestedService.requestedDate(), requestedService.quantity());
    }
}
