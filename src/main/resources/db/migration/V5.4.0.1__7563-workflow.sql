ALTER TABLE pendingworkflowinvocation
ADD COLUMN IF NOT EXISTS lock_id bigint,
ADD CONSTRAINT fk_pendingworkflowinvocation_lock_id FOREIGN KEY (lock_id)
      REFERENCES datasetlock (id);