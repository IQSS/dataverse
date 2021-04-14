-- Users can be deactivated.
ALTER TABLE authenticateduser ADD COLUMN IF NOT EXISTS deactivated BOOLEAN;
-- Prevent old users from having null for deactivated.
UPDATE authenticateduser SET deactivated = false WHERE deactivated IS NULL;
-- A timestamp of when the user was deactivated.
ALTER TABLE authenticateduser ADD COLUMN IF NOT EXISTS deactivatedtime timestamp without time zone;
