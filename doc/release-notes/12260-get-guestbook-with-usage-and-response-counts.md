## API Enhancement
API call to `/api/guestbooks/{dataverseAlias}/list` will now include `"usageCount":#` and `"responseCount":#` in the response.
By adding the query param "ignoreStats=true" these values can be excluded for faster response.
