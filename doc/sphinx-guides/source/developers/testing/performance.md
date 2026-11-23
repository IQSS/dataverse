# Performance Testing

## Introduction
Performance tests measure how your application behaves under load, focusing on execution time, resource consumption, and database efficiency.
Unlike *unit tests*, which verify isolated logic, or *integration* or *API tests*, which validate component interactions and full request lifecycles, performance tests quantify *how fast* operations complete and *how many* database queries they trigger.

## Running Performance Tests
Performance tests are excluded from the default test run to save CI/CD time and local resources.
To execute them, use the Maven `verify` lifecycle phase and override the `it.groups` property:

```shell
mvn verify -Dit.groups=performance
```

```{note}
The `it.groups` property accepts a comma-separated list.
You can combine groups (e.g., `-Dit.groups=integration,performance`) as necessary.
However, it is highly recommended to run them in isolation due to their computational intensity and sensitivity to system load.
```

## Testing database-bound code 
Performance tests for code relying on retrieving entities from a database are essential for catching regressions in ORM efficiency.
They can identify N+1 query problems or ensure that heavy data processing pipelines (e.g., exporting large datasets) remain responsive as the codebase evolves.

### Prerequisites
Any tests around database-bound code rely on [Testcontainers](https://www.testcontainers.org/) to spin up ephemeral database instances.
Avoiding in-memory databases for such tests allow for more realistic testing as seen in actual deployments.
Consequently, you must have **Docker** installed and running, allowing Testcontainer to start a PostgreSQL server.

- If you use a local Docker daemon, ensure it has sufficient memory allocated (typically 1GB+ is recommended for running Postgres containers alongside your tests).
- If your Docker daemon runs remotely, ensure the `DOCKER_HOST` environment variable is correctly configured in your shell so Testcontainers can locate it.

The automated testing setup will look up a system property `postgresql.server.version` to determine which container image tag to use.
The property is injected from `pom.xml` by Maven Failsafe and use a reasonable fallback value if missing.
To test with a different version of PostgreSQL, you may set the Maven property `postgresql.server.version` for a run.

### Example
Performance test classes must follow specific conventions to be discovered and executed correctly:

1. **Package Location:**  
   Place your test class in `src/test/java`, mirroring the package structure of the code you want to test (e.g., `edu.harvard.iq.dataverse.export`). 
   This placement grants the test class access to package private members in `src/main/java`, which is often necessary when testing internal services directly without going through the full API layer.
2. **Naming Convention:**   
   Name the class `*IT.java` so that the Maven Failsafe plugin automatically picks it up during the `integration-test` phase.
3. **Setup Annotation:**  
   Annotate the class with `@JpaPerformanceTest` to have everything set up automatically for you.
   A `JpaEntityManagerService` will be injected into a static class field for you, allowing interaction with a JPA Entity Manager.

Below is a minimal, generic example [`SamplePerformanceIT`](/_static/developers/testing/SamplePerformanceIT.java) demonstrating the structure and how to run a transaction with or without a return value.

```{literalinclude} /_static/developers/testing/SamplePerformanceIT.java
:name: sample-performance-test
:language: java
:start-at: // 
```

### Understanding JpaEntityManagerService
The `JpaEntityManagerService` class abstracts away the boilerplate required to set up a JPA environment for testing.
Here is what it does under the hood:

1. **Automatic PostgreSQL Server Setup:**  
   The involved JUnit Test Extension makes sure to create a single server instance to speed up test setups.
   Nonetheless, any test class will run within its own database on the server, guaranteeing test database isolation.

2. **Automatic Schema Generation:**  
   When you call `.start()` on a `JpaEntityManagerService` instance, it initializes an EclipseLink `EntityManagerFactory` configured to automatically generate the database schema (`schema-generation.database.action=create`).
   This guarantees that every test run begins with a pristine database structure derived directly from your current JPA entity mappings.
   You do not need to run Flyway migrations or seed the database beforehand.

3. **Transaction Management:**    
   The service handles the lifecycle of JPA transactions automatically.
   You simply pass a lambda to `inTransaction()` or `inTransactionVoid()`.
   The service will:
   1. Create an `EntityManager` and begin a transaction.
   2. Execute your lambda.
   3. Commit the transaction on success, or roll it back if a `RuntimeException` is thrown.
   4. Close the `EntityManager` in a `finally` block to prevent resource leaks.

4. **Query Statistics via Wrapped DataSource:**  
   To make it easy to profile ORM behavior, `JpaEntityManagerService` wraps the underlying PostgreSQL `DataSource` using a proxy that intercepts all SQL statements.

   By default, the proxy tracks query counts, which you can retrieve via `QueryCountHolder.getGrandTotal()`.
   This provides immediate, programmatic insight into database efficiency without needing to parse verbose SQL logs.
   It is particularly useful for:
   - Verifying that a batch operation executes in a single query rather than a loop.
   - Catching N+1 query problems by asserting on the number of `SELECT` statements.
   
   *Advanced Usage:* The default service only tracks query counts.
   If you need detailed SQL logging (including bound parameters) or custom execution metrics, you can extend `JpaEntityManagerService` and register additional `StatementListener` implementations on the `ProxyDataSourceBuilder` during initialization.