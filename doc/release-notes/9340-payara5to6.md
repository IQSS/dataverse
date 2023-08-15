## Upgrade from Payara 5 to Payara 6

1. Download Payara 6.2023.7 as of this writing:

   `curl -L -O https://nexus.payara.fish/repository/payara-community/fish/payara/distributions/payara/6.2023.7/payara-6.2023.7.zip`

1. Unzip it to /usr/local (or your preferred location):

   `sudo unzip payara-6.2023.7.zip -d /usr/local/`

1. Change ownership of the unzipped Payara to your "service" user ("dataverse" by default):

   `sudo chown -R dataverse /usr/local/payara6`

1. Undeploy Dataverse (if deployed, using the unprivileged service account. Version 5.14 is assumed in the example below):

   `sudo -u dataverse /usr/local/payara5/bin/asadmin list-applications`

   `sudo -u dataverse /usr/local/payara5/bin/asadmin undeploy dataverse-5.14`

1. Stop Payara 5:

   `sudo /usr/local/payara5/bin/asadmin stop-domain`

1. Copy Dataverse-related lines from Payara 5 to Payara 6 domain.xml:

   `sudo cp /usr/local/payara6/glassfish/domains/domain1/config/domain.xml /usr/local/payara6/glassfish/domains/domain1/config/domain.xml.orig`

   `sudo egrep 'dataverse|doi' /usr/local/payara5/glassfish/domains/domain1/config/domain.xml > lines.txt`

   `sudo vi /usr/local/payara6/glassfish/domains/domain1/config/domain.xml`

   The lines will be part of three sections, shown below, but your content will vary.
   
   Section 1: mail resource (under `<resources>`)

   ```
   <mail-resource auth="false" host="localhost" from="do-not-reply@dev1.dataverse.org" user="dataversenotify" jndi-name="mail/notifyMailSession"></mail-resource>
   ```

   Section 2: system properties (under `<server name="server" config-ref="server-config">`)
    
   ```
   <system-property name="dataverse.db.user" value="dvnuser"></system-property>
   <system-property name="dataverse.db.host" value="localhost"></system-property>
   <system-property name="dataverse.db.port" value="5432"></system-property>
   <system-property name="dataverse.db.name" value="dvndb"></system-property>
   <system-property name="dataverse.db.password" value="dvnsecret"></system-property>
   ```

   Note: if you used the Dataverse installer, you won't have a `dataverse.db.password` property. See "Create Password Aliases" below.

   Section 3: JVM options (under `<java-config classpath-suffix="" debug-options="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=9009" system-classpath="">`)

   ```
   <jvm-options>-Ddataverse.files.directory=/usr/local/dvn/data</jvm-options>
   <jvm-options>-Ddataverse.files.file.type=file</jvm-options>
   <jvm-options>-Ddataverse.files.file.label=file</jvm-options>
   <jvm-options>-Ddataverse.files.file.directory=/usr/local/dvn/data</jvm-options>
   <jvm-options>-Ddataverse.rserve.host=localhost</jvm-options>
   <jvm-options>-Ddataverse.rserve.port=6311</jvm-options>
   <jvm-options>-Ddataverse.rserve.user=rserve</jvm-options>
   <jvm-options>-Ddataverse.rserve.password=rserve</jvm-options>
   <jvm-options>-Ddataverse.auth.password-reset-timeout-in-minutes=60</jvm-options>
   <jvm-options>-Ddataverse.timerServer=true</jvm-options>
   <jvm-options>-Ddataverse.fqdn=dev1.dataverse.org</jvm-options>
   <jvm-options>-Ddataverse.siteUrl=https://dev1.dataverse.org</jvm-options>
   <jvm-options>-Ddataverse.files.storage-driver-id=file</jvm-options>
   <jvm-options>-Ddoi.username=testaccount</jvm-options>
   <jvm-options>-Ddoi.password=notmypassword</jvm-options>
   <jvm-options>-Ddoi.baseurlstring=https://mds.test.datacite.org/</jvm-options>
   <jvm-options>-Ddoi.dataciterestapiurlstring=https://api.test.datacite.org</jvm-options>
   ```

1. Copy jhove.conf and jhoveConfig.xsd from Payara 5, edit and change payara5 to payara6

   `sudo cp /usr/local/payara5/glassfish/domains/domain1/config/jhove* /usr/local/payara6/glassfish/domains/domain1/config/`

   `sudo chown dataverse /usr/local/payara6/glassfish/domains/domain1/config/jhove*`

   `sudo -u dataverse vi /usr/local/payara6/glassfish/domains/domain1/config/jhove.conf`

1. Update systemd unit file (or other init system) to from `/usr/local/payara5` to `/usr/local/payara6`, if applicable.

1. Start Payara:

   `sudo -u dataverse /usr/local/payara6/bin/asadmin start-domain`

1. If you're using password aliases for your database, rserve and datacite jvm-options, recreate them here:

   ```
   $ echo "AS_ADMIN_ALIASPASSWORD=yourDBpassword" > /tmp/dataverse.db.password.txt
   $ sudo -u dataverse /usr/local/payara6/bin/asadmin create-password-alias --passwordfile /tmp/dataverse.db.password.txt 
   Enter the value for the aliasname operand> dataverse.db.password
   Command create-password-alias executed successfully.
   ```

   You'll want to perform simalar commands for `rserve_password_alias` and `doi_password_alias` if you're using Rserve and/or Datacite.

1. Deploy the Dataverse 6.0 warfile:

   `sudo -u dataverse /usr/local/payara6/bin/asadmin deploy /path/to/dataverse-6.0.war`

1. Check that you get a version number from Dataverse:

   `curl http://localhost:8080/api/info/version`

1. Create the network listener on port 8009

   `sudo -u dataverse /usr/local/payara6/bin/asadmin create-network-listener --protocol http-listener-1 --listenerport 8009 --jkenabled true jk-connector`

1. Perform one final Payara restart to ensure that timers are initialized properly. This example stops Payara manually, then starts Payara using systemd:

   `sudo -u dataverse /usr/local/payara6/bin/asadmin stop-domain`
   `sudo systemctl start payara`