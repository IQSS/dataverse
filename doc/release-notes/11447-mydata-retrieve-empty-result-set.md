## Feature MyData API Endpoint - don't return error for empty result set

**GET /api/mydata/retrieve** will now return "data" block with 0 results if the result set is empty, Also, the "success" status will be returned as 'true' and the message giving context as to the 0 results will be returned in "message" instead of "error_message".  All true errors will still return "success":false and "error_message":"Some error" with no "data" block.
