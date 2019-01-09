====
Tips
====

If you just followed the steps in :doc:`dev-environment` for the first time, you will need to get set up to deploy code to Glassfish. Below you'll find other tips as well.

.. contents:: |toctitle|
	:local:

Iterating on Code and Redeploying
---------------------------------

When you followed the steps in the :doc:`dev-environment` section, the war file was deployed to Glassfish by the ``install`` script. That's fine but once you're ready to make a change to the code you will need to get comfortable with undeploying and redeploying code (a war file) to Glassfish.

It's certainly possible to manage deployment and undeployment of the war file via the command line using the ``asadmin`` command that ships with Glassfish (that's what the ``install`` script uses and the steps are documented below), but we recommend getting set up with and IDE such as Netbeans to manage deployment for you.

Undeploy the war File from the ``install`` Script
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Because the initial deployment of the war file was done outside of Netbeans by the ``install`` script, it's a good idea to undeploy that war file to give Netbeans a clean slate to work with.

Assuming you installed Glassfish in ``/usr/local/glassfish4``, run the following ``asadmin`` command to see the version of Dataverse that the ``install`` script deployed:

``/usr/local/glassfish4/bin/asadmin list-applications``

You will probably see something like ``dataverse-4.8.5  <ejb, web>`` as the output. To undeploy, use whichever version you see like this:

``/usr/local/glassfish4/bin/asadmin undeploy dataverse-4.8.5``

Now that Glassfish doesn't have anything deployed, we can proceed with getting Netbeans set up to deploy the code.

Add Glassfish 4.1 as a Server in Netbeans
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Dataverse only works with a specific version of Glassfish (see https://github.com/IQSS/dataverse/issues/2628 ) so you need to make sure Netbeans is deploying to that version rather than a newer version of Glassfish that may have come bundled with Netbeans.

Launch Netbeans and click "Tools" and then "Servers". Click "Add Server" and select "Glassfish Server" and set the installation location to ``/usr/local/glassfish4``. The default are fine so you can click "Next" and "Finish". To avoid confusing, click "Remove Server" on the newer version of Glassfish that came bundled with Glassfish.

Please note that if you are on a Mac, Netbeans may be unable to start Glassfish due to proxy settings in Netbeans. Go to the "General" tab in Netbeans preferences and click "Test connection" to see if you are affected. If you get a green checkmark, you're all set. If you get a red exclamation mark, change "Proxy Settings" to "No Proxy" and retest. A more complicated answer having to do with changing network settings is available at https://discussions.apple.com/thread/7680039?answerId=30715103022#30715103022 and the bug is also described at https://netbeans.org/bugzilla/show_bug.cgi?id=268076

At this point you can manage Glassfish using Netbeans. Click "Window" and then "Services". Expand "Servers" and right-click Glassfish to stop and then start it so that it appears in the Output window. Note that you can expand "Glassfish" and "Applications" to see if any applications are deployed.

Ensure that Dataverse Will Be Deployed to Glassfish 4.1
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Click "Window" and then "Projects". Click "File" and then "Project Properties (dataverse)". Click "Run" and change "Server" from "No Server Selected" to your installation of Glassfish 4.1. Click OK.

Make a Small Change to the Code
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Let's make a tiny change to the code, compile the war file, deploy it, and verify that that we can see the change.

One of the smallest changes we can make is adjusting the build number that appears in the lower right of every page.

From the root of the git repo, run the following command to set the build number to the word "hello" (or whatever you want):

``scripts/installer/custom-build-number hello``

This should update or place a file at ``src/main/java/BuildNumber.properties``.

Then, from Netbeans, click "Run" and then "Clean and Build Project (dataverse)". After this completes successfully, click "Run" and then "Run Project (dataverse)"

Confirm the Change Was Deployed
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

After deployment, check the build number in the lower right to make sure it has been customized. You can also check the build number by running the following command:

``curl http://localhost:8080/api/info/version``

If you can see the change, great! Please go fix a bug or work on a feature! :)

Actually, before you start changing any code, you should create a branch as explained in the :doc:`version-control` section.

While it's fresh in your mind, if you have any suggestions on how to make the setup of a development environment easier, please get in touch!

Netbeans Connector Chrome Extension
-----------------------------------

For faster iteration while working on JSF pages, it is highly recommended that you install the Netbeans Connector Chrome Extension listed in the :doc:`tools` section. When you save XHTML or CSS files, you will see the changes immediately. Hipsters call this "hot reloading". :)

Database Schema Exploration
---------------------------

With over 100 tables, the Dataverse PostgreSQL database ("dvndb") can be somewhat daunting for newcomers. Here are some tips for coming up to speed. (See also the :doc:`sql-upgrade-scripts` section.)

pgAdmin
~~~~~~~~

Back in the :doc:`dev-environment` section, we had you install pgAdmin, which can help you explore the tables and execute SQL commands. It's also listed in the :doc:`tools` section.

SchemaSpy
~~~~~~~~~

SchemaSpy is a tool that creates a website of entity-relationship diagrams based on your database.

As part of our build process for running integration tests against the latest code in the "develop" branch, we drop the database on the "phoenix" server, recreate the database by deploying the latest war file, and run SchemaSpy to create the following site: http://phoenix.dataverse.org/schemaspy/latest/relationships.html

To run this command on your laptop, download SchemaSpy and take a look at the syntax in ``scripts/deploy/phoenix.dataverse.org/post``

To read more about the phoenix server, see the :doc:`testing` section.

Deploying With ``asadmin``
--------------------------

Sometimes you want to deploy code without using Netbeans or from the command line on a server you have ssh'ed into.

For the ``asadmin`` commands below, we assume you have already changed directories to ``/usr/local/glassfish4/glassfish/bin`` or wherever you have installed Glassfish.

There are four steps to this process:

1. Build the war file: ``mvn package``
2. Check which version of Dataverse is deployed: ``./asadmin list-applications``
3. Undeploy the Dataverse application (if necessary): ``./asadmin undeploy dataverse-VERSION``
4. Copy the war file to the server (if necessary)
5. Deploy the new code: ``./asadmin deploy /path/to/dataverse-VERSION.war``

Running the Dataverse ``install`` Script in Non-Interactive Mode
----------------------------------------------------------------

Rather than running the installer in "interactive" mode, it's possible to put the values in a file. See "non-interactive mode" in the :doc:`/installation/installation-main` section of the Installation Guide.

Preventing Glassfish from Phoning Home
--------------------------------------

By default, Glassfish reports analytics information. The administration guide suggests this can be disabled with ``./asadmin create-jvm-options -Dcom.sun.enterprise.tools.admingui.NO_NETWORK=true``, should this be found to be undesirable for development purposes.

Solr
----

Once some dataverses, datasets, and files have been created and indexed, you can experiment with searches directly from Solr at http://localhost:8983/solr/#/collection1/query and look at the JSON output of searches, such as this wildcard search: http://localhost:8983/solr/collection1/select?q=*%3A*&wt=json&indent=true . You can also get JSON output of static fields Solr knows about: http://localhost:8983/solr/schema/fields

You can simply double-click "start.jar" rather that running ``java -jar start.jar`` from the command line. Figuring out how to stop Solr after double-clicking it is an exercise for the reader.

Git
---

Set Up SSH Keys
~~~~~~~~~~~~~~~

You can use git with passwords over HTTPS, but it's much nicer to set up SSH keys. https://github.com/settings/ssh is the place to manage the ssh keys GitHub knows about for you. That page also links to a nice howto: https://help.github.com/articles/generating-ssh-keys

From the terminal, ``ssh-keygen`` will create new ssh keys for you:

- private key: ``~/.ssh/id_rsa`` - It is very important to protect your private key. If someone else acquires it, they can access private repositories on GitHub and make commits as you! Ideally, you'll store your ssh keys on an encrypted volume and protect your private key with a password when prompted for one by ``ssh-keygen``. See also "Why do passphrases matter" at https://help.github.com/articles/generating-ssh-keys

- public key: ``~/.ssh/id_rsa.pub`` - After you've created your ssh keys, add the public key to your GitHub account.

Git on Mac
~~~~~~~~~~

On a Mac, you won't have git installed unless you have "Command Line Developer Tools" installed but running ``git clone`` for the first time will prompt you to install them.

----

Previous: :doc:`dev-environment` | Next: :doc:`troubleshooting`
