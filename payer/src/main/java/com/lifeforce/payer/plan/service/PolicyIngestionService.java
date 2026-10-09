package com.lifeforce.payer.plan.service;

import com.lifeforce.payer.plan.domain.policy.IngestionStatus;
import com.lifeforce.payer.plan.domain.policy.Policy;
import com.lifeforce.payer.plan.repository.PolicyRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PolicyIngestionService {
    private static final Pattern HEADING_PATTERN =
            Pattern.compile("^##[ \\t]+(\\d+)\\.[ \\t]+(.+)$");

    private final PolicyRepository policyRepository;
    private final Path basePath;
    private final VectorStore vectorStore;
    private final TransactionTemplate statusTransaction;

    public PolicyIngestionService(PolicyRepository policyRepository,
                                  @Value("${payer.policy-documents.base-directory}") String baseDirectory,
                                  VectorStore vectorStore,
                                  PlatformTransactionManager transactionManager) {
        this.policyRepository = policyRepository;
        this.basePath = Path.of(baseDirectory).toAbsolutePath().normalize();
        this.vectorStore = vectorStore;
        this.statusTransaction = new TransactionTemplate(transactionManager);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void ingestPolicy(UUID policyId) throws IOException {
        Policy currentPolicy = policyRepository.findById(policyId).orElse(null);
        if (currentPolicy == null
                || currentPolicy.getIngestionStatus() == IngestionStatus.INGESTED
                || currentPolicy.getSourceFileName() == null) {
            return;
        }

        Path sourceFilePath = basePath.resolve(currentPolicy.getSourceFileName());
        String markdown = Files.readString(sourceFilePath, StandardCharsets.UTF_8);
        List<Document> chunks = chunkPolicyString(markdown, policyId, currentPolicy.getSourceFileName());

        // PgVectorStore generates embeddings here; do not hold the status transaction open.
        vectorStore.add(chunks);
        statusTransaction.executeWithoutResult(status -> {
            currentPolicy.markAsIngested();
            policyRepository.save(currentPolicy);
        });
    }

    private List<Document> chunkPolicyString(String markdown, UUID policyId, String sourceFileName) {
        String[] sections = markdown.split("(?m)(?=^##[ \\t]+)");
        List<Document> chunks = new ArrayList<>();

        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            String section = sections[sectionIndex].trim();
            if (section.isEmpty()) {
                continue;
            }
            String heading = section.lines().findFirst().orElseThrow();
            // The first split element can contain YAML, the title and document control.
            if (sectionIndex == 0 && !heading.startsWith("##")) {
                continue;
            }

            Matcher headingMatcher = HEADING_PATTERN.matcher(heading);
            if (!headingMatcher.matches()) {
                throw new IllegalArgumentException("Invalid section heading: " + heading);
            }

            String sectionId = headingMatcher.group(1);
            String sectionName = headingMatcher.group(2);
            // PgVectorStore upserts by ID, so a retry of this policy keeps the same rows.
            UUID chunkId = UUID.nameUUIDFromBytes(
                    (policyId + ":" + sectionId).getBytes(StandardCharsets.UTF_8));

            chunks.add(Document.builder()
                    .id(chunkId.toString())
                    .text("search_document: " + section)
                    .metadata(Map.of(
                            "policy_id", policyId.toString(),
                            "source_file", sourceFileName,
                            "section_name", sectionName,
                            "section_id", sectionId,
                            "chunk_index", chunks.size()
                    ))
                    .build());
        }

        if (chunks.isEmpty()) {
            throw new IllegalArgumentException("Policy document has no numbered sections: " + sourceFileName);
        }
        return chunks;
    }
}
