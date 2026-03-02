## Release Highlights

This release contains major upgrades to core components. Detailed upgrade instructions can be found below.

- Infrastructure updates
  - Payara has been upgraded from version 6 to 7
  - Java has been upgraded from version 17 to 21

## Upgrade Instructions

### Upgrade from Java 17 to Java 21

Java 21 is now required to run Dataverse. Solr can run under Java 17 or Java 21 but the latter is recommended and the switch is shown below. In preparation for the Java upgrade, the steps below instruct you to stop both Dataverse (Payara) and Solr.

1. Undeploy Dataverse from Payara 6, if deployed, using the unprivileged service account ("dataverse", by default).

   `sudo -u dataverse /usr/local/payara6/bin/asadmin list-applications`

   `sudo -u dataverse /usr/local/payara6/bin/asadmin undeploy dataverse-6.9`

1. Stop Payara 6.

   `sudo -u dataverse /usr/local/payara6/bin/asadmin stop-domain`

1. Stop Solr.

   `sudo systemctl stop solr.service`

1. Install Java 21.

   Assuming you are using RHEL or a derivative such as Rocky Linux:

   `sudo dnf install java-21-openjdk`

1. Set Java 21 as the default.

   Assuming you are using RHEL or a derivative such as Rocky Linux:

   `sudo alternatives --config java`

1. Test that Java 21 is the default.

   `java -version`

### Upgrade from Payara 6 to Payara 7

If you are running Payara as a non-root user (and you should be!), **remember not to execute the commands below as root**. Use `sudo` to change to that user first. For example, `sudo -i -u dataverse` if `dataverse` is your dedicated application user.

The steps below reuse your existing domain directory with the new distribution of Payara. You may also want to review the Payara upgrade instructions as it could be helpful during any troubleshooting:
[Payara Release Notes](https://docs.payara.fish/community/docs/Release%20Notes/Release%20Notes%207.2026.2.html).
We also recommend you ensure you followed all update instructions from the past releases regarding Payara.
(The most recent Payara update was for [Dataverse 6.9](https://github.com/IQSS/dataverse/releases/tag/v6.9).)

1. Download Payara 7.2026.2.

   `curl -L -O https://nexus.payara.fish/repository/payara-community/fish/payara/distributions/payara/7.2026.2/payara-7.2026.2.zip`

1. Unzip it to /usr/local (or your preferred location).

   `sudo unzip payara-7.2026.2.zip -d /usr/local/`

1. Set permission for the service account ("dataverse" by default).

   `sudo chown -R root:root /usr/local/payara7`
   `sudo chown dataverse /usr/local/payara7/glassfish/lib`
   `sudo chown -R dataverse:dataverse /usr/local/payara7/glassfish/domains/domain1`

1. Undeploy Dataverse, if deployed, using the unprivileged service account.

   `sudo -u dataverse /usr/local/payara6/bin/asadmin list-applications`

   `sudo -u dataverse /usr/local/payara6/bin/asadmin undeploy dataverse-6.9`

1. Stop Payara 6, if running.

   Payara 6 should already be stopped because of the Java upgrade above.

   `sudo -u dataverse /usr/local/payara6/bin/asadmin stop-domain`

1. Start and stop Payara 7 to let it reformat its domain.xml file.

   When Payara starts, it will reformat its domain.xml file, which we will be backing up and editing. By stopping and starting Payara, the `diff` between the backup and the edited file will be easier to read.

   `sudo -u dataverse /usr/local/payara7/bin/asadmin start-domain`
   `sudo -u dataverse /usr/local/payara7/bin/asadmin stop-domain`

1. Copy Dataverse-related lines from Payara 6 to Payara 7 domain.xml.

   First, back up the Payara 7 domain.xml file we will be editing.

   `sudo -u dataverse cp -a /usr/local/payara7/glassfish/domains/domain1/config/domain.xml /usr/local/payara7/glassfish/domains/domain1/config/domain.xml.orig`

   Save the Dataverse-related lines from Payara 6 to a text file. Note that "doi" is for legacy settings like "doi.baseurlstring" that should be [converted](https://guides.dataverse.org/en/6.10/installation/config.html#legacy-single-pid-provider-dataverse-pid-datacite-mds-api-url) to modern equivalents if they are still present.

   `sudo egrep 'dataverse|doi' /usr/local/payara6/glassfish/domains/domain1/config/domain.xml > lines.txt`

   Edit the Payara 7 domain.xml and insert the Dataverse-related lines. More details are below.

   `sudo vi /usr/local/payara7/glassfish/domains/domain1/config/domain.xml`

   If any JVM options reference the old payara6 path (`/usr/local/payara6`) be sure to change it to payara7.

   The lines will appear in two sections, examples shown below (but your content will vary).

   Section 1: system properties (under `<server name="server" config-ref="server-config">`)

   ```
   <system-property name="dataverse.db.user" value="dvnuser"></system-property>
   <system-property name="dataverse.db.host" value="localhost"></system-property>
   <system-property name="dataverse.db.port" value="5432"></system-property>
   <system-property name="dataverse.db.name" value="dvndb"></system-property>
   <system-property name="dataverse.mail.system-email" value="noreply@dev1.dataverse.org"></system-property>
   <system-property name="dataverse.mail.mta.host" value="localhost"></system-property>
   ```

   Note: If you used the Dataverse installer, you won't have a `dataverse.db.password` property. See "Create password aliases" below.

   Section 2: JVM options (under `<java-config classpath-suffix="" system-classpath="" debug-options="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=9009">`, the one under `<config name="server-config">`, not under `<config name="default-config">`)

   ```
   <jvm-options>-Ddataverse.files.directory=/usr/local/dvn/data</jvm-options>
   <jvm-options>-Ddataverse.files.file.type=file</jvm-options>
   <jvm-options>-Ddataverse.files.file.label=file</jvm-options>
   <jvm-options>-Ddataverse.files.file.directory=/usr/local/dvn/data</jvm-options>
   <jvm-options>-Ddataverse.rserve.host=localhost</jvm-options>
   <jvm-options>-Ddataverse.rserve.port=6311</jvm-options>
   <jvm-options>-Ddataverse.rserve.user=rserve</jvm-options>
   <jvm-options>-Ddataverse.rserve.password=rserve</jvm-options>
   <jvm-options>-Ddataverse.fqdn=dev1.dataverse.org</jvm-options>
   <jvm-options>-Ddataverse.siteUrl=https://dev1.dataverse.org</jvm-options>
   <jvm-options>-Ddataverse.auth.password-reset-timeout-in-minutes=60</jvm-options>
   <jvm-options>-Ddataverse.pid.providers=fake</jvm-options>
   <jvm-options>-Ddataverse.pid.fake.type=FAKE</jvm-options>
   <jvm-options>-Ddataverse.pid.fake.label=Fake DOI Provider</jvm-options>
   <jvm-options>-Ddataverse.pid.fake.authority=10.5072</jvm-options>
   <jvm-options>-Ddataverse.pid.fake.shoulder=FK2/</jvm-options>
   <jvm-options>-Ddataverse.pid.default-provider=fake</jvm-options>
   <jvm-options>-Ddataverse.timerServer=true</jvm-options>
   <jvm-options>-Ddataverse.files.storage-driver-id=file</jvm-options>
   <jvm-options>-Ddataverse.mail.system-email=noreply@dev1.dataverse.org</jvm-options>
   <jvm-options>-Ddataverse.files.uploads=/tmp</jvm-options>
   <jvm-options>-Ddataverse.feature.api-session-auth=0</jvm-options>
   <jvm-options>-Ddataverse.spi.exporters.directory=/var/lib/dataverse/exporters</jvm-options>
   ```

1. Comment out JSP servlet mappings.

   First, backup the file you'll be editing.

   `sudo cp -a /usr/local/payara7/glassfish/domains/domain1/config/default-web.xml /usr/local/payara7/glassfish/domains/domain1/config/default-web.xml.orig`

   Then, edit the file and follow the instructions below.

   `sudo vi /usr/local/payara7/glassfish/domains/domain1/config/default-web.xml`

   Comment out the following section and save the file.

   ```
   <servlet-mapping>
     <servlet-name>jsp</servlet-name>
     <url-pattern>*.jsp</url-pattern>
     <url-pattern>*.jspx</url-pattern>
   </servlet-mapping>
   ```

1. Check the `Xmx` setting in `domain.xml`.

   Under `/usr/local/payara7/glassfish/domains/domain1/config/domain.xml`, check the `Xmx` setting under `<config name="server-config">`, where you put the Dataverse-related JVM options, not the one under `<config name="default-config">`. This sets the JVM heap size; a good rule of thumb is half of your system's total RAM. You may specify the value in MB (`8192m`) or GB (`8g`).

1. Copy `jhove.conf` and `jhoveConfig.xsd` from Payara 6, edit and change `payara6` to `payara7`.

   `sudo bash -c 'cp /usr/local/payara6/glassfish/domains/domain1/config/jhove* /usr/local/payara7/glassfish/domains/domain1/config'`

   `sudo bash -c 'chown dataverse /usr/local/payara7/glassfish/domains/domain1/config/jhove*'`

   `sudo -u dataverse vi /usr/local/payara6/glassfish/domains/domain1/config/jhove.conf`

1. Copy logos from Payara 6 to Payara 7.

   These logos are for collections (dataverses).

   `sudo -u dataverse cp -r /usr/local/payara6/glassfish/domains/domain1/docroot/logos /usr/local/payara7/glassfish/domains/domain1/docroot`

1. If you are using Make Data Count (MDC), make various updates.

   Your `:MDCLogPath` database setting might be pointing to a Payara 6 directory such as `/usr/local/payara6/glassfish/domains/domain1/logs`. If so, edit this to be payara7.

   `curl -X PUT -d '/usr/local/payara7/glassfish/domains/domain1/logs' http://localhost:8080/api/admin/settings/:MDCLogPath`

   You'll probably want to copy your logs over as well.

   Update Counter Processer to put payara7 in the counter-processor-config.yaml and counter_daily.sh. See https://guides.dataverse.org/en/6.10/admin/make-data-count.html

1. If you've enabled access logging or any other site-specific configuration, be sure to preserve them. For instance, the default domain.xml includes

   ```
   <http-service>
   <access-log></access-log>
   ```

   but you may wish to include

   ```
   <http-service access-logging-enabled="true">
   <access-log format="%client.name% %datetime% %request% %status% %response.length% %header.user-agent% %header.referer% %cookie.JSESSIONID% %header.x-forwarded-for%"></access-log>
   ```

   Be sure to keep a previous copy of your domain.xml for reference.

1. Update systemd unit file (or other init system) from `/usr/local/payara6` to `/usr/local/payara7`, if applicable.

    This example is for systemd:

   `sudo vi /usr/lib/systemd/system/payara.service`

   See also https://guides.dataverse.org/en/6.10/installation/prerequisites.html#launching-payara-on-system-boot

1. Start Payara 7.

   `sudo -u dataverse /usr/local/payara7/bin/asadmin start-domain`

1. Create password aliases for your database, rserve and datacite jvm-options, if you're using them.

   `echo "AS_ADMIN_ALIASPASSWORD=yourDBpassword" > /tmp/dataverse.db.password.txt`

   `sudo -u dataverse /usr/local/payara7/bin/asadmin create-password-alias --passwordfile /tmp/dataverse.db.password.txt`

   When you are prompted "Enter the value for the aliasname operand", enter `dataverse.db.password`

   You should see "Command create-password-alias executed successfully."

   You'll want to perform similar commands for `rserve_password_alias` and `doi_password_alias` if you're using Rserve and/or DataCite.

1. Create the network listener on port 8009.

   `sudo -u dataverse /usr/local/payara7/bin/asadmin create-network-listener --protocol http-listener-1 --listenerport 8009 --jkenabled true jk-connector`

1. Deploy the Dataverse 6.10 war file.

   `wget https://github.com/IQSS/dataverse/releases/download/v6.10/dataverse-6.10.war`

   `sudo -u dataverse /usr/local/payara7/bin/asadmin deploy dataverse-6.10.war`

1. Check that you get a version number from Dataverse.

   This is just a sanity check that Dataverse has been deployed properly.

   `curl http://localhost:8080/api/info/version`

1. Perform one final Payara restart to ensure that timers are initialized properly.

   `sudo -u dataverse /usr/local/payara7/bin/asadmin stop-domain`

   `sudo -u dataverse /usr/local/payara7/bin/asadmin start-domain`

1. Start Solr under Java 21 now that it's the default.

   `sudo systemctl start solr.service`

1. For installations with internationalization or text customizations:

Please remember to update translations via [Dataverse language packs](https://github.com/GlobalDataverseCommunityConsortium/dataverse-language-packs).

If you have text customizations you can get the latest English files from <https://github.com/IQSS/dataverse/tree/v6.10/src/main/java/propertyFiles>.