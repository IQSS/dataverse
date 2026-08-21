## Upgrade Instructions

1. Ensure API endpoints are blocked

It is very important for the "admin" API endpoint to be blocked. Additionally, we recommend blocking the "builtin-users" endpoint. See [the guides](https://guides.dataverse.org/en/6.12/installation/config.html#blocking-api-endpoints) for details.

In the command below, replace "demo.dataverse.org" with the name of your server. Do the check remotely, not from the server itself.

`curl https://demo.dataverse.org/api/admin/settings`

If you can see your settings, follow the instructions [the guides](https://guides.dataverse.org/en/6.12/installation/config.html#blocking-api-endpoints) to block API endpoints.

Please note that as reported in #12232, versions of the guides from 6.7 through 6.11 incorrectly described how to configure the setting `dataverse.api.blocked.endpoints`. This was fixed in pull request #12636 for the 6.12 guides. The following is the correct command to use:

`asadmin create-jvm-options '-Ddataverse.api.blocked.endpoints=admin,builtin-users'`

That is, the comma-separated list should be "admin,builtin-users" and not "api/admin,api/builtin-users" as described in previous versions of the guides.