package com.lifeforce.payer.request.domain;

import java.util.Date;

public record RequestedService(
        String code,
        String codeSystem,
        String description,
        Date requestedDate,
        Integer quantity
) {}
