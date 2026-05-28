import requests
from requests_toolbelt.multipart.encoder import MultipartEncoder
from datetime import datetime
import json

########################## configuration for a draft dataset without files

dataverse_server = 'https://dev.archaeology.datastations.nl'
api_key = 'change-me'
persistentId = 'doi:10.5072/DAR/HBGPN5'

####################
print (' preparation: add file foo/bar  ' + ('-' * 40))

url = '%s/api/datasets/:persistentId/add?persistentId=%s' % (dataverse_server, persistentId)
files = {'file': ('bar', ('content2: %s' % datetime.now()))}
jason_data = {"jsonData": json.dumps({"directoryLabel": "foo"})}# conflicting dir
response = requests.post(url, headers={'X-Dataverse-key': api_key}, data=jason_data, files=files, verify=False)
print (response.status_code)
print (response.json())

####################
print (' preparation: add file foo.tab/bar  ' + ('-' * 40))

url = '%s/api/datasets/:persistentId/add?persistentId=%s' % (dataverse_server, persistentId)
files = {'file': ('bar', ('content2: %s' % datetime.now()))}
jason_data = {"jsonData": json.dumps({"directoryLabel": "foo.tab"})}# conflicting dir
response = requests.post(url, headers={'X-Dataverse-key': api_key}, data=jason_data, files=files, verify=False)
print (response.status_code)
print (response.json())

####################
print (' preparation: add file x to have a file to change  ' + ('-' * 40))

###
url = '%s/api/datasets/:persistentId/add?&persistentId=%s' % (dataverse_server, persistentId)
unique_content = 'content2: %s' % datetime.now()
files = {'file': ('x', unique_content)}
jason_data = {"jsonData": json.dumps({"label": "x"})}
response = requests.post(url, headers={'X-Dataverse-key': api_key}, data=jason_data, files=files, verify=False)
print (response.status_code)
print (response.json())

file_id = response.json()['data']['files'][0]['dataFile']['id']

####################
print (' file conflicting with existing dir gets sequence number  ' + ('-' * 40))

###
url = '%s/api/datasets/:persistentId/add?persistentId=%s' % (dataverse_server, persistentId)
files = {'file': ('foo', ('content2: %s' % datetime.now()))}
jason_data = {"jsonData": json.dumps({"label": "foo"})}
response = requests.post(url, headers={'X-Dataverse-key': api_key}, data=jason_data, files=files, verify=False)

print (response.json())
print (response.status_code)

####################
print (' tabular file conflicting with existing dir gets seq nr once converted to .tab ' + ('-' * 40))

url = '%s/api/datasets/:persistentId/add?persistentId=%s' % (dataverse_server, persistentId)
files = {'file': ('foo.csv', ('header1,header2\nvalue1,%s' % datetime.now()))}
jason_data = {"jsonData": json.dumps({"label": "foo.csv"})}
response = requests.post(url, headers={'X-Dataverse-key': api_key}, data=jason_data, files=files, verify=False)
print (response.status_code)
print (response.json())

####################
print (' files API metadata:  dir foo/bar conflicts with previously created file foo/bar: returns bad-request  ' + ('-' * 40))

### files API https://guides.dataverse.org/en/latest/api/native-api.html#updating-file-metadata
url = f'{dataverse_server}/api/files/{file_id}/metadata'
files = {'jsonData': (None, '{"directoryLabel": "foo/bar", "label": "files-api.txt"}  ' + ('-' * 40))}
response = requests.post(url, headers={'X-Dataverse-key': api_key}, files=files, verify=False)

print(response.status_code)
print(response.text)

####################
print ('datasets API update existing file into name conflicting with existing dir: returns bad-request  ' + ('-' * 40))

### datasets API https://guides.dataverse.org/en/latest/api/native-api.html#update-file-metadata
url = f'{dataverse_server}/api/datasets/:persistentId/files/metadata?key={api_key}&persistentId={persistentId}'
json_content = [{"dataFileId": file_id, "directoryLabel": "foo/bar", "label": "datasets-api.txt"}]
headers = {'X-Dataverse-key': api_key, 'Content-Type': 'application/json'}
response = requests.post(url, headers=headers, json=json_content, verify=False)

print(response.status_code)
print(response.text)

####################
print ('datasets API add file conflicting with existing file: gets seq nr  ' + ('-' * 40))

url = '%s/api/datasets/:persistentId/add?persistentId=%s' % (dataverse_server, persistentId)
files = {'file': ('fox', ('content2: %s' % datetime.now()))}
jason_data = {"jsonData": json.dumps({"label": "x"})}
response = requests.post(url, headers={'X-Dataverse-key': api_key}, data=jason_data, files=files, verify=False)

print (response.json())
print (response.status_code)

####################
print ('dataset API add dir conflicting with existing file: returns bad-request  ' + ('-' * 40))

url = '%s/api/datasets/:persistentId/add?persistentId=%s' % (dataverse_server, persistentId)
files = {'file': ('foo', ('content2: %s' % datetime.now()))}
jason_data = {"jsonData": json.dumps({"label": "dir-conflicts-with-file.txt", "directoryLabel": "foo/bar"})}
response = requests.post(url, headers={'X-Dataverse-key': api_key}, data=jason_data, files=files, verify=False)

print (response.json())
print (response.status_code)

####################
print (' datasets API: another file on existing dir is OK  ' + ('-' * 40))

url = '%s/api/datasets/:persistentId/add?persistentId=%s' % (dataverse_server, persistentId)
files = {'file': ('beer', ('content2: %s' % datetime.now()))}
jason_data = {"jsonData": json.dumps({"directoryLabel": "foo"})}# conflicting dir
response = requests.post(url, headers={'X-Dataverse-key': api_key}, data=jason_data, files=files, verify=False)
print (response.status_code)
print (response.json())

####################
print (' datasets API: a file with different capitalization is OK  ' + ('-' * 40))

url = '%s/api/datasets/:persistentId/add?persistentId=%s' % (dataverse_server, persistentId)
files = {'file': ('Beer', ('content2: %s' % datetime.now()))}
jason_data = {"jsonData": json.dumps({"directoryLabel": "foo"})}# conflicting dir
response = requests.post(url, headers={'X-Dataverse-key': api_key}, data=jason_data, files=files, verify=False)
print (response.status_code)
print (response.json())

####################
print (' files API replace: dir foo/bar conflicts with previously created file: returns bad-request  ' + ('-' * 40))

url = f'{dataverse_server}/api/files/{file_id}/replace'
files = {
    'jsonData': (None, '{"directoryLabel": "foo/bar", "label": "x", "forceReplace":true}  ' + ('-' * 40)),
    'file': ('foo', ('content2: %s' % datetime.now()))
}
response = requests.post(url, headers={'X-Dataverse-key': api_key}, files=files, verify=False)

print(response.status_code)
print(response.text)

####################
# not configured on DANS VM? Might also have no added value over previous test.
#
# print (' datasets API remote file: file foo conflicts with previously created dir: returns bad-request ????  ' + ('-' * 40))
#
# url = '%s/api/datasets/:persistentId/add?persistentId=%s' % (dataverse_server, persistentId)
# files = {
#     'jsonData': (None, '{"directoryLabel": "foo/bar", "label": "x", "forceReplace":true, "description":"A remote image.","storageIdentifier":"file://themes/custom/qdr/images/01234567890-012345678901","checksumType":"MD5","md5Hash":"509ef88afa907eaf2c17c1c8d8fde77e","fileName":"testlogo.png","mimeType":"image/png"}  ' + ('-' * 40)),
# }
# response = requests.post(url, headers={'X-Dataverse-key': api_key}, files=files, verify=False)
#
# print(response.status_code)
# print(response.text)
