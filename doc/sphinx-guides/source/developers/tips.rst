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

Running the Dataverse ``install`` Script in Non-Interactive Mode
----------------------------------------------------------------

Rather than running the installer in "interactive" mode, it's possible to put the values in a file. See "non-interactive mode" in the :doc:`/installation/installation-main` section of the Installation Guide.

Preventing Glassfish from Phoning Home
--------------------------------------

By default, Glassfish reports analytics information. The administration guide suggests this can be disabled with ``asadmin create-jvm-options -Dcom.sun.enterprise.tools.admingui.NO_NETWORK=true``, should this be found to be undesirable for development purposes.

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

Cloning the Project from Netbeans
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

From Netbeans, click "Team" then "Remote" then "Clone". Under "Repository URL", enter the `"ssh clone URL" <https://help.github.com/articles/which-remote-url-should-i-use/#cloning-with-ssh>`_ for your fork (if you do not have push access to the repo under IQSS) or ``git@github.com:IQSS/dataverse.git`` (if you do have push access to the repo under IQSS). See also https://netbeans.org/kb/docs/ide/git.html#github

----

Previous: :doc:`troubleshooting` | Next: :doc:`version-control`
