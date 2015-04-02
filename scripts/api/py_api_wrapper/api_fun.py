import os, sys
import time
import json
from dataverse_api_link import DataverseAPILink

def msg(s): print s
def dashes(char='-'): msg(40*char)
def msgt(s): dashes(); msg(s); dashes()
def msgx(s): dashes('\/'); msg(s); dashes('\/'); sys.exit(0)

def get_dataverse_link_object(apikey='pete'):
    server_with_api = 'http://localhost:8080/'
    #server_with_api = 'https://dvn-build.hmdc.harvard.edu'
    return DataverseAPILink(server_with_api, use_https=False, apikey=apikey)

def check_dv():    
    dat = get_dataverse_link_object()
    dat.save_current_metadata('local-data')

    #add_and_publish_dataverses('local-data/dataverses_2014-0612_11.json','pete')
    add_and_publish_dataverses('demo-data/dataverses_2014-0609_16.json','pete')

def load_users_from_api_file(fname):
    """
    Given the JSON results of the list users command (/api/builtin-users):
        (a) Iterate through the list
        (b) Check if a user exists (by id)
        (c) If the user is not found, create the user
        
    :param fname: full path to a file with user info in JSON format
    """
    if not os.path.isfile(fname):
        msgx('File not found: %s' % fname)
        
    # Load the JSON file
    user_dict = json.loads(open(fname,'r').read())

    # Get a the DataverseAPILink object
    dv_lnk_obj = get_dataverse_link_object('pete')
    dv_lnk_obj.set_return_mode_python()
    
    # Iterate through json
    for user_info in user_dict.get('data', []):
        # check if user exists via api
        current_user_info = dv_lnk_obj.get_user_data(user_info.get('id', None))
        if current_user_info and current_user_info.get('status') == 'OK': 
            continue    # The user exist, loop to the next user

        user_info.pop('id')     # Use the param, except for the 'id'
        
        # Create the user, passing user params and a password
        #
        
        new_password = user_info.get('userName')
        dv_lnk_obj.create_user(user_info, new_password)
        
def add_and_publish_dataverses(fname, apikey):
    if not os.path.isfile(fname):
        msgx('File not found: %s' % fname)

    # Load the JSON file
    dv_dict = json.loads(open(fname,'r').read())

    # Get a the DataverseAPILink object
    dv_lnk_obj = get_dataverse_link_object(apikey)
    dv_lnk_obj.set_return_mode_python()

    # Iterate through json
    previous_alias = "root"
    for dv_info in dv_dict.get('data', []):
        # check if user exists via api
        current_dv_info = dv_lnk_obj.get_dataverse_by_id_or_alias(dv_info.get('id', None))
        
        # DV exists, continue loop
        if current_dv_info and current_dv_info.get('status') == 'OK': 
            msg('>>> FOUND IT')
            previous_alias = current_dv_info['data']['alias']
            continue    # The user exist, loop to the next user
            
        # No DV, create it
        keys_not_needed = ['id', 'ownerID', 'creationDate', 'creator']
        for key in keys_not_needed:
            if dv_info.has_key(key):
                dv_info.pop(key)
        
        msg('params to send: %s' % dv_info)
        # If created,  publish it
        json_resp = dv_lnk_obj.create_dataverse(previous_alias, dv_info)
        if json_resp.get('status') == 'OK': 
            new_dv_data = json_resp.get('data', {})
            new_id = new_dv_data.get('id', None)
            if new_id is not None:
                dv_lnk_obj.publish_dataverse(new_id)
        previous_alias = current_dv_info.get("alias", "root")
        #break
            
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
        dat.create_dataverse(parent_dv_name_or_id, dv_params, )
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
    check_dv()
    #load_users_from_api_file('demo-data/users_2014-0609_14.json')
    #load_users_from_api_file('demo-data/rp_users.json')
    #add_and_publish_dataverses('demo-data/dataverses_2014-0609_14.json', 'gromit')
    #add_and_publish_dataverses('demo-data/rp_dataverses.json', 'gromit')
    
    #add_dataverses('Other DV #', 17, 23, 'snoopy')
    #add_dataverses('Uma\'s Other Retricted DVs #', 7, 8, 'pete')
    #delete_dataverses_id_greather_than(177, 'pete')
