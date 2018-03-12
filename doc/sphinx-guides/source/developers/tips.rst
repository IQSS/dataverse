====
Tips
====

.. contents:: |toctitle|
	:local:

Iterating on Code and Redeploying
---------------------------------

Deploy on Save
~~~~~~~~~~~~~~

Out of the box, Netbeans is configured to "Deploy on Save" which means that if you save any changes to project files such as Java classes, XHTML files, or "bundle" files (i.e. Bundle.properties), the project is recompiled and redeployed to Glassfish automatically. This behavior works well for many of us but if you don't like it, you can turn it off by right-clicking "dataverse" under the Projects tab, clicking "Run" and unchecking "Deploy on Save".

Deploying Manually
~~~~~~~~~~~~~~~~~~

For developers not using Netbeans, or deploying to a non-local system for development, code can be deployed manually.
There are four steps to this process:

1. Build the war file: ``mvn package``
2. Undeploy the Dataverse application (if necessary): ``asadmin undeploy dataverse-VERSION``
3. Copy the war file to the development server (if necessary)
4. Deploy the new code: ``asadmin deploy /path/to/dataverse-VERSION.war``

The :doc:`/installation/installation-main` section of the Installation Guide has more information on this topic.

Netbeans Connector Chrome Extension
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

For faster iteration while working on JSF pages, it is highly recommended that you install the Netbeans Connector Chrome Extension listed in the :doc:`tools` section. When you save XHTML or CSS files, you will see the changes immediately.

Preventing Glassfish from Phoning Home
--------------------------------------

By default, Glassfish reports analytics information. The administration guide suggests this can be disabled with ``asadmin create-jvm-options -Dcom.sun.enterprise.tools.admingui.NO_NETWORK=true``, should this be found to be undesirable for development purposes.

----

Previous: :doc:`troubleshooting` | Next: :doc:`version-control`
