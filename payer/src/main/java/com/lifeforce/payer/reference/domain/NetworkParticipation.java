package com.lifeforce.payer.reference.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity(name = "network_participation")
@IdClass(NetworkParticipationId.class)
public class NetworkParticipation {
    @Id
    private UUID payerId;

    @Id
    private UUID organizationId;

    private boolean inNetwork;

    @Id
    private Instant effectiveFrom;

    private Instant effectiveTo;
}
