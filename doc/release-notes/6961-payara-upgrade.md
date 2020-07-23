Upgrade Dataverse from Glassfish 4.1 to Payara 5
================================================

Download Payara, v5.2020.2 as of this writing:

	# curl -L -O https://github.com/payara/Payara/releases/download/payara-server-5.2020.2/payara-5.2020.2.zip
	# sha256sum payara-5.2020.2.zip 
	  1f5f7ea30901b1b4c7bcdfa5591881a700c9b7e2022ae3894192ba97eb83cc3e

Unzip it somewhere (/usr/local is a safe bet)

	# sudo unzip payara-5.2020.2.zip -d /usr/local/

Copy the Postgres driver to /usr/local/payara5/glassfish/lib

	# sudo cp /usr/local/glassfish4/glassfish/lib/postgresql-42.2.9.jar /usr/local/payara5/glassfish/lib/

Move payara5/glassfish/domains/domain1 out of the way

	# sudo mv /usr/local/payara5/glassfish/domains/domain1 /usr/local/payara5/glassfish/domains/domain1.orig

Undeploy the Dataverse web application

	# sudo /usr/local/glassfish4/bin/asadmin list-applications
	# sudo /usr/local/glassfish4/bin/asadmin undeploy dataverse

Stop Glassfish; copy domain1 to Payara

	# sudo /usr/local/glassfish4/bin/asadmin stop-domain
	# sudo cp -ar /usr/local/glassfish4/glassfish/domains/domain1 /usr/local/payara5/glassfish/domains/

Remove the Glassfish cache directories

	# sudo rm -rf /usr/local/payara5/glassfish/domains/domain1/generated/
	# sudo rm -rf /usr/local/payara5/glassfish/domains/domain1/osgi-cache/

In domain.xml:
=============

Replace the -XX:PermSize and -XX:MaxPermSize JVM options with -XX:MetaspaceSize and -XX:MaxMetaspaceSize.

        <jvm-options>-XX:MetaspaceSize=256m</jvm-options>
        <jvm-options>-XX:MaxMetaspaceSize=512m</jvm-options>

Set both Xmx and Xms at startup to avoid runtime re-allocation. Your Xmx value should likely be higher: 

	<jvm-options>-Xmx2048m</jvm-options>
	<jvm-options>-Xms2048m</jvm-options>

Add the below JVM options beneath the -Ddataverse settings:  

	<jvm-options>-Dfish.payara.classloading.delegate=false</jvm-options>
	<jvm-options>-XX:+UseG1GC</jvm-options>
	<jvm-options>-XX:+UseStringDeduplication</jvm-options>
	<jvm-options>-XX:+DisableExplicitGC</jvm-options>

Change any full pathnames /usr/local/glassfish4/... to /usr/local/payara5/... or whatever it is in your case. (Specifically check the -Ddataverse.files.directory and -Ddataverse.files.file.directory JVM options)

In domain1/config/jhove.conf, change the hard-coded /usr/local/glassfish4 path, as above.

(Optional): If you renamed your service account from glassfish to payara or appserver, update the ownership permissions:

	# sudo chown -R appserver /usr/local/payara5/glassfish/domains/domain1
	# sudo chown -R appserver /usr/local/payara5/glassfish/lib

Finally, start Payara:

	# sudo -u payara /usr/local/payara5/bin/asadmin start-domain

Deploy the Dataverse 5 warfile:

	# sudo -u payara /usr/local/payara5/bin/asadmin deploy /path/to/dataverse-5.0.war

Then restart Payara:

	# sudo -u payara /usr/local/payara5/bin/asadmin stop-domain
	# sudo -u payara /usr/local/payara5/bin/asadmin start-domain

