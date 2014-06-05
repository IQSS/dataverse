"""
Use Dataverse native APIs described here: https://github.com/IQSS/dataverse/tree/master/scripts/api

5/8/2013 - scratch work, examining API
6/5/2013 - Back to implementing some API work

Requires the python requests library:  http://docs.python-requests.org
"""
import sys
#import urllib 
#import urllib2
import json
import requests
from msg_util import *

class DataverseAPILink:
    """Used to test the Dataverse API described in github:
        https://github.com/IQSS/dataverse/tree/master/scripts/api 
    """
    RETURN_MODE_STR = 'RETURN_MODE_STR'
    RETURN_MODE_PYTHON = 'RETURN_MODE_PYTHON'
    HTTP_GET = 'GET'
    HTTP_POST = 'POST'
    HTTP_DELETE = 'DELETE'
    HTTP_METHODS = [HTTP_GET, HTTP_POST, HTTP_DELETE]
    
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
        """
        request = urllib2.Request(url_str)
        request.get_method = lambda:method  # GET, POST, DELETE
        if kwargs:
            request.add_data(urllib.urlencode(kwargs))

        response = urllib2.urlopen(request)

        print response.info()

        if self.return_mode == self.RETURN_MODE_PYTHON:
            json_response = json.loads(response.read())
            response.close()  
            return json_response

        json_str = response.read()
        response.close()  
        return json_str
        """
       
    def get_dataverse_metadata(self, dv_id=None):
        """List all dataverses using GET http://{{SERVER}}/api/dvs
        :param dv_id: dataverse id or None.  None lists all dataverses
        :type dv_id: int, None, ':root' 
        :return: JSON, dataverses metadata
        """
        msgt('get_dataverse_metadata: [%s]' % dv_id)    
        url_str = self.get_server_name() + '/api/dvs'
        if dv_id is not None:
            url_str += '/%s' % dv_id
        return self.make_api_call(url_str, self.HTTP_GET)

    def list_dataverses(self):
        msgt('list_dataverses')    
        return self.get_dataverse_metadata()
        
    def get_root_dataverse_metadata(self):
        msgt('get_root_dataverse_metadata')    
        return self.get_dataverse_metadata(':root')
        
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
    
    
    def get_dataset_info(self, dataset_id, dataset_version=None):
        """List all dataverses using GET http://{{SERVER}}/api/datasets/?key={{apikey}}
        @return: JSON, list of dataverses
        """
        msgt('get_dataset_info')    
        if not self.apikey:
            msg('Sorry!  You need an api key!')
            return
        url_str = self.get_server_name() + '/api/datasets/%s' % dataset_id
        if dataset_version:
            url_str = self.get_server_name() + '/api/datasets/%s/versions/%s/metadata' % (dataset_id, dataset_version)
        else:
            url_str = self.get_server_name() + '/api/datasets/%s' % (dataset_id)    

        url_str = '%s?key=%s' % (url_str, self.apikey)
        
        return self.make_api_call(url_str, self.HTTP_GET)
        

    def list_datasets(self):
        """List all datadryd using GET http://{{SERVER}}/api/datasets?key={{apikey}}
        @return: JSON, list of dataverses
        """
        msgt('list_datasets')    
        if not self.apikey:
            msg('Sorry!  You need an api key!')
            return
        url_str = self.get_server_name() + '/api/datasets?key=%s' % self.apikey
        return self.make_api_call(url_str, self.HTTP_GET)
    
    

    def delete_dataverse_by_id(self, id_val):        
        msgt('delete_dataverse_by_id: %s' % id_val)    
        url_str = self.get_server_name() + '/api/dvs/%s?key=%s' % (id_val, self.apikey) 
        #kwargs = { 'key': self.apikey }
        return self.make_api_call(url_str, self.HTTP_DELETE)#, kwargs)


    def get_user_data(self, uid_or_username=None):
        """Get metadata for a specific user
        GET http://{{SERVER}}/api/users/{{uid}}
        """
        msgt('get_user_data: %s' % uid_or_username)    
        url_str = self.get_server_name() + '/api/users/%s' % uid_or_username 
        return self.make_api_call(url_str, self.HTTP_GET)

    # Users
    def list_users(self):
        """List users
        GET http://{{SERVER}}/api/users
        """
        msgt('list_users')    
        
        url_str = self.get_server_name() + '/api/users'
        return self.make_api_call(url_str, self.HTTP_GET)
    
    # Roles
    def list_roles(self):
        """List roles
        GET http://{{SERVER}}/api/roles
        """
        msgt('list_roles')    

        url_str = self.get_server_name() + '/api/roles'
        return self.make_api_call(url_str, self.HTTP_GET)
    
    # Roles
    def list_metadata_blocks(self):
        """List metadata blocks
        GET http://{{SERVER}}/api/metadatablocks
        """
        msgt('list_metadata_blocks')    

        url_str = self.get_server_name() + '/api/metadatablocks'
        return self.make_api_call(url_str, self.HTTP_GET)

    def get_metadata_for_dataset(self, dataset_id, version_id):
        """Lists all the metadata blocks and their content, for the given dataset and version.

        GET http://{{SERVER}}/api/datasets/{{id}}/versions/{{versionId}}/metadata?key={{apikey}}

        ex/ http://dvn-build.hmdc.harvard.edu/api/datasets/9/versions/1/metadata?key=pete
        """
        msgt('get_metadata_for_dataset')    

        url_str = self.get_server_name() + '/api/datasets/%s/versions/%s/metadata' % (dataset_id, version_id)
        params = { 'key': self.apikey }
        return self.make_api_call(url_str, self.HTTP_GET, params)
    
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

    #json_text = dat.list_dataverses()

    #print json_text
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
    
    
    """
    dat.set_return_mode_python()
    d = dat.list_dataverses()   # python dictionary {}
    print d.keys()
    dv_names = [dv_info.get('name', '?') for dv_info in d['data']]
    print dv_names
    """
    """
    
    dat.set_return_mode_python()
    user_info = dat.list_users()
    user_ids = [info['id'] for info in user_info['data'] if info['id'] is not None]
    for uid in user_ids:
        print dat.get_user_data(uid)
    
    print dat.list_datasets()
    """    
    #print dat.get_root_dataverse_metadata()
    #print dat.get_dataverse_metadata(':root')
    #print dat.list_datasets()
    #print dat.get_dataset_info(113,':latest')
    #print dat.make_dataverse('pete')
    #print dat.delete_dataverse_by_id()
    