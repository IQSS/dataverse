ALTER TABLE pendingworkflowinvocation
ADD COLUMN IF NOT EXISTS lock_id bigint,
      
DO $$
BEGIN
IF NOT EXISTS (SELECT 1 FROM information_schema.constraint_column_usage WHERE constraint_name='fk_pendingworkflowinvocation_lock_id')
THEN
ALTER TABLE pendingworkflowinvocation ADD CONSTRAINT fk_pendingworkflowinvocation_lock_id FOREIGN KEY (lock_id) REFERENCES datasetlock (id);
END IF;
END$$;