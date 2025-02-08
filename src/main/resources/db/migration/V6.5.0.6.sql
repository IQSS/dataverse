ALTER TABLE license 
ADD COLUMN rights_identifier IF NOT EXISTS VARCHAR(255),
ADD COLUMN rights_identifier_scheme IF NOT EXISTS VARCHAR(255),
ADD COLUMN scheme_uri IF NOT EXISTS VARCHAR(255),
ADD COLUMN language_code IF NOT EXISTS VARCHAR(5);

-- Update existing entries

UPDATE license SET 
    rights_identifier = 'Apache-2.0',
    rights_identifier_scheme = 'SPDX',
    scheme_uri = 'https://spdx.org/licenses/',
    language_code = 'en'
WHERE uri = 'https://www.apache.org/licenses/LICENSE-2.0';

UPDATE license SET 
    rights_identifier = 'CC0-1.0',
    rights_identifier_scheme = 'SPDX',
    scheme_uri = 'https://spdx.org/licenses/',
    language_code = 'en'
WHERE uri = 'http://creativecommons.org/publicdomain/zero/1.0';

UPDATE license SET 
    rights_identifier = 'CC-BY-4.0',
    rights_identifier_scheme = 'SPDX',
    scheme_uri = 'https://spdx.org/licenses/',
    language_code = 'en'
WHERE uri = 'https://creativecommons.org/licenses/by/4.0/';

UPDATE license SET 
    rights_identifier = 'CC-BY-NC-4.0',
    rights_identifier_scheme = 'SPDX',
    scheme_uri = 'https://spdx.org/licenses/',
    language_code = 'en'
WHERE uri = 'https://creativecommons.org/licenses/by-nc/4.0/';

UPDATE license SET 
    rights_identifier = 'CC-BY-NC-ND-4.0',
    rights_identifier_scheme = 'SPDX',
    scheme_uri = 'https://spdx.org/licenses/',
    language_code = 'en'
WHERE uri = 'https://creativecommons.org/licenses/by-nc-nd/4.0/';

UPDATE license SET 
    rights_identifier = 'CC-BY-NC-SA-4.0',
    rights_identifier_scheme = 'SPDX',
    scheme_uri = 'https://spdx.org/licenses/',
    language_code = 'en'
WHERE uri = 'https://creativecommons.org/licenses/by-nc-sa/4.0/';

UPDATE license SET 
    rights_identifier = 'CC-BY-ND-4.0',
    rights_identifier_scheme = 'SPDX',
    scheme_uri = 'https://spdx.org/licenses/',
    language_code = 'en'
WHERE uri = 'https://creativecommons.org/licenses/by-nd/4.0/';

UPDATE license SET 
    rights_identifier = 'CC-BY-SA-4.0',
    rights_identifier_scheme = 'SPDX',
    scheme_uri = 'https://spdx.org/licenses/',
    language_code = 'en'
WHERE uri = 'https://creativecommons.org/licenses/by-sa/4.0/';

UPDATE license SET 
    rights_identifier = 'MIT',
    rights_identifier_scheme = 'SPDX',
    scheme_uri = 'https://spdx.org/licenses/',
    language_code = 'en'
WHERE uri = 'https://opensource.org/licenses/MIT';
