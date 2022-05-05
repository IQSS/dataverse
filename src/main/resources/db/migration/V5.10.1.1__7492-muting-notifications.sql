ALTER TABLE authenticateduser
ADD COLUMN IF NOT EXISTS mutedemails VARCHAR(1000);

ALTER TABLE authenticateduser
ADD COLUMN IF NOT EXISTS mutednotifications VARCHAR(1000);