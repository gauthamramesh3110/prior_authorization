package com.lifeforce.payer.plan.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

import java.util.UUID;

@Entity
public class Policy {
    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    ReviewMode reviewMode;

    @Enumerated(EnumType.STRING)
    Match match;
}
