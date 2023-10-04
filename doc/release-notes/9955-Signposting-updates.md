This release fixes several issues (#9952, #9953, #9957) where the Signposting output did not match the Signposting specification. These changes introduce backward-incompatibility, but since Signposting support was added recently (in Dataverse 5.14 in PR #8981), we feel it's best to do this clean up and not support the old implementation that was not fully compliant with the spec.

To fix #9952, we surround the license info with `<` and `>`.

To fix #9953, we no longer wrap the response in a `{"status":"OK","data":{` JSON object. This has also been noted in the guides at https://dataverse-guide--9955.org.readthedocs.build/en/9955/api/native-api.html#retrieve-signposting-information

To fix #9957, we corrected the mime/content type, changing it from `json+ld` to `ld+json`. For backward compatibility, we are still supporting the old one, for now.
