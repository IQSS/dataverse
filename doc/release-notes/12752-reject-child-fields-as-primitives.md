Metadata APIs now reject child fields that are sent as if they were top level fields.

Compound metadata fields such as `author` are made up of child fields such as `authorName`. Those
child fields could previously be submitted on their own, for example:

```
curl -H "X-Dataverse-key:$API_TOKEN" -X PUT \
  "$SERVER_URL/api/datasets/:persistentId/editMetadata?persistentId=$PID" \
  --data '{"fields":[{"typeName":"authorName","value":"Poe, Edgar Allen"}]}'
```

That created a dataset field with no parent, which cannot be displayed or edited in the UI and which
breaks metadata export. Such a request now fails with a 400 response. This affects the native JSON
APIs (dataset create and update, `editMetadata`, `deleteMetadata`, and the template metadata API) as
well as the JSON-LD/semantic metadata API.

The same check is now also applied to the children of compound fields that do not allow multiples,
which previously were not validated against the parent's list of child field types at all.

See also #12752.
