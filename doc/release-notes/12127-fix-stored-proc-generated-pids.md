This release fixes a bug which prevents PIDs from being generated when the `identifier-generation-style` is set to `storedProcGenerated`.

Previously, this caused a database error ("ERROR: procedure generateidentifierfromstoredprocedure(unknown) does not exist").