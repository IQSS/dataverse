ALTER TABLE termsofuseandaccess ADD COLUMN IF NOT EXISTS license_id BIGINT;

DO $$
BEGIN

  BEGIN
    ALTER TABLE termsofuseandaccess ADD CONSTRAINT fk_termsofuseandcesss_license_id foreign key (license_id) REFERENCES license(id);
  EXCEPTION
    WHEN duplicate_object THEN RAISE NOTICE 'Table constraint fk_termsofuseandcesss_license_id already exists';
  END;

  BEGIN
      INSERT INTO license (uri, name, shortdescription, active, isDefault, iconurl) VALUES ('http://creativecommons.org/publicdomain/zero/1.0', 'CC0','You can copy, modify, distribute and perform the work, even for commercial purposes, all without asking permission.',true, true, '/resources/images/cc0.png');
  EXCEPTION
    WHEN unique_violation THEN RAISE NOTICE 'CC0 has already been added to the license table';
  END;

  BEGIN
      UPDATE termsofuseandaccess
        SET license_id = (SELECT license.id FROM license WHERE license.name = 'CC0')
        WHERE termsofuseandaccess.license = 'CC0' AND termsofuseandaccess.license_id IS NULL;
  EXCEPTION
    WHEN undefined_column THEN RAISE NOTICE 'license is not in table - new instance';
  END;

END $$;
ALTER TABLE termsofuseandaccess DROP COLUMN IF EXISTS license;
