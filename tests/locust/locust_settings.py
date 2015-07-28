from os.path import isfile, join, dirname, abspath
import json

KEY_PERSISTENT_IDS = 'PERSISTENT_IDS'
STRESS_TEST_DIR = dirname(abspath(__file__))
SETTINGS_FILE = join(STRESS_TEST_DIR, 'settings.json')


def get_settings_info(key_name):
    SETTINGS_LOOKUP = {}
    assert isfile(SETTINGS_FILE), 'File not found: %s' % SETTINGS_FILE

    if key_name in SETTINGS_LOOKUP:
        return SETTINGS_LOOKUP.get(key_name)

    json_settings = json.loads(open(SETTINGS_FILE, 'r').read())

    assert key_name in json_settings, 'Key *%s* not found in settings file' % key_name

    cred_value = json_settings.get(key_name)

    SETTINGS_LOOKUP[key_name] = cred_value

    return cred_value
