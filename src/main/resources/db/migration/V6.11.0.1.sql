-- This migration ensures that the function used for identifier generation
-- is marked as VOLATILE to prevent caching issues that could lead to
-- infinite loops in the application code.

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 
        FROM pg_proc 
        WHERE proname = 'generateidentifierfromstoredprocedure'
    ) THEN
        ALTER FUNCTION generateIdentifierFromStoredProcedure() VOLATILE;
    END IF;
END $$;
