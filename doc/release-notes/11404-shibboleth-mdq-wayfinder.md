### For Dataverse instances that use Shibboleth as members of InCommon federation

Please note that most of the known Dataverse instances that support Shibboleth logins do so without being part of InCommon, and therefore are not affected. All such instances will be able to continue using the old login workflow without needing to make any configuration changes. 

For the relatively few instances using InCommon: Since InCommon discontinued their old-style federation metadata feed, a new Shibboleth implementation have been added to utilize the recommended replacements: the MDQ protocol and the WayFinder service. In order to continue using InCommon, such instances will need to modify their shibd configuration and their registration with Incommon, plus set a new feature flag. See the upgrade instruction for details.


### New Settings

- dataverse.feature.shibboleth-use-wayfinder
- dataverse.feature.shibboleth-use-localhost

### For the Upgrade Instruction:

[(strip this from the real release note) this should be the very last of the optional upgrade steps; as it's been pointed out to me that there may not be any instances affected by this aside from Harvard and UNC, both of which have been active participants in the development of the underlying code and the upgrade process below. ... which kind of makes including them in the release note somewhat unnecessary (?). but I figure we should include them anyway, in case there's an instance out there we are not aware of. - L.A.]


If your instance is offering institutional Shibboleth logins as part of the InCommon federation, you must make some changes to your service configuration:

Note that if your Dataverse instance is using Shibboleth outside of InCommon, your login workflow should continue working unchanged, so please skip this section.

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

c. Set the feature flag `dataverse.feature.shibboleth-use-wayfinder=true`. 

