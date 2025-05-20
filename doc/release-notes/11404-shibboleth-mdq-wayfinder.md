### For Dataverse instances using Shibboleth

Since the old-style federation metadata feed was discontinued by InCommon, the Shibboleth login components have been re-implemented to utilize the recommended replacements: the MDQ protocol and the WayFinder service. From now on, this will be the default behavior of the login page for shib. users. Dataverse instances using Shibboleth as members of the InCommon federation will need to modify their shibd configuration and, possibly, their registration with Incommon. See the upgrade instruction for details.

It is also possible for a Dataverse instance to continue using the old login page mechanism (the most likely use case for this would be if you are using Shibboleth without being part of InCommon, for example, by running shibd with a static list of known metadata providers). In this case, set the feature flag `dataverse.feature.shibboleth-use-discofeed=true` to preserve the legacy workflow as is. 

### New Settings

- dataverse.feature.shibboleth-use-discofeed
- dataverse.feature.shibboleth-use-localhost

### For the Upgrade Instruction:

If your instance is offering institutional Shibboleth logins as part of the InCommon federation, you must make some changes to your service configuration.

a. Configure your Service Provider (SP) in the InCommon Federation Manager to use WayFinder following [their instructions](https://spaces.at.internet2.edu/display/federation/how-to-configure-service-to-use-wayfinder).

b. Reconfigure your locally-running `shibd` service to use WayFinder and the new MDQ metadata retrieval protocol.
Download and place the new [production signing key](https://spaces.at.internet2.edu/display/MDQ/production-mdq-signing-key) in `/etc/shibboleth` and name it `inc-md-cert-mdq.pem`.
Change the `SSO` and `MetadataProvider` sections of the `/etc/shibboleth/shibboleth2.xml` configuration file as follows:

```
<SSO discoveryProtocol="SAMLDS" discoveryURL="https://wayf.incommonfederation.org/DS/WAYF">
     SAML2 SAML1
</SSO>
```
and
```
<MetadataProvider type="MDQ" id="incommon" ignoreTransport="true" cacheDirectory="inc-mdq-cache"
  maxCacheDuration="86400" minCacheDuration="60" baseUrl="https://mdq.incommon.org/">
    <MetadataFilter type="Signature" certificate="inc-md-cert-mdq.pem"/>
    <MetadataFilter type="RequireValidUntil" maxValidityInterval="1209600"/>
</MetadataProvider>
```
See [How to configure a Shibboleth service provider (SP) to use MDQ](https://spaces.at.internet2.edu/display/MDQ/how-to-configure-shib-sp-to-use-mdq) for more information.


If your Dataverse instance is using Shibboleth without being a member of the InCommon federation, you can preserve your working configuration as is and configure Dataverse to continue using the old-style login workflow by setting the feature flag `dataverse.feature.shibboleth-use-discofeed=true`.

