=====
Tools
=====

These are handy tools for your :doc:`/developers/dev-environment/`.

.. contents:: :local:

Netbeans Connector Chrome Extension
+++++++++++++++++++++++++++++++++++

The `Netbeans Connector <https://chrome.google.com/webstore/detail/netbeans-connector/hafdlehgocfcodbgjnpecfajgkeejnaa?hl=en>`_ extension for Chrome allows you to see changes you've made to HTML pages the moment you save the file without having to refresh your browser. See also 
http://wiki.netbeans.org/ChromeExtensionInstallation

Maven
+++++

With Maven installed you can run `mvn package` and `mvn test` from the command line. It can be downloaded from https://maven.apache.org

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

Vagrant
+++++++

Vagrant allows you to spin up a virtual machine running Dataverse on your development workstation. You'll need to install Vagrant from https://www.vagrantup.com and VirtualBox from https://www.virtualbox.org.

We assume you have already cloned the repo from https://github.com/IQSS/dataverse as explained in the :doc:`/developers/dev-environment` section.

From the root of the git repo, run ``vagrant up`` and eventually you
should be able to reach an installation of Dataverse at
http://localhost:8888 (or whatever forwarded_port indicates in the
Vagrantfile).

Please note that running ``vagrant up`` for the first time should run the ``downloads/download.sh`` script for you to download required software such as Glassfish and Solr and any patches. However, these dependencies change over time so it's a place to look if ``vagrant up`` was working but later fails.

MSV
+++

`MSV (Multi Schema Validator) <http://msv.java.net>`_ can be used from the command line to validate an XML document against a schema. Download the latest version from https://java.net/downloads/msv/releases/ (msv.20090415.zip as of this writing), extract it, and run it like this:

.. code-block:: bash

    $ java -jar /tmp/msv-20090415/msv.jar Version2-0.xsd ddi.xml 
    start parsing a grammar.
    validating ddi.xml
    the document is valid.
