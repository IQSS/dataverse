ALTER TABLE ONLY setting DROP CONSTRAINT setting_pkey ;

ALTER TABLE setting ADD COLUMN IF NOT EXISTS ID SERIAL PRIMARY KEY;

ALTER TABLE setting ADD COLUMN IF NOT EXISTS lang text;

ALTER TABLE setting
   ADD CONSTRAINT non_empty_lang
            CHECK (lang <> '');

CREATE UNIQUE INDEX unique_settings
    ON setting
       (name, coalesce(lang, ''));
