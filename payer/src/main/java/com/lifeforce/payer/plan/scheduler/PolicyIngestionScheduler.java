package com.lifeforce.payer.plan.scheduler;

import com.lifeforce.payer.plan.domain.policy.IngestionStatus;
import com.lifeforce.payer.plan.domain.policy.Policy;
import com.lifeforce.payer.plan.repository.PolicyRepository;
import com.lifeforce.payer.plan.service.PolicyIngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PolicyIngestionScheduler {
    private static final Logger logger = LoggerFactory.getLogger(PolicyIngestionScheduler.class);

    private final PolicyRepository policyRepository;
    private final PolicyIngestionService policyIngestionService;

    public PolicyIngestionScheduler(PolicyRepository policyRepository,
                                    PolicyIngestionService policyIngestionService) {
        this.policyRepository = policyRepository;
        this.policyIngestionService = policyIngestionService;
    }

    @Scheduled(fixedDelayString = "${payer.scheduler.policy-ingestion-delay}", initialDelayString = "${payer.scheduler.policy-ingestion-delay}")
    public void ingestPendingPolicies() {
        List<Policy> policies = policyRepository.findByIngestionStatusOrderByIdAsc(IngestionStatus.NOT_INGESTED);
        for (Policy policy : policies) {
            try {
                policyIngestionService.ingestPolicy(policy.getId());
            } catch (Exception exception) {
                logger.error("Failed to ingest policy {}", policy.getId(), exception);
            }
        }
    }
}
