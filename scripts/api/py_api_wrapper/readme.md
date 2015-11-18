## note: not yet updated to work with new permissions
----

# Python API Wrapper Guide

(6/5/2014 - work in progress)

This a python class "DataverseAPILink" which may be used to make the API calls described in the Dataverse [API Guide](https://github.com/IQSS/dataverse/tree/master/scripts/api/readme.md)

Results of API calls may by returned as JSON (string format) or as python dictionaries.


## Dependency 

[python requests module](http://docs.python-requests.org/)

## Quick example

List the dataverses


```python
from dataverse_api_link import DataverseAPILink

server_with_api = 'demo.dataverse.org'
dal = DataverseAPILink(server_with_api, use_https=False, apikey='admin')
json_text = dal.list_dataverses()
print json_text
```

Output: 
```javascript
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
       
		(etc, etc)
```
	
Return the same list as a python object

```python

dat.set_return_mode_python()	# Return python dict instead of a string
d = dat.list_dataverses()   	# python dictionary {}
print d.keys()
dv_names = [dv_info.get('name', 'no name?') for dv_info in d['data']]
print dv_names
```

Output:
```python
[u'status', u'data']
[u'b', u'Beta Candidate', u'kc58', u'Kevin Smoke Test 5/8', u'Penultimate Smoke Test', u"Pete's public place", u"Pete's restricted data", u"Pete's secrets", u'Root', u'smoke 5/7', u'testadd', u'testauthor', u'Test Cliosed', u'Test Open', u'testpete', u'Top dataverse of Pete', u'Top dataverse of Uma', u"Uma's first", u"Uma's restricted"]
```
### Users

List Users:

```python
dat.set_return_mode_python()
user_info = dat.list_users()
print user_info
```
	
Iterate through each user and pull the same data by 'id'

```python
user_ids = [info['id'] for info in user_info['data'] if info['id'] is not None]
for uid in user_ids:
   	print dat.get_user_data(uid)
```
