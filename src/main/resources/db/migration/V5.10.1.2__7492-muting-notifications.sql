ALTER TABLE authenticateduser
ADD COLUMN IF NOT EXISTS mutedemails TEXT;

ALTER TABLE authenticateduser
ADD COLUMN IF NOT EXISTS mutednotifications TEXT;