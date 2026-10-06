# Payer tests

Keep unit tests only in this repository for now. Tests run without PostgreSQL or a Spring application context.

Service tests instantiate the service directly and mock its dependencies. They test reference checks, coverage and network boundaries, status transitions, and the review and history records passed to repositories. Review service tests mock the policy evaluator; clinical criteria are tested separately in `PlanEvalServiceTests`.

Review query service tests mock repositories and check queue summaries and request/policy detail mapping.

Controller tests use standalone MockMvc with a mocked service. Scheduler tests mock both the repository and the service.

Run the tests from the repository root:

```powershell
.\payer\mvnw.cmd -B -f pom.xml -pl payer '-Dmaven.compiler.proc=full' test
```

Use the project's JDK 26. The compiler flag enables the existing Lombok annotation processor configuration.
