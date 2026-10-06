# Payer tests

Keep unit tests only in this repository for now. Tests run without PostgreSQL or a Spring application context.

Service tests instantiate the service directly and mock its dependencies. They test reference checks, coverage and network boundaries, status transitions, and the request and review records passed to repositories. Review service tests mock the policy evaluator; clinical criteria are tested separately in `PlanEvalServiceTests`.

Manual decision service tests mock repositories and check approvals, rejections, quantity rules, state guards, response fields, repeat decisions, and persistence failures.

Evidence service tests mock repositories and check requested items, allowed states, repeated requests, and persistence failures. Evidence controller tests use standalone MockMvc with mocked services and validate request fields.

Evidence submission service tests mock repositories and check provider matching, evidence merging, repeated evidence rounds, manual versus automatic routing, workflow guards, and preservation of original request fields. Submission controller tests use standalone MockMvc with mocked services to check validation and response mapping. Review service tests verify that reevaluation uses the evidence update time while initial reviews retain the original submission cutoff.

Review query service tests mock repositories and check queue summaries and request/policy detail mapping.

Provider request query tests mock repositories and check provider/status filtering, requests without reviews, batch review association, ownership checks, current evidence, and decision details. Request tracking controller tests use standalone MockMvc with mocked services. Query tests verify that reads do not save requests or reviews.

DTO mapping tests check conversion into domain value objects and compatibility with existing requested-service and clinical-evidence JSON.

Controller tests use standalone MockMvc with a mocked service. Scheduler tests mock both the repository and the service and verify that processing continues after one service call fails. Policy evaluator tests verify that structured evidence requests include only missing codes. Detail tests cover the structured request and evidence update timestamp.

Run the tests from the repository root:

```powershell
.\payer\mvnw.cmd -B -f pom.xml -pl payer test
```

Use the project's JDK 26. The payer Maven configuration enables the existing Lombok annotation processor.
