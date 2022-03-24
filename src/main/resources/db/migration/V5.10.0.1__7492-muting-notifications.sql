ALTER TABLE authenticateduser
ADD COLUMN IF NOT EXISTS mutedemails BIGINT;

ALTER TABLE authenticateduser
ADD COLUMN IF NOT EXISTS mutednotifications BIGINT;
