# Agent Instructions & CI Emulation Workflow (Dataverse)

This file defines the operational guidelines, verification gates, architectural guardrails, and build workflows for AI agents (including Google Jules and Antigravity) and contributors working on Dataverse.

---

## 1. Project Profile & Environment Constraints

- **Java Version**: OpenJDK / Amazon Corretto **21** (Dataverse 6.x strictly requires Java 21; do NOT use JDK 17).
  ```bash
  # macOS:
  export JAVA_HOME="$(/usr/libexec/java_home -v 21 2>/dev/null || echo '/Library/Java/JavaVirtualMachines/corretto-21.jdk/Contents/Home')"
  # Linux:
  # export JAVA_HOME="/usr/lib/jvm/java-21-amazon-corretto"
  export PATH="$JAVA_HOME/bin:$PATH"
  ```
- **Build Tools**: Apache Maven 3.9+ (`mvn` or `./mvnw`).
- **Remotes**:
  - `origin`: `https://github.com/tuannx/dataverse.git` (personal fork)
  - `upstream`: `https://github.com/IQSS/dataverse.git` (official Harvard IQSS repository)
- **Environment Modes**:
  - **Sandboxed Agent / Container (e.g. Jules)**: Runs in an unprivileged user space without Docker. Do NOT attempt to run `dockerd`, `docker compose`, or launch external service containers. External systems (PostgreSQL, Solr, Payara/App Server) are NOT running; integration tests (`*IT.java` or API-level end-to-end tests) must NOT be executed locally and will be triggered automatically by remote GitHub Actions upon PR creation.
  - **Full Local Dev Environment**: Can launch the complete runtime via `./scripts/dev/dev-start-frd.sh` (Payara + PostgreSQL 17 + Solr 9.8.0) and fast redeploy with `./scripts/dev/dev-frd.sh`.

---

## 2. CI Emulation & Local Verification Gate (Mandatory Before PR)

Before proposing any code change, committing, or opening a Pull Request, agents MUST execute the following verification steps:

1. **Compilation Check**: Verify strict build integrity.
   ```bash
   mvn clean test-compile -DskipTests=false
   # Or in sandboxed environments without global mvn:
   ./mvnw clean test-compile -DskipTests=false
   ```

2. **Lint & Code Style**: Ensure no checkstyle or static analysis regressions.
   ```bash
   mvn checkstyle:checkstyle
   # Or in sandboxed environments:
   ./mvnw checkstyle:check
   ```

3. **Targeted Unit Testing**: Run ONLY unit tests and mock-based tests (no database or server dependencies).
   ```bash
   # Run a single targeted test:
   mvn test -Dtest="<TargetedTestName>"
   # Or in sandboxed environments:
   ./mvnw test -Dtest="*Test" -DfailIfNoTests=false
   ```

4. **Self-Healing Protocol**:
   - If any compilation, lint, or test failure occurs during the checks above, inspect the stack trace and fix the issue iteratively.
   - Do NOT stop until all above checks return exit code 0.
   - When writing new logic or modifying existing methods, ensure corresponding unit tests are included using Mockito / in-memory mocks that respect domain invariants.

---

## 3. Critical Domain Guardrails for Dataverse

These architectural rules are strict and non-negotiable. Violating them causes critical runtime failures in production.

### A. JPA, Entities & Metamodel Navigation
- **NEVER query `@Transient` fields in JPA Criteria Queries or JPQL**:
  Fields annotated with `@Transient` (such as `DataFile.deleted` or `DataFile.markedAsDuplicate`) do NOT exist in the database schema or the JPA Metamodel.
  Using them in `criteriaBuilder`, JPQL, or NamedQueries (e.g. `fileMetadataRoot.get("dataFile").get("deleted")`) will compile successfully with `javac` but throws a fatal `java.lang.IllegalArgumentException: The attribute [deleted] is not present in the entity` at runtime in EclipseLink, crashing the application (HTTP 500).
  Always inspect entity class definitions to verify persistent vs transient attributes before writing queries.

### B. Dataset & File Lifecycle Rules
- **Draft Version File Deletion**:
  In Dataverse, a `DatasetVersion` links to files through `FileMetadata` (`datasetVersion.getFileMetadatas()`).
  When a file is deleted from a draft version (see `UpdateDatasetVersionCommand`):
  - If the file was added in this draft (unpublished), both the `DataFile` and its `FileMetadata` are physically purged from the database.
  - If the file was published in an earlier version, its `FileMetadata` for the draft version is removed from the database and from `datasetVersion.getFileMetadatas()`.
  - **Key takeaway**: Deleted draft files do NOT exist in `datasetVersion.getFileMetadatas()`. There is NO persistent `deleted=true` flag. Do NOT attempt to filter draft files by `DataFile.deleted`.
- **Purpose of `DataFile.deleted`**:
  `DataFile.deleted` is purely a transient in-memory UI flag used only for:
  1. `ManageFilePermissionsPage` to render permissions of files deleted in subsequent versions.
  2. `DatasetPage.isFileDeleted()` to warn users when viewing historical (already published) versions that a file was deleted in a later version.
  Filtering by `DataFile.deleted` on `DatasetVersion` will corrupt historical versions by hiding valid published files.

### C. Testing Integrity & Avoiding Fake Mocks
- **No Circular / Tautological Mocks**:
  Do NOT write unit tests that manually set fields to impossible states (e.g. manually calling `df.setDeleted(true)` on in-memory POJOs) to justify artificial filtering logic.
  Unit tests must validate real domain invariants.
  If mocking services or repositories, verify that the simulated behavior reflects actual JPA and command engine semantics.

### D. PR & Upstream Contribution Cleanliness
- **No Scratch Artifacts in PRs**:
  Never commit planning documents (`Plan.md`, notes, temporary scratch files, or test outputs) to git.
- **Explicit Issue Number Required**:
  Do NOT submit PRs with placeholders like `Fixes #<ISSUE_NUMBER>`.
  Every PR must either reference a verified issue on `IQSS/dataverse` (e.g. `Fixes #12695`) or explicitly state it is a refactor/chore without an upstream issue.
- **Code Style**:
  - 4 spaces indentation for Java (no tabs).
  - 2 spaces indentation for Shell scripts.
  - Keep PR diffs minimal (reformat only lines you touch).
- **PR Target**:
  Submit PRs targeting the `develop` branch of `IQSS/dataverse`.

---

## 4. Local Runtime & Fast Redeploy (FRD) (For Full Dev Environments)

The project includes an inner-loop development stack with Docker Compose and Payara Server:

1. **Initial Stack Launch**:
   ```bash
   ./scripts/dev/dev-start-frd.sh
   ```
   - Deploys exploded WAR to `./target/dataverse`.
   - Starts Payara, PostgreSQL 17, and Solr 9.8.0.
   - Access URL: `http://localhost:8080` (Default credentials: `dataverseAdmin` / `admin1`).
   - Payara Admin Console: `http://localhost:4949`.

2. **Fast Redeploy (~12s sync into running container)**:
   ```bash
   ./scripts/dev/dev-frd.sh
   ```
   - Compiles modified classes and syncs (`rsync`) them directly into the running container without restarting services.

3. **Teardown**:
   ```bash
   ./scripts/dev/dev-down-frd.sh
   ```

---

## 5. Known Gotchas & Troubleshooting

- **Unidata Repository SSL Error (`PKIX path building failed`)**:
  - `artifacts.unidata.ucar.edu` uses an InCommon / Sectigo certificate chain.
  - If downloading `edu.ucar:cdm-core` fails, ensure `sectigotlsrootr46` is imported into the Java 21 truststore (`cacerts`).
- **PrimeFaces Maven Repo 403 Forbidden**:
  - `repository.primefaces.org` blocks non-browser agents via Cloudflare challenge (returning 403).
  - All modern PrimeFaces artifacts (`primefaces:11.0.0:jakarta`, `all-themes:1.0.10`) exist on Maven Central. If offline or clean builds stall, avoid querying `prime-repo`.
