-- Users can be disabled.
ALTER TABLE authenticateduser ADD COLUMN IF NOT EXISTS disabled BOOLEAN;
-- A timestamp of when the user was disabled.
ALTER TABLE authenticateduser ADD COLUMN IF NOT EXISTS disabledtime timestamp without time zone;
