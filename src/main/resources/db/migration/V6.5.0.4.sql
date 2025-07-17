ALTER TABLE dvobject ADD COLUMN IF NOT EXISTS separator character varying(255) DEFAULT '';

UPDATE dvobject SET separator='/' WHERE protocol = 'doi' OR protocol = 'hdl';