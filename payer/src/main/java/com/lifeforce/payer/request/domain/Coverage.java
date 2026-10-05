package com.lifeforce.payer.request.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity(name = "coverage")
public class Coverage {
    @Id
    private UUID id;

    private UUID patientId;

    private UUID planId;

    private Instant start;

    private Instant end;
}
