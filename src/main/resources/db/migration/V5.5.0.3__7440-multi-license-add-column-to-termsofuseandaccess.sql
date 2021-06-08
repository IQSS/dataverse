
ALTER TABLE termsofuseandaccess ADD COLUMN IF NOT EXISTS license_id BIGINT;

ALTER TABLE termsofuseandaccess ADD CONSTRAINT fk_termsofuseandcesss_license_id foreign key (license_id) REFERENCES license(id);