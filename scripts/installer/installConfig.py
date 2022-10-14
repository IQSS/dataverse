from installUtils import is_python_3
if is_python_3():
    from configparser import ConfigParser
else:
    from ConfigParser import SafeConfigParser

def read_config_file(configFile):
    if is_python_3():
        config = ConfigParser()
    else:
        config = SafeConfigParser()
    config.read(configFile)
    return config


