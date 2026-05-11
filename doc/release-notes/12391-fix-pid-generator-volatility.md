Fixed an issue where dataset creation could become stuck in an infinite loop when using a custom stored procedure for PID generation.

The root cause was the stored procedure being marked as `IMMUTABLE` in PostgreSQL, which allowed the database or persistence layer to cache its result.

A Flyway migration has been added to automatically set the volatility of `generateIdentifierFromStoredProcedure()` to `VOLATILE` instead.