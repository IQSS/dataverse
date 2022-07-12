### HTTP Range Requests: New HTTP status codes and headers for Datafile Access API

The Basic File Access resource for datafiles (/api/access/datafile/$id) was slightly modified in order to comply better with the HTTP specification for range requests.

If the request contains a "Range" header:
* The returned HTTP status is now 206 (Partial Content) instead of 200
* A "Content-Range" header is returned containing information about the returned bytes
* An "Accept-Ranges" header with value "bytes" is returned

CORS rules/headers were modified accordingly:
* The "Range" header is added to "Access-Control-Allow-Headers"
* The "Content-Range" and "Accept-Ranges" header are added to "Access-Control-Expose-Headers"
