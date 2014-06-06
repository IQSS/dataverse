"""
Use Dataverse native APIs described here: https://github.com/IQSS/dataverse/tree/master/scripts/api

5/8/2013 - scratch work, examining API
6/5/2013 - Back to implementing some API work
6/6/2013 - Move function parameters into API_SPECS, create functions on init

Requires the python requests library:  http://docs.python-requests.org
"""
import sys
import json
import requests
from msg_util import *
import types #import MethodType, FunctionType

class SingleAPISpec:
    """Convenience class when making API functions"""
    ATTR_NAMES = ['new_function_name', 'name', 'url_path', 'use_api_key', 'num_id_vals']
    ID_VAL_IN_URL = '{{ID_VAL}}'
    
    def __init__(self, spec_list):
        if not (type(spec_list) in (list,tuple) and len(spec_list) == 5):
            raise Exception('Bad spec.  Expected list or tuple of 5 values\n%s' % spec_list)

        for idx, attr in enumerate(self.ATTR_NAMES):
            self.__dict__[attr] = spec_list[idx]
        
    #def get_url_path(self, id_val):
    #    if self.use_id_val:
    #        return self.url_path.replace(self.ID_VAL_IN_URL, '%s' % id_val)
            
        
class DataverseAPILink:
    """Used to test the Dataverse API described in github:
        https://github.com/IQSS/dataverse/tree/master/scripts/api 
    
    Example:
    from dataverse_api import DataverseAPILink
    server_with_api = 'https://dvn-build.hmdc.harvard.edu'

    dat = DataverseAPILink(server_with_api, use_https=False, apikey='pete')
    dat.set_return_mode_python()
    print dat.list_users()
    print dat.list_roles()
    print dat.list_dataverses()
    print dat.list_datasets()
    print dat.view_dataverse_by_id_or_alias(5)
    print dat.view_dataset_metadata_by_id_version(123, 57)
    print dat.view_root_dataverse()
    print dat.get_user_data(1)
    """
    RETURN_MODE_STR = 'RETURN_MODE_STR'
    RETURN_MODE_PYTHON = 'RETURN_MODE_PYTHON'
    HTTP_GET = 'GET'
    HTTP_POST = 'POST'
    HTTP_DELETE = 'DELETE'
    HTTP_METHODS = [HTTP_GET, HTTP_POST, HTTP_DELETE]
    
    # Each List corresponds to 'new_function_name', 'name', 'url_path', 'use_api_key', 'num_id_vals'
    #
    API_SPECS = (    
    # USERS
       [ 'list_users', 'List Users', '/api/users', False, 0]\
    ,  ['get_user_data', 'Get metadata for a specific user', '/api/users/%s' % SingleAPISpec.ID_VAL_IN_URL, False, 1]\
    
    # ROLES
    ,  ['list_roles', 'List Roles', '/api/roles', False, 0]\

    # Datasets
    ,  ['list_datasets', 'List Datasets', '/api/datasets', True, 0]\
    ,  ['view_dataset_by_id', 'View Dataset By ID' \
        , '/api/datasets/%s' % (SingleAPISpec.ID_VAL_IN_URL,), True, 1]\
    #,  ['view_dataset_versions_by_id', 'View Dataset By ID', '/api/datasets/%s/versions' % SingleAPISpec.ID_VAL_IN_URL, True, True]\
    # Dataverses
    ,  ['list_dataverses', 'List Dataverses', '/api/dvs', False, 0]\
    ,  ['view_dataverse_by_id_or_alias', 'View Dataverse by ID or Alias', '/api/dvs/%s' % (SingleAPISpec.ID_VAL_IN_URL,), False, 1]\
    ,  ['view_root_dataverse', 'View Root Dataverse', '/api/dvs/:root', False, 0]\
    
    # Metadata
    ,  ['list_metadata_blocks', 'List metadata blocks', '/api/metadatablocks', False, 0]
    ,  ['view_dataset_metadata_by_id_version', 'View Dataset By ID'\
        , '/api/datasets/%s/versions/%s/metadata' % (SingleAPISpec.ID_VAL_IN_URL, SingleAPISpec.ID_VAL_IN_URL),  True, 2]\
    )
                
    
    def __init__(self, server_name, use_https, apikey=None):
        """
        :param  server_name: e.g. dataverse.org, dvn-build.hmdc.harvard.edu, etc.
        :type   server_name: str
        :param  use_https: Use https for api calls?
        :type   use_https: boolean
        """
        self.server_name = server_name
        if len(self.server_name.split('//')) > 1:       # remove accidental additional of http:// or https://
            self.server_name = self.server_name.split('//')[-1]
        self.use_https = use_https
        self.apikey = apikey
        self.update_server_name()
        self.return_mode = self.RETURN_MODE_STR
        self.make_basic_functions()
        
    def set_return_mode_python(self):
        """API calls return JSON text response as a Python object
        Uses json.loads(json_str)
        """
        self.return_mode = self.RETURN_MODE_PYTHON
    
    def set_return_mode_string(self):
        """API calls return JSON responses as a string"""
        self.return_mode = self.RETURN_MODE_STR
    
        
    def update_server_name(self):
        if self.server_name is None:
            raise Exception('Server name is None!')
            
        if self.server_name.endswith('/'):  # cut trailing slash
            self.server_name = self.server_name[-1]

        server_name_pieces = self.server_name.split('//')
        if len(server_name_pieces) > 1:
            self.server_name = server_name_pieces[1]
        
    def get_server_name(self):
        
        if self.use_https:
            return 'https://' + self.server_name
        return 'http://' + self.server_name
        
    def make_api_call(self, url_str, method, params={}, headers=None):
        """
        Use the requests library to make the actual API call
        
        :param url_str: str, url to call
        :param method: str indicating http method: GET, POST, DELETE, etc. Must be in self.HTTP_METHODS: GET, POST, DELETE, 
        :param params: dict containing python parameters
        :param headers: optional dict containing headers. e.g. {'content-type': 'application/json'}
        
        :returns: response from the request
        :rtype: depends on self.RETURN_MODE_PYTHON; either text or JSON converted to python dict
        """
        
        msg('url_str: [%s]\nmethod:[%s]\nparams:[%s]\nheaders:[%s]' % (url_str, method, params, headers))
        if url_str is None:
            return None
        if not method in self.HTTP_METHODS:
            msgt('Error: Method not found: %s' % method)
        if not type(params) == dict:
            msgt('Params must be a python dict, {}')
            
        params = json.dumps(params)

        if method == self.HTTP_GET:
            r = requests.get(url_str, data=params)
        elif method == self.HTTP_POST:
            if headers is not None:
                r = requests.post(url_str, data=params, headers=headers)
            else:
                r = requests.post(url_str, data=params)
        elif method == self.HTTP_DELETE:
            r = requests.delete(url_str, data=params)
            
        print r.status_code
        print r.encoding
        #print r.text
        
        if self.return_mode == self.RETURN_MODE_PYTHON:
            return r.json()
        return r.text
       
        
    def create_dataverse(self, parent_dv_alias_or_id, dv_params):
        """Create a dataverse
        POST http://{{SERVER}}/api/dvs/{{ parent_dv_name }}?key={{username}}

        :param parent_dv_alias_or_id: str or integer, the alias or id of an existing datavese 
        :param dv_params: dict containing the parameters for the new dataverse

        Sample: Create Dataverse

        from dataverse_api import DataverseAPILink
        server_with_api = 'dvn-build.hmdc.harvard.edu'
        dat = DataverseAPILink(server_with_api, use_https=False, apikey='pete')
        dv_params = {
                    "alias":"hm_dv",
                    "name":"Home, Home on the Dataverse",
                    "affiliation":"Affiliation value",
                    "contactEmail":"pete@mailinator.com",
                    "permissionRoot":False,
                    "description":"API testing"
                    }
        parent_dv_alias_or_id = 'root'
        print dat.create_dataverse(parent_dv_alias_or_id, dv_params)
        """
        msgt('create_dataverse')
        print 'dv_params', dv_params
        if not type(dv_params) is dict:
            msgx('dv_params is None')
            
        url_str = self.get_server_name() + '/api/dvs/%s?key=%s' % (parent_dv_alias_or_id, self.apikey)
        headers = {'content-type': 'application/json'}    
        return self.make_api_call(url_str, self.HTTP_POST, params=dv_params, headers=headers)
    
    

    def show_api_info(self):
        for spec in self.API_SPECS:
            print spec[0]
        
        

    def make_basic_functions(self):
        """
        Go through API_SPECS and add the functions to DataverseAPILink
        """
        for spec in self.API_SPECS:

            # Load the params
            spec_obj = SingleAPISpec(spec)

            # Create the function
            code_str = """
def %s(self, *args):
    url_path = '%s'
    if args:
        for val in args:
            url_path = url_path.replace('%s', `val`, 1)
        #url_path += '/' + str(id_val)

    return self.make_api_get_call('%s', url_path, %s)""" \
                            % (spec_obj.new_function_name\
                                , spec_obj.url_path
                                , SingleAPISpec.ID_VAL_IN_URL
                                , spec_obj.name
                                , spec_obj.use_api_key)
            print code_str
            exec(code_str)

            # Bind the function to this object
            self.__dict__[spec_obj.new_function_name] = types.MethodType(eval(spec_obj.new_function_name), self)
            
            
            
    def make_api_get_call(self, call_name, url_path, use_api_key=False, id_val=None):
        msgt(call_name)
        if use_api_key:
            url_str = '%s%s?key=%s' % (self.get_server_name(), url_path, self.apikey)
        else:
            url_str = '%s%s' % (self.get_server_name(), url_path)

        return self.make_api_call(url_str, self.HTTP_GET)
   
    def make_api_delete_call(self, call_name, url_path, use_api_key=False, id_val=None):
        msgt(call_name)
        if use_api_key:
            url_str = '%s%s?key=%s' % (self.get_server_name(), url_path, self.apikey)
        else:
            url_str = '%s%s' % (self.get_server_name(), url_path)

        return self.make_api_call(url_str, self.HTTP_DELETE)#, kwargs)
    
    #def delete_dataverse_by_id(self, id_val):        
    #    msgt('delete_dataverse_by_id: %s' % id_val)    
    #    url_str = self.get_server_name() + '/api/dvs/%s?key=%s' % (id_val, self.apikey) 
    #kwargs = { 'key': self.apikey }
    #    return self.make_api_call(url_str, self.HTTP_DELETE)#, kwargs)

    
    '''
    def make_dataverse(self, username, dvn_info_as_dict, dv_id=None):
        """Make a dataverse
        POST http://{{SERVER}}/api/dvs?key={{username}}
        """
        msgt('make_dataverse: [username:%s]  [dv_id:%s]' % (username, dv_id))
        url_str = self.get_server_name() + '/api/dvs?key=%s' % username 
        #kwargs = {'key': username}
        return self.make_api_call(url_str, self.HTTP_POST, dvn_info_as_dict)
    '''   
    
def write_to_file(content, fname):
    open(fname, 'w').write(content)
    print 'file written: %s' % fname
    
def save_current_metadata(server_name, apikey):
    dat = DataverseAPILink(server_name, use_https=False, apikey=apikey)
    #dat.set_return_mode_python()
    dat.set_return_mode_string()
   
    #---------------------------
    # Retrieve the users
    #---------------------------
    user_json = dat.list_roles()
    print user_json
    #---------------------------
    # Retrieve the dataverses
    #---------------------------
    dv_json = dat.list_dataverses()
    print dv_json

    #---------------------------
    # Retrieve the datasets
    #---------------------------
    dset_json = dat.list_datasets()
    print dset_json
    
    output_dir = 'demo-data'
    
    """                    
    geo_list = []
    for ds in dataset_ids:
        did, vid = ds.split(':')
        geo_meta = dat.get_dataset_info(did,':latest')
        geo_list.append(geo_meta.strip())
        write_to_file(geo_meta, '%s/2014_0513_dataset_id_%s.json' % (output_dir, did))
    #geo_content = '\n'.join(geo_list)
    """
    
if __name__=='__main__':
    
    
    server_with_api = 'https://dvn-build.hmdc.harvard.edu'
    
    dat = DataverseAPILink(server_with_api, use_https=False, apikey='pete')
    dat.set_return_mode_python()
    
    #dat.set_return_mode_string()
    """ """
    print dat.view_dataverse_by_id_or_alias(5)
    print dat.view_dataset_metadata_by_id_version(123, 57)
    print dat.list_users()
    print dat.list_roles()
    print dat.list_datasets()
    print dat.list_dataverses()
    print dat.view_root_dataverse()
    print dat.get_user_data(1)
    print dat.list_metadata_blocks()
    
    sys.exit(0)
  
    """
    Test delete some dataverses
    """
    # List the dataverses
    dv_json = dat.list_dataverses()
    print dv_json
    # Pull dataverse ids > 30
    dv_ids = [dv['id'] for dv in dv_json.get("data") if dv['id'] > 30]
    
    # reverse order ids
    dv_ids.sort()
    dv_ids.reverse()
    
    # delete them
    for dv_id in dv_ids:
        print dat.delete_dataverse_by_id(dv_id)
    #print dat.list_datasets()
    
    
  