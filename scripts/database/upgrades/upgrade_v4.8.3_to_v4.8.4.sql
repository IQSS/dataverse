-- Google login has used 131 characters. 64 is not enough.
ALTER TABLE oauth2tokendata ALTER COLUMN accesstoken TYPE text;
