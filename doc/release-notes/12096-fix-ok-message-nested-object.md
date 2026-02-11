### API Response Format Fix for `message` Field

The `message` field in API responses from certain endpoints was incorrectly returned as a nested object (`{"message": {"message": "..."}}`) instead of a plain string (`{"message": "..."}`).

This has been fixed. The following endpoints now return the `message` field as a string, consistent with all other API responses:

- `POST /api/datasets/{id}/add` (when uploading duplicate files)
- `PUT /api/admin/settings`
- `PUT /api/dataverses/{id}`
- `PUT /api/dataverses/{id}/inputLevels`
- `POST /api/admin/savedsearches`
- `PUT /api/harvest/clients/{nickName}`
- `PUT /api/harvest/server/oaisets/{specname}`

**Note:** If you have integrations that implemented workarounds for the nested `message` object, you may need to update your code to expect a plain string instead.
If you need time to update your integrations, you can temporarily revert to the legacy behavior by setting this JVM option:

```
dataverse.legacy.api-response-message-style=true
```

This flag will be removed in a future version.

**Note:** As of this version, there is also an experimental opt-in feature that will align API responses on about 230 more occassions.
In these responses, the message is embedded into the "data" field as a nested object.
If you want to test your integrations and clients, please enable the `dataverse.feature.unify-api-response-message-style` feature flag.
In a future version of Dataverse, this now experimental style is going to become the supported default.
