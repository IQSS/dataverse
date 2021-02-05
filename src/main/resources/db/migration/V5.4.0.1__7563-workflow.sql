ALTER TABLE pendingworkflowinvocation
ADD COLUMN IFNOTEXISTS datasetlock_id bigint,
ADD CONSTRAINT fk_pendingworkflowinvocation_datasetlock_id FOREIGN KEY (datasetlock_id)
      REFERENCES datasetlock (id);