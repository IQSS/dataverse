Fixed an issue where dataset creation could become stuck in an infinite loop when using a custom stored procedure for PID generation.

The issue occurred when the stored procedure was marked as `IMMUTABLE` in PostgreSQL, allowing the database or persistence layer to cache its result.

To prevent this, a Flyway migration now automatically updates the volatility of `generateIdentifierFromStoredProcedure()` to `VOLATILE`.

The `identifier_from_timestamp.sql` function provided in the docs has also been updated to use `clock_timestamp()` instead of `now()`, avoiding the same issue. If you use `identifier_from_timestamp.sql` as your custom stored procedure, we recommend updating to the latest version.
