"""
Use Dataverse native APIs described here: https://github.com/IQSS/dataverse/tree/master/scripts/api

5/8/2013 - scratch work, examining API
6/5/2013 - Back to implementing some API work
6/6/2013 - Move function parameters into API_SPECS, create functions on init

Requires the python requests library:  http://docs.python-requests.org

"""
import os
import sys
import json
import requests
from msg_util import *
import types # MethodType, FunctionType
from datetime import datetime
from single_api_spec import SingleAPISpec

def msg(s): print s
def dashes(char='-'): msg(40*char)
def msgt(s): dashes(); msg(s); dashes()
def msgx(s): dashes('\/'); msg(s); dashes('\/'); sys.exit(0)

        
class DataverseAPILink:
    """
    Convenience class to access the Dataverse API described in github:

        https://github.com/IQSS/dataverse/tree/master/scripts/api 
    
    Example:
    from dataverse_api_link import DataverseAPILink
    server_with_api = 'https://dvn-build.hmdc.harvard.edu'

    dat = DataverseAPILink(server_with_api, use_https=False, apikey='pete')
    dat.set_return_mode_python()
    print dat.list_users()
    print dat.list_roles()
    print dat.list_dataverses()
    print dat.list_datasets()
    print dat.get_dataverse_by_id_or_alias(5)
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
    
    # Each List corresponds to 'new_function_name', 'name', 'url_path', 'use_api_key', 'num_id_vals', 'use_params_dict'
    #
    API_READ_SPECS = (    
    # USERS
       [ 'list_users', 'List Users', '/api/builtin-users', False, 0]\
    ,  ['get_user_data', 'Get metadata for a specific user', '/api/builtin-users/%s' % SingleAPISpec.URL_PLACEHOLDER, False, 1]\
    
    # ROLES
    ,  ['list_roles', 'List Roles', '/api/roles', False, 0]\

    # Datasets
    ,  ['list_datasets', 'List Datasets', '/api/datasets', True, 0]\
    ,  ['view_dataset_by_id', 'View Dataset By ID' \
        , '/api/datasets/%s' % (SingleAPISpec.URL_PLACEHOLDER,), True, 1]\
    #,  ['view_dataset_versions_by_id', 'View Dataset By ID', '/api/datasets/%s/versions' % SingleAPISpec.URL_PLACEHOLDER, True, True]\
    # Dataverses
    ,  ['list_dataverses', 'List Dataverses', '/api/dataverses', False, 0]\
    ,  ['get_dataverse_by_id_or_alias', 'View Dataverse by ID or Alias', '/api/dataverses/%s' % (SingleAPISpec.URL_PLACEHOLDER,), False, 1]\
    ,  ['view_root_dataverse', 'View Root Dataverse', '/api/dataverses/:root', False, 0]\
    
    # Metadata
    ,  ['list_metadata_blocks', 'List metadata blocks', '/api/metadatablocks', False, 0]
    ,  ['view_dataset_metadata_by_id_version', 'View Dataset By ID'\
        , '/api/datasets/%s/versions/%s/metadata' % (SingleAPISpec.URL_PLACEHOLDER, SingleAPISpec.URL_PLACEHOLDER),  True, 2]\

    )


    API_WRITE_SPECS = (
        
        # Create a Dataverse
        #   curl -H "Content-type:application/json" -X POST -d @data/dv-pete-top.json "http://localhost:8080/api/dataverses/root?key=pete"
        #
        #[ 'create_dataverse', 'Create Dataverse', '/api/dataverses/%s' % SingleAPISpec.URL_PLACEHOLDER, True, 1, True]\
        
        # Create a User
        #   curl -H "Content-type:application/json" -X POST -d @data/userPete.json "http://localhost:8080/api/builtin-users?password=pete"
        #
        #[ 'create_user', 'Create User', '/api/builtin-users?password=%s' % SingleAPISpec.URL_PLACEHOLDER, False, 1, True]\
        #,
    )
                
    API_DELETE_SPECS = (
        # Dataset
        [ 'delete_dataset', 'Delete Dataset', '/api/builtin-users/%s' % SingleAPISpec.URL_PLACEHOLDER, True, True]\
        #DELETE http://{{SERVER}}/api/datasets/{{id}}?key={{apikey}}
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
            if self.server_name.endswith('/'):
                self.server_name = self.server_name[:-1]
        self.use_https = use_https
        self.apikey = apikey
        self.update_server_name()
        self.return_mode = self.RETURN_MODE_STR
        self.bind_basic_functions()
        
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
            
        msg('Status Code: %s' % r.status_code)
        msg('Encoding: %s' % r.encoding)
        msg('Text: %s' % r.text)
        
        if self.return_mode == self.RETURN_MODE_PYTHON:
            return r.json()
        
        #print json.dumps(json.loads(s), indent=4)
        try:
            return json.dumps(json.loads(r.text), indent=4)
        except:
            pass
        return r.text
    
    
    def create_user(self, dv_params, new_password):
        """
        Create a user

        :param dv_params: dict containing the parameters for the new user
        :param new_password: str for the user's password
        """
        msgt('create_user')
        if not type(dv_params) is dict:
            msgx('dv_params is None')

        #    [ 'create_user', 'Create User', '/api/builtin-users?password=%s' % SingleAPISpec.URL_PLACEHOLDER, False, 1, True]\
        
        url_str = self.get_server_name() + '/api/builtin-users?password=%s' % (new_password)
        headers = {'content-type': 'application/json'}    
        return self.make_api_call(url_str, self.HTTP_POST, params=dv_params, headers=headers)
    
    
    def create_dataverse(self, parent_dv_alias_or_id, dv_params):
        """Create a dataverse
        POST http://{{SERVER}}/api/dataverses/{{ parent_dv_name }}?key={{username}}

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
        if not type(dv_params) is dict:
            msgx('dv_params is None')
            
        url_str = self.get_server_name() + '/api/dataverses/%s?key=%s' % (parent_dv_alias_or_id, self.apikey)
        headers = {'content-type': 'application/json'}    
        return self.make_api_call(url_str, self.HTTP_POST, params=dv_params, headers=headers)
        
    def publish_dataverse(self, dv_id_or_name):
        """
        Publish a dataverse based on its id or alias
        #POST http://{{SERVER}}/api/dataverses/{{identifier}}/actions/:publish?key={{apikey}}
        
        :param dv_id_or_name: Dataverse id (str or int) or alias (str)
        """
        msgt('publish_dataverse')
        print 'dv_id_or_name', dv_id_or_name
        if dv_id_or_name is None:
            msgx('dv_id_or_name is None')
        
        url_str = self.get_server_name() + '/api/dataverses/%s/actions/:publish?key=%s' % (dv_id_or_name, self.apikey)
        headers = {'content-type': 'application/json'}    
        return self.make_api_call(url_str, self.HTTP_POST)
        
        
    def show_api_info(self):
        for spec in self.API_READ_SPECS:
            print spec[0]
        

    def bind_single_function(self, spec_list, function_name_for_api_call):
        """
        :param spec_list: list or tuple defining function sepcs
        :param function_name_for_api_call: str naming coded function in the DataverseAPILink
        """
         # Load the function specs
        single_api_spec = SingleAPISpec(spec_list)

        # Pull the code to generate the function.  e.g. def function_name(params): etc, etc
        code_str = single_api_spec.get_code_str(function_name_for_api_call)    # ---- GET ----
        
        # Create the function 
        exec(code_str)

        # Bind the function to this instance of DataverseAPILink
        self.__dict__[single_api_spec.new_function_name] = types.MethodType(eval(single_api_spec.new_function_name), self)
        

    def bind_basic_functions(self):
        """
        Go through API specs and add the functions to DataverseAPILink
        """
        
        # Add read functions
        for spec in self.API_READ_SPECS:
            self.bind_single_function(spec, 'make_api_get_call')
        
        # Decided to explicitly write add functions for clarity
        # Add write functions
        #for spec in self.API_WRITE_SPECS:
        #    self.bind_single_function(spec, 'make_api_write_call')

         
    

    def make_api_write_call(self, call_name, url_path, use_api_key=False, id_val=None, params_dict={}):
        msgt(call_name)
        print 'params_dict', params_dict
        if not type(params_dict) is dict:
            msgx('params_dict is not a dict.  Found: %s' % type(params_dict))

        if use_api_key:
            url_str = '%s%s?key=%s' % (self.get_server_name(), url_path, self.apikey)
        else:
            url_str = '%s%s' % (self.get_server_name(), url_path)

        headers = {'content-type': 'application/json'}    
        return self.make_api_call(url_str, self.HTTP_POST, params=params_dict, headers=headers)

    
            
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


    def save_to_file(self, fname, content):
        dirname = os.path.dirname(fname)
        if not os.path.isdir(dirname):
            msgx('This directory does not exist: %s' % dirname)
        fh = open(fname, 'w')
        fh.write(content)
        fh.close()
        msg('File written: %s' % fname)
        
        
    def save_current_metadata(self, output_dir):
        """
        For the current server, save JSON with information on:
            - Users
            - Dataverses
            - Datasets
        """
        msgt('run_dataverse_backup')
        if not os.path.isdir(output_dir):
            msgx('This directory does not exist: %s' % output_dir)
    
        #date_str = datetime.now().strftime('%Y-%m%d_%H%M')
        date_str = datetime.now().strftime('%Y-%m%d_%H')
        
        self.set_return_mode_string()

        #---------------------------
        # Retrieve the users
        #---------------------------
        user_json = self.list_users()
        self.save_to_file(os.path.join(output_dir, 'users_%s.json' % date_str), user_json)

        #---------------------------
        # Retrieve the roles
        #---------------------------
        #roles_json = self.list_roles()
        #self.save_to_file(os.path.join(output_dir, 'roles_%s.json' % date_str), roles_json)
    
        #---------------------------
        # Retrieve the dataverses
        #---------------------------
        dv_json = self.list_dataverses()
        self.save_to_file(os.path.join(output_dir, 'dataverses_%s.json' % date_str), dv_json)

        #---------------------------
        # Retrieve the datasets
        #---------------------------
        dset_json = self.list_datasets()
        self.save_to_file(os.path.join(output_dir, 'datasets_%s.json' % date_str), dset_json)        

    
    def delete_dataverse_by_id(self, id_val):        
        msgt('delete_dataverse_by_id: %s' % id_val)    
        url_str = self.get_server_name() + '/api/dataverses/%s?key=%s' % (id_val, self.apikey) 
        return self.make_api_call(url_str, self.HTTP_DELETE)

    

    
if __name__=='__main__':
    import time
    
    #POST http://{{SERVER}}/api/dataverses/{{identifier}}/actions/:publish?key={{apikey}}
    
    server_with_api = 'https://dvn-build.hmdc.harvard.edu'
    dat = DataverseAPILink(server_with_api, use_https=False, apikey='pete')
    #dat.save_current_metadata('demo-data')
    #sys.exit(0)
    #dat.set_return_mode_string()
    
    """ """
    dv_params = {
                    "alias":"hm_dv",
                    "name":"Home, Home on the Dataverse",
                    "affiliation":"Affiliation value",
                    "contactEmail":"pete@mailinator.com",
                    "permissionRoot":False,
                    "description":"API testing"
                    }
    print dat.create_dataverse('root', dv_params)
    #print dat.create_user('some_pw', dv_params)
    """
    print dat.get_dataverse_by_id_or_alias(5)
    print dat.view_dataset_metadata_by_id_version(123, 57)
    print dat.list_users()
    print dat.list_roles()
    print dat.list_datasets()
    print dat.list_dataverses()
    print dat.view_root_dataverse()
    print dat.get_user_data(1)
    print dat.list_metadata_blocks()
    """
    