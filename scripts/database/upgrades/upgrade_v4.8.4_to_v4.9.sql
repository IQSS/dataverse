ALTER TABLE externaltool ADD COLUMN type character varying(255);
UPDATE externaltool SET type = 'configure';
ALTER TABLE externaltool ALTER COLUMN type SET NOT NULL;
