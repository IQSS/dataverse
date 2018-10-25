INSERT INTO setting(name, content) VALUES (':UploadMethods', 'native/http');
ALTER TABLE datavariable ADD COLUMN factor BOOLEAN;
ALTER TABLE ingestrequest ADD COLUMN forceTypeCheck BOOLEAN;
