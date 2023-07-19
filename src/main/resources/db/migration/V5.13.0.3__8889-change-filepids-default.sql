INSERT INTO setting(content, lang, name) VALUES
              ('true', '', ':FilePIDsEnabled')
       ON CONFLICT DO NOTHING;
DELETE FROM setting where name=':FilePIDsEnabled' AND content='false';
