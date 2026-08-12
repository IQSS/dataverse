## Upgrade Instructions

(Put this near the end.)

1. The [Dataverse 6.10 release notes](https://github.com/IQSS/dataverse/releases/tag/v6.10) explained how to upgrade from Payara 6 to 7 and mentioned that any other settings that were customized also need to be migrated. Several examples were given but they did not originally include the commonly edited setting for the number of seconds before a request times out. The 6.10 and 6.11 release notes have been updated to now include this example. As described in [the guides](https://guides.dataverse.org/en/6.12/installation/config.html#http-request-timeout-seconds) you might want to shorten or lengthen this value. The default is 900 seconds (15 minutes). If you set it to an hour using the command from the guides (`asadmin set server-config.network-config.protocols.protocol.http-listener-1.http.request-timeout-seconds=3600`) you should see a line like this in your domain.xml file:

   `<http request-timeout-seconds="3600" max-connections="250" default-virtual-server="server">`

   See also #12543 and #12606.
