package com.lifeforce.payer.plan.domain.plan;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Entity(name = "plan")
public class Plan {
    @Id
    private UUID id;

    UUID payerId;

    String name;

    @OneToMany(mappedBy = "plan")
    List<PlanService> planServices;
}
