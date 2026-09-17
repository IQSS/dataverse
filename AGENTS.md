# Agent Instructions: CI Emulation & Development Workflow

You are operating inside a sandboxed Linux container simulating a local developer environment. Follow these operational constraints strictly.

## 1. Project Profile & Constraints
- **Stack**: Java (JDK 17), Maven/Gradle build system.
- **Environment Isolation**: This sandbox runs in unprivileged user space. Do NOT attempt to run `dockerd`, `docker compose`, or launch external service containers.
- **External Dependencies**: External systems (PostgreSQL, Solr, Payara/App Server) are NOT running. Integration tests (`*IT.java` or API-level end-to-end tests) must NOT be executed here. They will be triggered automatically by remote GitHub Actions upon PR creation.

## 2. CI Emulation & Local Verification Gate
Before proposing any code change, committing, or opening a Pull Request, you MUST execute the following verification steps:
1. **Compilation Check**: Verify strict build integrity.
   ```bash
   ./mvnw clean test-compile -DskipTests=false
   ```

2. **Lint & Code Style**: Ensure no checkstyle or static analysis regressions.
   ```bash
   ./mvnw checkstyle:check spotbugs:check
   ```

3. **Targeted Unit Testing**: Run ONLY unit tests and mock-based tests.
   ```bash
   ./mvnw test -Dtest="*Test" -DfailIfNoTests=false
   ```

## 3. Self-Healing Protocol
- If any compilation, lint, or test failure occurs during the checks above, inspect the stack trace and fix the issue iteratively.
- Do not stop until all above checks return exit code 0.
- If you write new logic or modify existing methods, ensure you include or update corresponding unit tests using Mockito / In-Memory mocks.

## 4. PR Deliverables
When generating the Pull Request:
- Provide a clear, bulleted summary of changes.
- Reference the tracking issue explicitly: Fixes #<ISSUE_NUMBER>.
- Include the list of unit test suites that passed locally in the sandbox.
