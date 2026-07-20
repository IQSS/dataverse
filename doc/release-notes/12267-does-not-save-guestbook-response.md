## BUG
Fixes 2 bugs
1. missing "gbrids" in the signed url query parameter list will no longer include "&gbrids=" without a value
2. For SPA, when a user attempting to download files with a guestbook response has no api token but is authenticated by bearer token, a temporary api token will be generated with an expiration of 1 minute which is used for signing and decoding the signed url.
