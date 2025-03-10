### SameSite Cookie Attribute

The SameSite cookie attribute is defined in an upcoming revision to [RFC 6265](https://datatracker.ietf.org/doc/html/rfc6265) (HTTP State Management Mechanism) called [6265bis](https://datatracker.ietf.org/doc/html/draft-ietf-httpbis-rfc6265bis-19>) ("bis" meaning "repeated"). The possible values are "None", "Lax", and "Strict".

"If no SameSite attribute is set, the cookie is treated as Lax by default" by browsers according to [MDN](https://developer.mozilla.org/en-US/docs/Web/HTTP/Cookies#controlling_third-party_cookies_with_samesite). This was the previous behavior of Dataverse, to not set the SameSite attribute.

New Dataverse installations now explicitly set to the SameSite cookie attribute to "Lax" out of the box through the installer (in the case of a "classic" installation) or through an updated base image (in the case of a Docker installation). Classic installations should follow the upgrade instructions below to bring their installation up to date with the behavior for new installations. Docker installations will automatically get the updated base image.

While you are welcome to experiment with "Strict", which is intended to help prevent Cross-Site Request Forgery (CSRF) attacks, as described in the RFC proposal and an OWASP [cheetsheet](https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html#samesite-cookie-attribute), our testing so far indicates that some functionality, such as OIDC login, seems to be incompatible with "Strict".

You should avoid the use of "None" as it is less secure that "Lax". See also [the guides](https://dataverse-guide--11210.org.readthedocs.build/en/11210/installation/config.html#samesite-cookie-attribute), https://github.com/IQSS/dataverse-security/issues/27, #11210, and the upgrade instructions below.

## Upgrade instructions

To bring your Dataverse installation in line with new installations, as described in [the guides](https://dataverse-guide--11210.org.readthedocs.build/en/11210/installation/config.html#samesite-cookie-attribute), we recommend running the following commands:

```
./asadmin set server-config.network-config.protocols.protocol.http-listener-1.http.cookie-same-site-value=Lax

./asadmin set server-config.network-config.protocols.protocol.http-listener-1.http.cookie-same-site-enabled=true
```

Please note that "None" is less secure than "Lax" and should be avoided. You can test the setting by inspecting headers with curl, looking at the JSESSIONID cookie for "SameSite=Lax" (yes, it's expected to be repeated, probably due to a bug in Payara) like this:

```
% curl -s -I http://localhost:8080 | grep JSESSIONID
Set-Cookie: JSESSIONID=6574324d75aebeb86dc96ecb3bb0; Path=/;SameSite=Lax;SameSite=Lax
```

Before making the changes above, SameSite attribute should be absent, like this:

```
% curl -s -I http://localhost:8080 | grep JSESSIONID
Set-Cookie: JSESSIONID=6574324d75aebeb86dc96ecb3bb0; Path=/
```
