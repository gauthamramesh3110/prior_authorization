package com.lifeforce.payer.request.dto;

import java.util.UUID;

public record AuthorizationSubmissionResponse(
        UUID requestId,
        SubmissionStatus responseStatus,
        String message
) {
}
