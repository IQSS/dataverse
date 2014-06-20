
class SingleAPISpec:
    """
    Convenience class used to help DataverseAPILink when making API functions
    """
    
    ATTR_NAMES = ['new_function_name', 'name', 'url_path', 'use_api_key', 'num_id_vals', 'use_param_dict']
    URL_PLACEHOLDER = '{{ID_VAL}}'
    
    def __init__(self, spec_list):
        if not type(spec_list) in (list,tuple):
            raise Exception('Bad spec.  Expected list or tuple.\nReceived: %s' % type(spec_list))
            
        num_params = len(spec_list)    
        if not num_params in (5,6):
            raise Exception('Expected 5 or 6 values.\nReceived: %s' % spec_list)

        # Lazy way to add attributes
        for idx, attr in enumerate(self.ATTR_NAMES):
            if (idx) == num_params:
                self.__dict__[attr] = None          # only 5 params given, param_dict not needed
            else:
                self.__dict__[attr] = spec_list[idx]                
                # e.g., 1st iteration is equivalent of "self.new_function_name = spec_list[0]"
            
            
    def get_code_str(self, dv_link_function_to_call='make_api_get_call'):
        """
        Used to create functions within the DataverseAPILink class
        """
        if self.use_param_dict is True:
            # call_name, url_path, use_api_key=False, id_val=None, params_dict={}
            code_str = """
def %s(self, param_dict, *args):
    url_path = '%s'
    if args:
        for val in args:
            if not type(val) in (str, unicode):
                val = `val`
            url_path = url_path.replace('%s', val, 1)
        #url_path += '/' + str(id_val)
    print 'OK!'
    print 'param_dict', param_dict
    return self.%s('%s', url_path, %s, None, param_dict)""" \
                            % (self.new_function_name\
                                , self.url_path
                                , SingleAPISpec.URL_PLACEHOLDER
                                , dv_link_function_to_call
                                , self.name
                                , self.use_api_key)

        else:
            code_str = """
def %s(self, *args):
    url_path = '%s'
    if args:
        for val in args:
            if not type(val) in (str, unicode):
                val = `val`
            url_path = url_path.replace('%s', val, 1)
        #url_path += '/' + str(id_val)

    return self.%s('%s', url_path, %s)""" \
                            % (self.new_function_name\
                                , self.url_path
                                , SingleAPISpec.URL_PLACEHOLDER
                                , dv_link_function_to_call
                                , self.name
                                , self.use_api_key)
        print code_str
        return code_str

   