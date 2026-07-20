### Feature: Extend List File Access Requests API ###

Added ability to get access request history via the `/datafile/{id}/listRequests` API. The API returns a list of users/groups where the request for access is waiting for an accept or reject. Already accepted or rejected requests are not returned.

By adding the flag 'includeHistory=true' all of the requests will be returned. Pagination is also implemented in this feature. Adding a start page parameter and max list size (`&start=0` and `&per_page=20`) can limit the amount of data being returned.

See https://guides.dataverse.org/en/latest/api/dataaccess.html#list-file-access-requests

