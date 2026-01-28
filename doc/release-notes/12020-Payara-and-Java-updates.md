### Payara 7.x and Java 21 support

This version of Dataverse has a minimal requirement of Payara 7.2026.1 and Java 21. (Later versions may work.)

#### Update Instructions:

In addition to the standard Payara update instructions and instructions related to Java 21 (do we say more than install Java 21 following standard guidance for your OS?):

Either update to put Payara in a /usr/local/payara7 dir or perhaps drop to just /usr/local/payara (at QDR /usr/local/payara is a symbolic link so we can change versions easily)
Any reference to payara6 has to change, e.g. if counter-processor is deployed, the paths in counter-processor-config.yaml and counter_daily.sh have to be updated. 

Copy the following files from the distributed domain.xml version to your domain. I don't know if all of these are required but we don't edit these by default and they have changed over time.
As with the 6.9 release, at least updating the *.p12 files is important (to get new root certs).
cacerts.p12
default-web.xml
glassfish-acc.xml
javaee.server.policy
keyfile
keystore.p12
login.conf
restrict.server.policy
server.policy
wss-server-config-1.0.xml
wss-server-config-2.0.xml

Make the following changes to your domain.xml file:

In the <config name="server-config"> element (and optionally again in the <config name="default-config"> element):
 
1) Change
<jacc-provider policy-provider="fish.payara.security.jacc.provider.PolicyProviderImpl" name="default" policy-configuration-factory-provider="fish.payara.security.jacc.provider.PolicyConfigurationFactoryImpl"></jacc-provider>
to 
<jacc-provider policy-provider="org.glassfish.exousia.modules.def.DefaultPolicy" name="default" policy-configuration-factory-provider="org.glassfish.exousia.modules.def.DefaultPolicyConfigurationFactory"></jacc-provider>

2) Add the jvm option:
<jvm-options>-Djakarta.security.jacc.PolicyFactory.provider=org.glassfish.exousia.modules.def.DefaultPolicyFactory</jvm-options>
