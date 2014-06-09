import time
import json
from dataverse_api_link import DataverseAPILink

def msg(s): print s
def dashes(char='-'): msg(40*char)
def msgt(s): dashes(); msg(s); dashes()
def msgx(s): dashes('\/'); msg(s); dashes('\/'); sys.exit(0)


def get_dataverse_link_object(apikey='pete'):
    server_with_api = 'https://dvn-build.hmdc.harvard.edu'
    return DataverseAPILink(server_with_api, use_https=False, apikey=apikey)

def add_dataverses(name, cnt=1, parent_dv_name_or_id=1, apikey='snoopy'):
    # get the DataverseAPILink
     dat = get_dataverse_link_object(apikey=apikey)
     dat.set_return_mode_python()
     
     for x in range(249, 260):      
         dat.publish_dataverse(x)
     return
     for x in range(0, cnt):        
        num  = x+1
        alias_str = "new_dv_%d" % num
        dv_params_str = """{ "alias":"%s",
                    "name":"%s %s",
                    "affiliation":"Affiliation value",
                    "contactEmail":"pete@malinator.com",
                    "permissionRoot":true,
                    "description":"More API testing"
                    }""" % (alias_str, name, num)
    
        dv_params = json.loads(dv_params_str)
        dat.create_dataverse2(parent_dv_name_or_id, dv_params, )
        if x % 20 == 0: time.sleep(1)


def delete_dataverses_id_greather_than(id_num, apikey):
     if not type(id_num) == int:
         raise('id_num needs be an int--not a %s' % type(id_num))

     # get the DataverseAPILink
     dat = get_dataverse_link_object(apikey=apikey)
     dat.set_return_mode_python()
     
     # List the dataverses
     dv_json = dat.list_dataverses()
     print dv_json
     # Pull dataverse ids > 30
     dv_ids = [dv['id'] for dv in dv_json.get("data") if dv['id'] > id_num]

     # reverse order ids
     dv_ids.sort()
     dv_ids.reverse()

     # delete them
     for dv_id in dv_ids:
         print dat.delete_dataverse_by_id(dv_id)
     #print dat.list_datasets()
     
if __name__ == '__main__':
    add_dataverses('Other DV #', 17, 23, 'snoopy')
    #add_dataverses('Uma\'s Other Retricted DVs #', 7, 8, 'pete')
    #delete_dataverses_id_greather_than(177, 'pete')
