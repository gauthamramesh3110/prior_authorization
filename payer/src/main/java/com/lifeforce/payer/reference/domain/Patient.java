package com.lifeforce.payer.reference.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;

import java.util.UUID;

@Getter
@Entity(name = "patient")
public class Patient {
    @Id
    private UUID id;

    private String name;
}
