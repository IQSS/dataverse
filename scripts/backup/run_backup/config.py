import ConfigParser
import sys
Config = ConfigParser.ConfigParser()
Config.read("config.ini")

def ConfigSectionMap(section):
    dict1 = {}
    options = Config.options(section)
    for option in options:
        try:
            dict1[option] = Config.get(section, option)
            if dict1[option] == -1:
                sys.stderr.write("skip: %s\n" % option)
        except:
            print("exception on %s!" % option)
            dict1[option] = None
    return dict1
