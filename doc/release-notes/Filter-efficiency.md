### Improved efficiency for per-request Filters

This release improves the performance of Dataverse's per-request handling of CORS Headers and API calls

It adds new jvm-options/Microprofile settings replacing the now deprecated database settings.

Additional changes:

- CORS headers can now be configured with a list of desired origins, methods, and allowed and exposed headers.
- An 'X-Dataverse-unblock-key' header has been added that can be used instead of the less secure 'unblock-key' query parameter when the :BlockedApiPolicy is set to 'unblock-key'
- Warnings have been added to the log if the Blocked Api settings are misconfigured or if the key is weak (when the "unblock-key" policy is used)
- The new `dataverse.api.blocked.key` can be configured using Payara password aliases or other secure storage options. 

New JvmSettings:
- `dataverse.cors.origin`: Allowed origins for CORS requests
- `dataverse.cors.methods`: Allowed HTTP methods for CORS requests
- `dataverse.cors.headers.allow`: Allowed headers for CORS requests
- `dataverse.cors.headers.expose`: Headers to expose in CORS responses
- `dataverse.api.blocked.policy`: Policy for blocking API endpoints
- `dataverse.api.blocked.endpoints`: List of API endpoints to be blocked
- `dataverse.api.blocked.key`: Key for unblocking API endpoints

Deprecated database settings:
- `:AllowCors`
- `:BlockedApiPolicy`
- `:BlockedApiEndpoints`
- `:BlockedApiKey`


Upgrade instructions:

The deprecated database settings will continue to work in this version. To use the new settings (which are more efficient),

If :AllowCors is not set or is true:
bin/asadmin create-jvm-options -Ddataverse.cors.origin=*

Optionally set origin to a list of hosts and/or set other CORS JvmSettings

bin/asadmin create-jvm-options '-Ddataverse.api.blocked.endpoints=<current :BlockedApiEndpoints>'

If :BlockedApiPolicy is set and is not 'drop'
bin/asadmin create-jvm-options '-Ddataverse.api.blocked.policy=<current :BlockedApiPolicy>'

If :BlockedApiPolicy is 'unblock-key' and :BlockedApiKey is set

   `echo "API_BLOCKED_KEY_ALIAS=<value of :BlockedApiKey>" > /tmp/dataverse.api.blocked.key.txt`

   `sudo -u dataverse /usr/local/payara6/bin/asadmin create-password-alias --passwordfile /tmp/dataverse.api.blocked.key.txt`

   When you are prompted "Enter the value for the aliasname operand", enter `api_blocked_key_alias`

   You should see "Command create-password-alias executed successfully."

   bin/asadmin create-jvm-options '-Ddataverse.api.blocked.key=${ALIAS=api_blocked_key_alias}'
   
   Restart Payara:
   
service payara restart

Check server.log to verify that your new settings are in effect.
   
Cleanup: delete deprecated settings:
curl -X DELETE http://localhost:8080/api/admin/settings/:AllowCors
curl -X DELETE http://localhost:8080/api/admin/settings/:BlockedApiEndpoints
curl -X DELETE http://localhost:8080/api/admin/settings/:BlockedApiPolicy
curl -X DELETE http://localhost:8080/api/admin/settings/:BlockedApiKey

