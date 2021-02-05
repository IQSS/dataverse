ALTER TABLE pendingworkflowinvocation
ADD COLUMN IFNOTEXISTS lock_id bigint,
ADD CONSTRAINT fk_pendingworkflowinvocation_lock_id FOREIGN KEY (datasetlock_id)
      REFERENCES datasetlock (id);