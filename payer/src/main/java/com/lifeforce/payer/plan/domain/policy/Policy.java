package com.lifeforce.payer.plan.domain.policy;

import jakarta.persistence.*;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Entity
public class Policy {
    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    ReviewMode reviewMode;

    @Enumerated(EnumType.STRING)
    Match match;

    @OneToMany(mappedBy = "policy")
    private List<PolicyCriterion> policyCriteria;
}
