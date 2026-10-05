package com.lifeforce.payer.reference.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;

import java.util.UUID;

@Getter
@Entity(name = "provider")
public class Provider {
    @Id
    private UUID id;

    private UUID organizationId;

    private String name;
}
