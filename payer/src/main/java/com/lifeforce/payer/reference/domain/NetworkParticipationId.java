package com.lifeforce.payer.reference.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Getter
@EqualsAndHashCode
public class NetworkParticipationId implements Serializable {
    private UUID payerId;

    private UUID organizationId;

    private Instant effectiveFrom;
}
