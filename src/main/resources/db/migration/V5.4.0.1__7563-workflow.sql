ALTER TABLE pendingworkflowinvocation
ADD COLUMN IF NOT EXISTS lockid bigint;
