ALTER TABLE dataset ADD COLUMN useGenericThumbnail boolean;
ALTER TABLE maplayermetadata ADD COLUMN lastverifiedtime timestamp without time zone;
ALTER TABLE maplayermetadata ADD COLUMN lastverifiedstatus bigint;
