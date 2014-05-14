# Python API Wrapper Guide

(in progress)

This a python class "DataverseAPILink" which may be used to make the API calls described in the dataverse/scripts/api/readme.md
Results of API calls may by returned as JSON (string format) or as python dictionaries.

Given a DVN server name and optional apikey, the class uses python's basic urllib2 to make the API calls.
Generate the infamous _Pete_,_Uma_ and _Gabbi_. 

	setup-dvs.sh

Generates root dataverse and some other dataverses for Pete and Uma.

## Quick example

List the dataverses
	
	server_with_api = 'dataverse-demo.iq.harvard.edu'
	dal = DataverseAPILink(server_with_api, use_https=False, apikey='admin')
    json_text = dal.list_dataverses()
    print json_text

Output: 

	{
	    "status":"OK",
	    "data":[
	        {
	            "id":93,
	            "alias":"b",
	            "name":"b",
	            "affiliation":"b",
	            "contactEmail":"b@b",
	            "permissionRoot":false,
	            "creator":{
	                "id":13,
	                "firstName":"b",
	                "lastName":"b",
	                "userName":"b",
	                "affiliation":"b",
	                "position":"b",
	                "email":"b@b"
	            },
	            "description":"b",
	            "ownerId":1,
	            "creationDate":"2014-05-12 02:38:36 -04"
	        },
	        {
	            "id":77,
	            "alias":"bc",
	            "name":"Beta Candidate",
	            "affiliation":"Top",
	            "contactEmail":"pete@malinator.com",
	            "permissionRoot":false,
	            "creator":{
	                "id":1,
	                "firstName":"Pete",
	                "lastName":"Privileged",
	                "userName":"pete",
	                "affiliation":"Top ",
	                "position":"The Boss",
	                "email":"pete@malinator.com"
	            },
	            "description":"This is a test",
	            "ownerId":1,
	            "creationDate":"2014-05-08 04:06:58 -04"
	        },
			(etc, etc)
	
Return the same list as a python object

	dat.set_return_mode_python()
	d = dat.list_dataverses()   # python dictionary {}
	print d.keys()
	dv_names = [dv_info.get('name', '?') for dv_info in d['data']]
	print dv_names

Output:

	[u'status', u'data']
	[u'b', u'Beta Candidate', u'kc58', u'Kevin Smoke Test 5/8', u'Penultimate Smoke Test', u"Pete's public place", u"Pete's restricted data", u"Pete's secrets", u'Root', u'smoke 5/7', u'testadd', u'testauthor', u'Test Cliosed', u'Test Open', u'testpete', u'Top dataverse of Pete', u'Top dataverse of Uma', u"Uma's first", u"Uma's restricted"]

