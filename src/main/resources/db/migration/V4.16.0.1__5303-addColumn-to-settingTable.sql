ALTER TABLE ONLY setting DROP CONSTRAINT setting_pkey ;

ALTER TABLE setting ADD COLUMN IF NOT EXISTS ID SERIAL PRIMARY KEY;

ALTER TABLE setting ADD COLUMN IF NOT EXISTS lang text;

CREATE UNIQUE INDEX IF NOT EXISTS unique_settings
    ON setting
       (name, coalesce(lang, ''));
