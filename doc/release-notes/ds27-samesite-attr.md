### SameSite Cookie Attribute

The SameSite cookie attribute is defined in an upcoming revision to [RFC 6265](https://datatracker.ietf.org/doc/html/rfc6265) (HTTP State Management Mechanism) called [6265bis](https://datatracker.ietf.org/doc/html/draft-ietf-httpbis-rfc6265bis-19>) ("bis" meaning "repeated"). The possible values are "None", "Lax", and "Strict". "Strict" is intended to help prevent Cross-Site Request Forgery (CSRF) attacks, as described in the RFC proposal and an OWASP [cheetsheet](https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html#samesite-cookie-attribute). "Lax" is a middle ground between "Strict" and "None".

New Dataverse installations are now set to "Lax" out of the box by the installer (in the case of a "classic" installation) or through an updated base image (in the case of a Docker installation). See upgrade instructions below.


See also [the guides](https://dataverse-guide--11210.org.readthedocs.build/en/11210/installation/config.html#samesite-cookie-attribute), https://github.com/IQSS/dataverse-security/issues/27 and #11210.

## Upgrade instructions

To bring your Dataverse installation in line with the new "Lax" (as opposed to "None") value described in [the guides](https://dataverse-guide--11210.org.readthedocs.build/en/11210/installation/config.html#samesite-cookie-attribute), we recommend running the following commands:

```
asadmin set server-config.network-config.protocols.protocol.http-listener-1.http.cookie-same-site-value=Lax

./asadmin set server-config.network-config.protocols.protocol.http-listener-1.http.cookie-same-site-enabled=true
```
