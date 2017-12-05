-- Hopefully, 255 characters is enough. Google login has used 131 characters.
ALTER TABLE oauth2tokendata ALTER COLUMN accesstoken TYPE character varying(255);
