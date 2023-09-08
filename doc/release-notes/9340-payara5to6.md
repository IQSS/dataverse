## Upgrade from Payara 5 to Payara 6

1. Download Payara 6.2023.8 as of this writing:

   `curl -L -O https://nexus.payara.fish/repository/payara-community/fish/payara/distributions/payara/6.2023.8/payara-6.2023.8.zip`

1. Unzip it to /usr/local (or your preferred location):

   `sudo unzip payara-6.2023.8.zip -d /usr/local/`

1. Change ownership of the unzipped Payara to your "service" user ("dataverse" by default):

   `sudo chown -R dataverse /usr/local/payara6`

1. Undeploy Dataverse (if deployed, using the unprivileged service account. Version 5.14 is assumed in the example below):

   `sudo -u dataverse /usr/local/payara5/bin/asadmin list-applications`

   `sudo -u dataverse /usr/local/payara5/bin/asadmin undeploy dataverse-5.14`

1. Stop Payara 5:

   `sudo -u dataverse /usr/local/payara5/bin/asadmin stop-domain`

1. Copy Dataverse-related lines from Payara 5 to Payara 6 domain.xml:

   `sudo -u dataverse cp /usr/local/payara6/glassfish/domains/domain1/config/domain.xml /usr/local/payara6/glassfish/domains/domain1/config/domain.xml.orig`

   `sudo egrep 'dataverse|doi' /usr/local/payara5/glassfish/domains/domain1/config/domain.xml > lines.txt`

   `sudo vi /usr/local/payara6/glassfish/domains/domain1/config/domain.xml`

   The lines will appear in two sections, examples shown below (but your content will vary).
   
   Section 1: system properties (under `<server name="server" config-ref="server-config">`)
    
   ```
   <system-property name="dataverse.db.user" value="dvnuser"></system-property>
   <system-property name="dataverse.db.host" value="localhost"></system-property>
   <system-property name="dataverse.db.port" value="5432"></system-property>
   <system-property name="dataverse.db.name" value="dvndb"></system-property>
   <system-property name="dataverse.db.password" value="dvnsecret"></system-property>
   ```

   Note: if you used the Dataverse installer, you won't have a `dataverse.db.password` property. See "Create password aliases" below.

   Section 2: JVM options (under `<java-config classpath-suffix="" debug-options="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=9009" system-classpath="">`, the one under `<config name="server-config">`, not under `<config name="default-config">`)

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

1. Check the `Xmx` setting in `/usr/local/payara6/glassfish/domains/domain1/config/domain.xml`. (The one under `<config name="server-config">`, where you put the JVM options, not the one under `<config name="default-config">`.) Note that there are two such settings, and you want to adjust the one in the stanza with Dataverse options. This sets the JVM heap size; a good rule of thumb is half of your system's total RAM. You may specify the value in MB (`8192m`) or GB (`8g`).

1. Copy jhove.conf and jhoveConfig.xsd from Payara 5, edit and change payara5 to payara6

   `sudo cp /usr/local/payara5/glassfish/domains/domain1/config/jhove* /usr/local/payara6/glassfish/domains/domain1/config/`

   `sudo chown dataverse /usr/local/payara6/glassfish/domains/domain1/config/jhove*`

   `sudo -u dataverse vi /usr/local/payara6/glassfish/domains/domain1/config/jhove.conf`

1. Copy logos from Payara 5 to Payara 6

   These logos are for collections (dataverses).

   `sudo -u dataverse cp -r /usr/local/payara5/glassfish/domains/domain1/docroot/logos /usr/local/payara6/glassfish/domains/domain1/docroot`

1. If you are using Make Data Count (MDC), edit :MDCLogPath

   Your `:MDCLogPath` database setting might be pointing to a Payara 5 directory such as `/usr/local/payara5/glassfish/domains/domain1/logs`. If so, edit this to be Payara 6. You'll probably want to copy your logs over as well.

1. Update systemd unit file (or other init system) from `/usr/local/payara5` to `/usr/local/payara6`, if applicable.

1. Start Payara:

   `sudo -u dataverse /usr/local/payara6/bin/asadmin start-domain`

1. Create a Java mail resource, replacing "localhost" for mailhost with your mail relay server, and replacing "localhost" for fromaddress with the FQDN of your Dataverse server:

   `sudo -u dataverse /usr/local/payara6/bin/asadmin create-javamail-resource --mailhost "localhost" --mailuser "dataversenotify" --fromaddress "do-not-reply@localhost" mail/notifyMailSession`

1. Create password aliases for your database, rserve and datacite jvm-options, if you're using them:

   ```
   $ echo "AS_ADMIN_ALIASPASSWORD=yourDBpassword" > /tmp/dataverse.db.password.txt
   $ sudo -u dataverse /usr/local/payara6/bin/asadmin create-password-alias --passwordfile /tmp/dataverse.db.password.txt 
   Enter the value for the aliasname operand> dataverse.db.password
   Command create-password-alias executed successfully.
   ```

   You'll want to perform similar commands for `rserve_password_alias` and `doi_password_alias` if you're using Rserve and/or Datacite.

1. Enable workaround for FISH-7722:

   The following workaround is for https://github.com/payara/Payara/issues/6337

   `sudo -u dataverse /usr/local/payara6/bin/asadmin create-jvm-options --add-opens=java.base/java.io=ALL-UNNAMED`

1. Create the network listener on port 8009

   `sudo -u dataverse /usr/local/payara6/bin/asadmin create-network-listener --protocol http-listener-1 --listenerport 8009 --jkenabled true jk-connector`

1. Deploy the Dataverse 6.0 warfile:

   `sudo -u dataverse /usr/local/payara6/bin/asadmin deploy /path/to/dataverse-6.0.war`

1. Check that you get a version number from Dataverse:

   `curl http://localhost:8080/api/info/version`

1. Perform one final Payara restart to ensure that timers are initialized properly:

   `sudo -u dataverse /usr/local/payara6/bin/asadmin stop-domain`

   `sudo -u dataverse /usr/local/payara6/bin/asadmin start-domain`
