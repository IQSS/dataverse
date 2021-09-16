UPDATE template SET termsofuseandaccess_id = null;
ALTER TABLE template DROP COLUMN IF EXISTS termsofuseandaccess_id;

UPDATE datasetversion SET termsofuseandaccess_id = null;
ALTER TABLE datasetversion DROP COLUMN IF EXISTS termsofuseandaccess_id;

DROP TABLE IF EXISTS termsofuseandaccess;