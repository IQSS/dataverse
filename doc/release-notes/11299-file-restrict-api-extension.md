### Extend Restrict API to include new attributes

Original /restrict API only allowed for a boolean to update the restricted attribute of a file.
The extended API still allows for the single boolean for backward compatibility.
This change also allows for a JSON object to be passed which allows for the required `restrict` flag as well as optional attributes: `enableAccessRequest` and `termsOfAccess`.
If `enableAccessRequest` is false then the `termsOfAccess` text must also be included.

See [the guides](https://dataverse-guide--11349.org.readthedocs.build/en/11349/api/native-api.html#restrict-files), #11299, and #11349.
