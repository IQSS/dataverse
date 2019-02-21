=====
Tools
=====

These are handy tools for your :doc:`/developers/dev-environment/`.

.. contents:: |toctitle|
	:local:

Netbeans Connector Chrome Extension
+++++++++++++++++++++++++++++++++++

The `Netbeans Connector <https://chrome.google.com/webstore/detail/netbeans-connector/hafdlehgocfcodbgjnpecfajgkeejnaa?hl=en>`_ extension for Chrome allows you to see changes you've made to HTML pages the moment you save the file without having to refresh your browser. See also 
http://wiki.netbeans.org/ChromeExtensionInstallation

pgAdmin
+++++++

You probably installed pgAdmin when following the steps in the :doc:`dev-environment` section but if not, you can download it from https://www.pgadmin.org

Maven
+++++

With Maven installed you can run ``mvn package`` and ``mvn test`` from the command line. It can be downloaded from https://maven.apache.org

Vagrant
+++++++

Vagrant allows you to spin up a virtual machine running Dataverse on your development workstation. You'll need to install Vagrant from https://www.vagrantup.com and VirtualBox from https://www.virtualbox.org.

We assume you have already cloned the repo from https://github.com/IQSS/dataverse as explained in the :doc:`/developers/dev-environment` section.

From the root of the git repo (where the ``Vagrantfile`` is), run ``vagrant up`` and eventually you should be able to reach an installation of Dataverse at http://localhost:8888 (the ``forwarded_port`` indicated in the ``Vagrantfile``).

Please note that running ``vagrant up`` for the first time should run the ``downloads/download.sh`` script for you to download required software such as Glassfish and Solr and any patches. However, these dependencies change over time so it's a place to look if ``vagrant up`` was working but later fails.

On Windows if you see an error like ``/usr/bin/perl^M: bad interpreter`` you might need to run ``dos2unix`` on the installation scripts. 

PlantUML
++++++++

PlantUML is used to create diagrams in the guides and other places. Download it from http://plantuml.com and check out an example script at https://github.com/IQSS/dataverse/blob/v4.6.1/doc/Architecture/components.sh . Note that for this script to work, you'll need the ``dot`` program, which can be installed on Mac with ``brew install graphviz``.

Eclipse Memory Analyzer Tool (MAT)
++++++++++++++++++++++++++++++++++

The Memory Analyzer Tool (MAT) from Eclipse can help you analyze heap dumps, showing you "leak suspects" such as seen at https://github.com/payara/Payara/issues/350#issuecomment-115262625

It can be downloaded from http://www.eclipse.org/mat

If the heap dump provided to you was created with ``gcore`` (such as with ``gcore -o /tmp/gf.core $glassfish_pid``) rather than ``jmap``, you will need to convert the file before you can open it in MAT. Using ``gf.core.13849`` as example of the original 33 GB file, here is how you could convert it into a 26 GB ``gf.core.13849.hprof`` file. Please note that this operation took almost 90 minutes:

``/usr/java7/bin/jmap -dump:format=b,file=gf.core.13849.hprof /usr/java7/bin/java gf.core.13849``

A file of this size may not "just work" in MAT. When you attempt to open it you may see something like "An internal error occurred during: "Parsing heap dump from '/tmp/heapdumps/gf.core.13849.hprof'". Java heap space". If so, you will need to increase the memory allocated to MAT. On Mac OS X, this can be done by editing ``MemoryAnalyzer.app/Contents/MacOS/MemoryAnalyzer.ini`` and increasing the value "-Xmx1024m" until it's high enough to open the file. See also http://wiki.eclipse.org/index.php/MemoryAnalyzer/FAQ#Out_of_Memory_Error_while_Running_the_Memory_Analyzer

PageKite
++++++++

PageKite is a fantastic service that can be used to share your
local development environment over the Internet on a public IP address.

With PageKite running on your laptop, the world can access a URL such as
http://pdurbin.pagekite.me to see what you see at http://localhost:8080

Sign up at https://pagekite.net and follow the installation instructions or simply download https://pagekite.net/pk/pagekite.py

The first time you run ``./pagekite.py`` a file at ``~/.pagekite.rc`` will be
created. You can edit this file to configure PageKite to serve up port 8080
(the default GlassFish HTTP port) or the port of your choosing.

According to https://pagekite.net/support/free-for-foss/ PageKite (very generously!) offers free accounts to developers writing software the meets http://opensource.org/docs/definition.php such as Dataverse.

MSV
+++

`MSV (Multi Schema Validator) <http://msv.java.net>`_ can be used from the command line to validate an XML document against a schema. Download the latest version from https://java.net/downloads/msv/releases/ (msv.20090415.zip as of this writing), extract it, and run it like this:

.. code-block:: bash

    $ java -jar /tmp/msv-20090415/msv.jar Version2-0.xsd ddi.xml 
    start parsing a grammar.
    validating ddi.xml
    the document is valid.

FontCustom
++++++++++

The custom file type icons were created with the help of `FontCustom <https://github.com/FontCustom/fontcustom>`. Their README provides installation instructions as well as directions for producing your own vector-based icon font.

Here is a vector-based SVG file to start with as a template: :download:`icon-template.svg <../_static/icon-template.svg>`

SonarQube
+++++++++

SonarQube is a static analysis tool that can be used to identify possible problems in the codebase, or with new code. It may report false positives or false negatives, but can help identify potential problems before they are reported in prodution or to identify potential causes of problems reported in production.

Download SonarQube from https://www.sonarqube.org and start look in the `bin` directory for a `sonar.sh` script for your architecture. Once the tool is running on http://localhost:9000 you can use it as the URL in this example script to run sonar:

.. code-block:: bash

    #!/bin/sh

    mvn sonar:sonar \
    -Dsonar.host.url=${your_sonar_url} \
    -Dsonar.login=${your_sonar_token_for_project} \
    -Dsonar.test.exclusions='src/test/**,src/main/webapp/resources/**' \
    -Dsonar.issuesReport.html.enable=true \
    -Dsonar.issuesReport.html.location='sonar-issues-report.html' \
    -Dsonar.jacoco.reportPath=target/jacoco.exec

Once the analysis is complete, you should be able to access http://localhost:9000/dashboard?id=edu.harvard.iq%3Adataverse to see the report. To learn about resource leaks, for example, click on "Bugs", the "Tag", then "leak" or "Rule", then "Resources should be closed".

Infer
+++++

Infer is another static analysis tool that can be downloaded from https://github.com/facebook/infer

Example command to run infer:

.. code-block:: bash

    $  infer -- mvn package

Look for "RESOURCE_LEAK", for example.

lsof
++++

If file descriptors are not closed, eventually the open but unused resources can cause problems with system (glassfish in particular) stability.
Static analysis and heap dumps are not always sufficient to identify the sources of these problems.
For a quick sanity check, it can be helpful to check that the number of file descriptors does not increase after a request has finished processing.

For example...

.. code-block:: bash

    $  lsof | grep M6EI0N | wc -l
    0
    $  curl -X GET "http://localhost:8083/dataset.xhtml?persistentId=doi:10.5072/FK2/M6EI0N" > /dev/null
    $  lsof | grep M6EI0N | wc -l
    500

would be consistent with a file descriptor leak on the dataset page.

----

Previous: :doc:`making-releases` | Next: :doc:`unf/index`
