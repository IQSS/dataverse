====
Tips
====

If you just followed the steps in :doc:`classic-dev-env` for the first time, you will need to get set up to deploy code to your app server. Below you'll find other tips as well.

.. contents:: |toctitle|
	:local:

Iterating on Code and Redeploying
---------------------------------

When you followed the steps in the :doc:`classic-dev-env` section, the war file was deployed to Payara by the Dataverse Software installation script. That's fine but once you're ready to make a change to the code you will need to get comfortable with undeploying and redeploying code (a war file) to Payara.

It's certainly possible to manage deployment and undeployment of the war file via the command line using the ``asadmin`` command that ships with Payara (that's what the Dataverse Software installation script uses and the steps are documented below), but we recommend getting set up with an IDE such as Netbeans to manage deployment for you.

Undeploy the war File from the Dataverse Software Installation Script
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Because the initial deployment of the war file was done outside of Netbeans by the Dataverse Software installation script, it's a good idea to undeploy that war file to give Netbeans a clean slate to work with.

Assuming you installed Payara in ``/usr/local/payara6``, run the following ``asadmin`` command to see the version of the Dataverse Software that the Dataverse Software installation script deployed:

``/usr/local/payara6/bin/asadmin list-applications``

You will probably see something like ``dataverse-5.0 <ejb, web>`` as the output. To undeploy, use whichever version you see like this:

``/usr/local/payara6/bin/asadmin undeploy dataverse-5.0``

Now that Payara doesn't have anything deployed, we can proceed with getting Netbeans set up to deploy the code.

Add Payara as a Server in Netbeans
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Launch Netbeans and click "Tools" and then "Servers". Click "Add Server" and select "Payara Server" and set the installation location to ``/usr/local/payara6``. The defaults are fine so you can click "Next" and "Finish".

Please note that if you are on a Mac, Netbeans may be unable to start Payara due to proxy settings in Netbeans. Go to the "General" tab in Netbeans preferences and click "Test connection" to see if you are affected. If you get a green checkmark, you're all set. If you get a red exclamation mark, change "Proxy Settings" to "No Proxy" and retest. A more complicated answer having to do with changing network settings is available at https://discussions.apple.com/thread/7680039?answerId=30715103022#30715103022 and the bug is also described at https://netbeans.org/bugzilla/show_bug.cgi?id=268076

At this point you can manage Payara using Netbeans. Click "Window" and then "Services". Expand "Servers" and right-click Payara to stop and then start it so that it appears in the Output window. Note that you can expand "Payara" and "Applications" to see if any applications are deployed.

Ensure that the Dataverse Software Will Be Deployed to Payara
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Click "Window" and then "Projects". Click "File" and then "Project Properties (dataverse)". Click "Run" and change "Server" from "No Server Selected" to your installation of Payara. Click OK.

.. _custom_build_num_script:

Make a Small Change to the Code
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Let's make a tiny change to the code, compile the war file, deploy it, and verify that that we can see the change.

One of the smallest changes we can make is adjusting the build number that appears in the lower right of every page.

From the root of the git repo, run the following command to set the build number to the word "hello" (or whatever you want):

``scripts/installer/custom-build-number hello``

This should update or place a file at ``src/main/java/BuildNumber.properties``.

(See also :ref:`auto-custom-build-number` for other ways of changing the build number.)

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

Thumbnails
----------

In order for thumnails to be generated for PDFs, you need to install ImageMagick and configure Dataverse to use the ``convert`` binary.

Assuming you're using Homebrew:

``brew install imagemagick``

Then configure the JVM option mentioned in :ref:`install-imagemagick` to the path to ``convert`` which for Homebrew is usually ``/usr/local/bin/convert``.

Database Schema Exploration
---------------------------

With over 100 tables, the Dataverse PostgreSQL database can be somewhat daunting for newcomers. Here are some tips for coming up to speed. (See also the :doc:`sql-upgrade-scripts` section.)

.. _db-name-creds:

Database Name and Credentials
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The default database name and credentials depends on how you set up your dev environment.

.. list-table::
   :header-rows: 1
   :align: left

   * - MPCONFIG Key
     - Docker
     - Classic
   * - dataverse.db.name
     - ``dataverse``
     - ``dvndb``
   * - dataverse.db.user
     - ``dataverse``
     - ``dvnapp``
   * - dataverse.db.password
     - ``secret``
     - ``secret``

Here's an example of using these credentials from within the PostgreSQL container (see :doc:`/container/index`):

.. code-block:: bash

    pdurbin@beamish dataverse % docker exec -it postgres-1 bash
    root@postgres:/# export PGPASSWORD=secret
    root@postgres:/# psql -h localhost -U dataverse dataverse
    psql (16.3 (Debian 16.3-1.pgdg120+1))
    Type "help" for help.
    
    dataverse=# select id,alias from dataverse limit 1;
     id | alias 
    ----+-------
      1 | root
    (1 row)

See also :ref:`database-persistence` in the Installation Guide.

pgAdmin
~~~~~~~

If you followed the :doc:`classic-dev-env` section, we had you install pgAdmin, which can help you explore the tables and execute SQL commands. It's also listed in the :doc:`tools` section.

SchemaSpy
~~~~~~~~~

SchemaSpy is a tool that creates a website of entity-relationship diagrams based on your database.

We periodically run SchemaSpy and publish the output: https://guides.dataverse.org/en/6.2/schemaspy/index.html

To run SchemaSpy locally, take a look at the syntax in ``scripts/deploy/phoenix.dataverse.org/post``.

Deploying With ``asadmin``
--------------------------

Sometimes you want to deploy code without using Netbeans or from the command line on a server you have ssh'ed into.

For the ``asadmin`` commands below, we assume you have already changed directories to ``/usr/local/payara6/glassfish/bin`` or wherever you have installed Payara.

There are four steps to this process:

1. Build the war file: ``mvn package``
2. Check which version of the Dataverse Software is deployed: ``./asadmin list-applications``
3. Undeploy the Dataverse Software (if necessary): ``./asadmin undeploy dataverse-VERSION``
4. Copy the war file to the server (if necessary)
5. Deploy the new code: ``./asadmin deploy /path/to/dataverse-VERSION.war``

Running the Dataverse Software Installation Script in Non-Interactive Mode
--------------------------------------------------------------------------

Rather than running the installer in "interactive" mode, it's possible to put the values in a file. See "non-interactive mode" in the :doc:`/installation/installation-main` section of the Installation Guide.

Preventing Payara from Phoning Home
-----------------------------------

By default, Glassfish reports analytics information. The administration guide suggests this can be disabled with ``./asadmin create-jvm-options -Dcom.sun.enterprise.tools.admingui.NO_NETWORK=true``, should this be found to be undesirable for development purposes. It is unknown if Payara phones home or not.

Solr
----

.. TODO: This section should be moved into a dedicated guide about Solr for developers. It should be extended with
         information about the way Solr is used within the Dataverse Software, ideally explaining concepts and links to upstream docs.

Once some Dataverse collections, datasets, and files have been created and indexed, you can experiment with searches directly from Solr at http://localhost:8983/solr/#/collection1/query and look at the JSON output of searches, such as this wildcard search: http://localhost:8983/solr/collection1/select?q=*%3A*&wt=json&indent=true . You can also get JSON output of static fields Solr knows about: http://localhost:8983/solr/collection1/schema/fields

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

.. _auto-custom-build-number:

Automation of Custom Build Number on Webpage
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

You can create symbolic links from ``.git/hooks/post-checkout`` and ``.git/hooks/post-commit`` to ``scripts/installer/custom-build-number-hook``
to let Git automatically update ``src/main/java/BuildNumber.properties`` for you. This will result in showing branch name and
commit id in your test deployment webpages on the bottom right corner next to the version.

When you prefer manual updates, there is another script, see above: :ref:`custom_build_num_script`.

An alternative to that is using *MicroProfile Config* and set the option ``dataverse.build`` via a system property,
environment variable (``DATAVERSE_BUILD``) or `one of the other config sources
<https://docs.payara.fish/community/docs/Technical%20Documentation/MicroProfile/Config/Overview.html#config-sources>`__.

You could even override the version itself with the option ``dataverse.version`` in the same way, which is usually
picked up from a build time source.

See also discussion of version numbers in :ref:`run-build-create-war`.

Sample Data
-----------

You may want to populate your **non-production** Dataverse installations with sample data. You have a couple options:

- Code in https://github.com/IQSS/dataverse-sample-data (recommended). This set of sample data includes several common data types, data subsetted from production datasets in dataverse.harvard.edu, datasets with file hierarchy, and more.
- Scripts called from ``scripts/deploy/phoenix.dataverse.org/post``.

Switching from Glassfish to Payara
----------------------------------

If you already have a working dev environment with Glassfish and want to switch to Payara, you must do the following:

- Copy the "domain1" directory from Glassfish to Payara.

UI Pages Development
--------------------

While most of the information in this guide focuses on service and backing beans ("the back end") development in Java, working on JSF/Primefaces xhtml pages presents its own unique challenges. 

.. _avoid-efficiency-issues-with-render-logic-expressions:

Avoiding Inefficiencies in JSF Render Logic
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

It is important to keep in mind that the expressions in JSF ``rendered=`` attributes may be evaluated **multiple** times. So it is crucial not to use any expressions that require database lookups, or otherwise take any appreciable amount of time and resources. Render attributes should exclusively contain calls to methods in backing beans or caching service wrappers that perform any real work on the first call only, then keep returning the cached result on all the consecutive calls. This way it is irrelevant how many times PrimeFaces may need to call the method as any effect on the performance will be negligible.

If you are ever in doubt as to how many times the method in your render logic expression is called, you can simply add a logging statement to the method in question. Or you can simply err on the side of assuming that it's going to be called a lot, and ensure that any repeated calls are not expensive to process.

A simplest, trivial example would be a direct call to a method in SystemConfig service bean. For example, 

``<h:outputText rendered="#{systemConfig.advancedModeEnabled}" ...``

If this method (``public boolean isAdvancedModeEnabled()`` in ``SystemConfig.java``) consults a database setting every time it is called, this database query will be repeated every time JSF reevaluates the expression above. A lookup of a single database setting is not very expensive of course, but repeated enough times unnecessary queries do add up, especially on a busy server. So instead of SystemConfig, SettingsWrapper (a ViewScope bean) should be used to cache the result on the first call:

``<h:outputText rendered="#{settingsWrapper.advancedModeEnabled}" ...``

with the following code in ``SettingsWrapper.java``:

.. code:: java
	  
	  private Boolean  advancedModeEnabled = null; 
	  
	  public boolean isAdvancedModeEnabled() {
	     if (advancedModeEnabled == null) {
                advancedModeEnabled = systemConfig.isAdvancedModeEnabled();
             }
             return advancedModeEnabled; 
          }

A more serious example would be direct calls to PermissionServiceBean methods used in render logic expressions. This is something that has happened and caused some problems in real life. A simple permission service lookup (for example, whether a user is authorized to create a dataset in the current dataverse) can easily take 15 database queries. Repeated multiple times, this can quickly become a measurable delay in rendering the page. PermissionsWrapper must be used exclusively for any such lookups from JSF pages.

See also :doc:`performance`.
