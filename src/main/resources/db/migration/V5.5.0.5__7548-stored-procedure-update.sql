-- If the installation is using a stored procedure for generating
-- sequential numeric identifiers, create a wrapper function that
-- works with the new framework (the stored procedure now needs to
-- return a string) and update the database setting
DO $BODY$
BEGIN
  UPDATE setting SET content='storedProcGenerated' 
    WHERE name=':IdentifierGenerationStyle'
    AND content='sequentialNumber';
  BEGIN
    PERFORM generateIdentifierAsSequentialNumber();
  EXCEPTION
    -- If the above function does not exist, we can stop executing this script
    WHEN undefined_function THEN
      RETURN;
  END;
  BEGIN
    PERFORM generateIdentifierFromStoredProcedure();
  EXCEPTION
    -- We only create this function if it doesn't already exist,
    -- to avoid overwriting user modifications 
    WHEN undefined_function THEN
      CREATE FUNCTION generateIdentifierFromStoredProcedure()
      RETURNS varchar AS $$
      DECLARE
          identifier varchar;
      BEGIN
          identifier := generateIdentifierAsSequentialNumber()::varchar;
          RETURN identifier;
      END;
      $$ LANGUAGE plpgsql IMMUTABLE;
  END;
END $BODY$;
