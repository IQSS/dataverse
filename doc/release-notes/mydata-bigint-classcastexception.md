### My Data no longer fails when results include files

On installations whose database was created recently, `dvobject.id` is a `bigint` column, so the
JDBC driver returns a `Long` for it. My Data read that value as an `Integer`, which made
`/api/mydata/retrieve` (and the My Data page) fail with a `ClassCastException` and an HTTP 500
for any request whose results included a file. The value is now read as a `Number`, which is
correct whether the column is an `integer` (older databases) or a `bigint` (newer ones).
