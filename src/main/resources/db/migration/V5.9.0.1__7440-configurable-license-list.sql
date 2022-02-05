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
        WHERE license = 'CC0'
          AND license_id IS NULL
          AND termsofuse IS null
          AND confidentialitydeclaration IS null
          AND specialpermissions IS null
          AND restrictions IS null
          AND citationrequirements IS null
          AND depositorrequirements IS null
          AND conditions IS null
          AND disclaimer IS null;

      UPDATE termsofuseandaccess
        SET termsofuse=CONCAT('This dataset is made available under a Creative Commons CC0 license with the following additional/modified terms and conditions: ', termsofuse)
        WHERE license = 'CC0'
          AND license_id IS NULL
          AND NOT (termsofuse IS null
          AND confidentialitydeclaration IS null
          AND specialpermissions IS null
          AND restrictions IS null
          AND citationrequirements IS null
          AND depositorrequirements IS null
          AND conditions IS null
          AND disclaimer IS null);

      UPDATE termsofuseandaccess
        SET termsofuse='This dataset is made available with limited information on how it can be used. You may wish to communicate with the Contact(s) specified before use.'
        WHERE license = 'NONE'
          AND license_id IS NULL
          AND termsofuse IS null
          AND NOT (confidentialitydeclaration IS null
          AND specialpermissions IS null
          AND restrictions IS null
          AND citationrequirements IS null
          AND depositorrequirements IS null
          AND conditions IS null
          AND disclaimer IS null);

      UPDATE termsofuseandaccess
        SET termsofuse='This dataset is made available without information on how it can be used. You should communicate with the Contact(s) specified before use.'
        WHERE license = 'NONE'
          AND license_id IS NULL
          AND termsofuse IS null
          AND confidentialitydeclaration IS null
          AND specialpermissions IS null
          AND restrictions IS null
          AND citationrequirements IS null
          AND depositorrequirements IS null
          AND conditions IS null
          AND disclaimer IS null;

  EXCEPTION
    WHEN undefined_column THEN RAISE NOTICE 'license is not in table - new instance';
  END;

END $$;
ALTER TABLE termsofuseandaccess DROP COLUMN IF EXISTS license;
