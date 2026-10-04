package com.lifeforce.payer.request.dto;

import java.util.UUID;

public record HttpAuthorizationResponse(
        UUID requestId,
        ResponseStatus responseStatus,
        String message
) {
}
