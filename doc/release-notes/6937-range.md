### Support for HTTP "Range" Header for Partial File Downloads

Dataverse now supports the HTTP "Range" header, which allows users to download parts of a file. Here are some examples:

- `bytes=0-9` gets the first 10 bytes.
- `bytes=10-19` gets 20 bytes from the middle.
- `bytes=-10` gets the last 10 bytes.
- `bytes=9-` gets all bytes except the first 10.

Only a single range is supported. For more information, see the [Data Access API](https://guides.dataverse.org/en/latest/api/dataaccess.html) section of the API Guide.
