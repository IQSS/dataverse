INSERT INTO setting(content, lang, name) VALUES
              ('true', '', ':FilePIDsEnabled')
       ON CONFLICT DO NOTHING;
