# Payer tests

Service tests instantiate the service directly and mock its dependencies. They test reference checks, coverage and network boundaries, status transitions, and the review and history records passed to repositories. Review service tests mock the policy evaluator; clinical criteria are tested separately in `PlanEvalServiceTests`.

Controller tests use standalone MockMvc with a mocked service. Scheduler tests mock both the repository and the service.

Run these tests without PostgreSQL from the repository root:

```powershell
.\payer\mvnw.cmd -B -f pom.xml -pl payer '-Dmaven.compiler.proc=full' '-DexcludedGroups=integration' test
```

`ReviewWorkflowIntegrationTests` and `PayerApplicationTests` are tagged `integration` and require PostgreSQL. The workflow tests check persisted history, shared policy mappings, and transaction rollback when history saving fails. Each test removes its own fixture rows.

Run only the integration tests against your test database:

```powershell
.\payer\mvnw.cmd -B -f pom.xml -pl payer '-Dmaven.compiler.proc=full' '-Dgroups=integration' '-Dspring.datasource.url=jdbc:postgresql://localhost:5432/payer_test' '-Dspring.datasource.username=postgres' '-Dspring.datasource.password=postgres' test
```

Running `test` without a group filter includes both sets. Use the project's JDK 26. The compiler flag enables the existing Lombok annotation processor configuration.
