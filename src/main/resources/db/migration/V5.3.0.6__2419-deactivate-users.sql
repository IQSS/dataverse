-- Users can be deactivated.
ALTER TABLE authenticateduser ADD COLUMN IF NOT EXISTS deactivated BOOLEAN;
-- A timestamp of when the user was deactivated.
ALTER TABLE authenticateduser ADD COLUMN IF NOT EXISTS deactivatedtime timestamp without time zone;
