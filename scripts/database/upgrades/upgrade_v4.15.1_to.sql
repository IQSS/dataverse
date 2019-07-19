ALTER TABLE setting ADD COLUMN lang text;

UPDATE setting
SET lang = 'en';

ALTER TABLE ONLY setting
    DROP CONSTRAINT setting_pkey ;

ALTER TABLE ONLY setting
    ADD CONSTRAINT setting_pkey PRIMARY KEY (name,lang);
