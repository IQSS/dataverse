-- Insert the CC-BY license and add references to it from the termsofuseandacccess table

INSERT INTO license (id, uri, name, shortdescription, active, isDefault, iconurl, sortorder)
VALUES (2, 'http://creativecommons.org/licenses/by/4.0', 'CC BY 4.0', 'Creative Commons Attribution 4.0 International License.', true, false, 'https://licensebuttons.net/l/by/4.0/88x31.png', 2)
ON CONFLICT DO NOTHING;

UPDATE termsofuseandaccess SET license_id = (SELECT license.id FROM license WHERE license.name = 'CC BY 4.0')
WHERE id IN (SELECT id FROM ccby);
