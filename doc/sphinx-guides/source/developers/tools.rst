=====
Tools
=====

These are handy tools for your :doc:`dev-environment`.

.. contents:: |toctitle|
	:local:

Tools for Faster Deployment
+++++++++++++++++++++++++++

See :ref:`ide-trigger-code-deploy` in the Container Guide.

Netbeans Connector Chrome Extension
+++++++++++++++++++++++++++++++++++

The `Netbeans Connector <https://chrome.google.com/webstore/detail/netbeans-connector/hafdlehgocfcodbgjnpecfajgkeejnaa?hl=en>`_ extension for Chrome allows you to see changes you've made to HTML pages the moment you save the file without having to refresh your browser. See also 
http://wiki.netbeans.org/ChromeExtensionInstallation

Unfortunately, while the Netbeans Connector Chrome Extension used to "just work", these days a workaround described at https://www.youtube.com/watch?v=J6lOQS2rWK0&t=130 seems to be necessary. For now, under "Run" (under project properties), choose "Chrome" as the browser rather than "Chrome with NetBeans Connector". After you run the project, click the Netbeans logo in Chrome and then "Debug in NetBeans". For more information, please see the "workaround for Netbeans Connector Chrome Extension" post at https://groups.google.com/d/msg/dataverse-dev/agJZilD1l0Q/cMBkt5KDBQAJ

pgAdmin
+++++++

You may have installed pgAdmin when following the steps in the :doc:`classic-dev-env` section but if not, you can download it from https://www.pgadmin.org

Maven
+++++

With Maven installed you can run ``mvn package`` and ``mvn test`` from the command line. It can be downloaded from https://maven.apache.org

PlantUML
++++++++

PlantUML is used to create diagrams in the guides and other places. Download it from https://plantuml.com and check out an example script at https://github.com/IQSS/dataverse/blob/v4.6.1/doc/Architecture/components.sh . Note that for this script to work, you'll need the ``dot`` program, which can be installed on Mac with ``brew install graphviz``.

Eclipse Memory Analyzer Tool (MAT)
++++++++++++++++++++++++++++++++++

The Memory Analyzer Tool (MAT) from Eclipse can help you analyze heap dumps, showing you "leak suspects" such as seen at https://github.com/payara/Payara/issues/350#issuecomment-115262625

It can be downloaded from https://www.eclipse.org/mat

If the heap dump provided to you was created with ``gcore`` (such as with ``gcore -o /tmp/app.core $app_pid``) rather than ``jmap``, you will need to convert the file before you can open it in MAT. Using ``app.core.13849`` as example of the original 33 GB file, here is how you could convert it into a 26 GB ``app.core.13849.hprof`` file. Please note that this operation took almost 90 minutes:

``/usr/java7/bin/jmap -dump:format=b,file=app.core.13849.hprof /usr/java7/bin/java app.core.13849``

A file of this size may not "just work" in MAT. When you attempt to open it you may see something like "An internal error occurred during: "Parsing heap dump from '/tmp/heapdumps/app.core.13849.hprof'". Java heap space". If so, you will need to increase the memory allocated to MAT. On Mac OS X, this can be done by editing ``MemoryAnalyzer.app/Contents/MacOS/MemoryAnalyzer.ini`` and increasing the value "-Xmx1024m" until it's high enough to open the file. See also https://wiki.eclipse.org/index.php/MemoryAnalyzer/FAQ#Out_of_Memory_Error_while_Running_the_Memory_Analyzer

PageKite
++++++++

PageKite is a fantastic service that can be used to share your
local development environment over the Internet on a public IP address.

With PageKite running on your laptop, the world can access a URL such as
http://pdurbin.pagekite.me to see what you see at http://localhost:8080

Sign up at https://pagekite.net and follow the installation instructions or simply download https://pagekite.net/pk/pagekite.py

The first time you run ``./pagekite.py`` a file at ``~/.pagekite.rc`` will be
created. You can edit this file to configure PageKite to serve up port 8080
(the default app server HTTP port) or the port of your choosing.

According to https://pagekite.net/support/free-for-foss/ PageKite (very generously!) offers free accounts to developers writing software the meets https://opensource.org/docs/definition.php such as the Dataverse Project.

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
    -Dsonar.jacoco.reportPath=target/coverage-reports/jacoco-unit.exec

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

If file descriptors are not closed, eventually the open but unused resources can cause problems with system (app servers in particular) stability.
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

jmap and jstat
++++++++++++++

``jmap`` and ``jstat`` are parts of the standard JDK distribution. 
jmap allows you to look at the contents of the java heap. It can be used to create a heap dump, that you can then feed to another tool, such as ``Memory Analyzer Tool`` (see above). It can also be used as a useful tool of its own, for example, to list all the classes currently instantiated in memory:

.. code-block:: bash

   $ jmap -histo <app process id> 

will output a list of all classes, sorted by the number of instances of each individual class, with the size in bytes. 
This can be very useful when looking for memory leaks in the application. Another useful tool is ``jstat``, that can be used in combination with ``jmap`` to monitor the effectiveness of garbage collection in reclaiming allocated memory. 

In the example script below we stress running Dataverse Software applicatione with GET requests to a specific page in a Dataverse installation, use ``jmap`` to see how many Dataverse collection, Dataset and DataFile class object get allocated, then run ``jstat`` to see how the numbers are affected by both "Young Generation" and "Full" garbage collection runs (``YGC`` and ``FGC`` respectively):

(This is script is provided **as an example only**! You will have to experiment and expand it to suit any specific needs and any specific problem you may be trying to diagnose, and this is just to give an idea of how to go about it)

.. code-block:: bash

   #!/bin/sh

   # the script takes the numeric id of the app server process as the command line argument:
   id=$1 

   while :
   do  
       # Access the Dataverse collection xxx 10 times in a row: 
       for ((i = 0; i < 10; i++))
       do 
       	  # hide the output, standard and stderr:
       	  curl http://localhost:8080/dataverse/xxx 2>/dev/null > /dev/null
       done

       sleep 1

       # run jmap and save the output in a temp file: 

       jmap -histo $id > /tmp/jmap.histo.out

       # grep the output for Dataverse Collection, Dataset and DataFile classes: 
       grep '\.Dataverse$' /tmp/jmap.histo.out
       grep '\.Dataset$' /tmp/jmap.histo.out
       grep '\.DataFile$' /tmp/jmap.histo.out
       # (or grep for whatever else you may be interested in)

       # print the last line of the jmap output (the totals):
       tail -1 /tmp/jmap.histo.out

       # run jstat to check on GC:
       jstat -gcutil ${id} 1000 1 2>/dev/null

       # add a time stamp and a new line: 

       date
       echo 

    done

The script above will run until you stop it, and will output something like: 

.. code-block:: none
   
	439:           141          28200  edu.harvard.iq.dataverse.Dataverse
    	472:           160          24320  edu.harvard.iq.dataverse.Dataset
    	674:            60           9600  edu.harvard.iq.dataverse.DataFile
    	S0     S1     E      O      P     YGC     YGCT    FGC    FGCT     GCT   
    	0.00 100.00  35.32  20.15      ?      7    2.145     0    0.000    2.145
	Total     108808814     5909776392
	Wed Aug 14 23:13:42 EDT 2019

	385:           181          36200  edu.harvard.iq.dataverse.Dataverse
	338:           320          48640  edu.harvard.iq.dataverse.Dataset
	524:           120          19200  edu.harvard.iq.dataverse.DataFile
	S0     S1     E      O      P     YGC     YGCT    FGC    FGCT     GCT   
	0.00 100.00  31.69  45.11      ?      9    3.693     0    0.000    3.693
	Total     167998691     9080163904
	Wed Aug 14 23:14:59 EDT 2019

	367:           201          40200  edu.harvard.iq.dataverse.Dataverse
	272:           480          72960  edu.harvard.iq.dataverse.Dataset
	442:           180          28800  edu.harvard.iq.dataverse.DataFile
	S0     S1     E      O      P     YGC     YGCT    FGC    FGCT     GCT   
	0.00 100.00  28.05  69.94      ?     11    5.001     0    0.000    5.001
	Total     226826706    12230221352
	Wed Aug 14 23:16:16 EDT 2019

	... etc.

How to analyze the output, what to look for: 

First, look at the numbers in the jmap output. In the example above, you can immediately see, after the first three iterations, that every 10 Dataverse installation page loads results in the increase of the number of Dataset classes by 160. I.e., each page load leaves 16 of these on the heap. We can also see that each of the 10 page load cycles increased the heap by roughly 3GB; that each cycle resulted in a couple of YG (young generation) garbage collections, and in the old generation allocation being almost 70% full. These numbers in the example are clearly quite high and are an indication of some problematic memory allocation by the Dataverse installation page - if this is the result of something you have added to the page, you probably would want to investigate and fix it. However, overly generous memory use **is not the same as a leak** necessarily. What you want to see now is how much of this allocation can be reclaimed by "Full GC". If all of it gets freed by ``FGC``, it is not the end of the world (even though you do not want your system to spend too much time running ``FGC``; it costs CPU cycles, and actually freezes the application while it's in progress!). It is however a **really** serious problem, if you determine that a growing portion of the old. gen. memory (``"O"`` in the ``jmap`` output) is not getting freed, even by ``FGC``. This *is* a real leak now, i.e. something is leaving behind some objects that are still referenced and thus off limits to garbage collector. So look for the lines where the ``FGC`` counter is incremented. For example, the first ``FGC`` in the example output above: 

.. code-block:: none

   	271:           487          97400  edu.harvard.iq.dataverse.Dataverse
	216:          3920          150784  edu.harvard.iq.dataverse.Dataset	
	337:           372          59520  edu.harvard.iq.dataverse.DataFile
	Total     277937182    15052367360
	S0     S1     E      O      P     YGC     YGCT    FGC    FGCT     GCT   
	0.00 100.00  77.66  88.15      ?     17    8.734     0    0.000    8.734
	Wed Aug 14 23:20:05 EDT 2019

	265:           551         110200  edu.harvard.iq.dataverse.Dataverse
	202:          4080         182400  edu.harvard.iq.dataverse.Dataset
	310:           450          72000  edu.harvard.iq.dataverse.DataFile
	Total     142023031     8274454456
	S0     S1     E      O      P     YGC     YGCT    FGC    FGCT     GCT   
	0.00 100.00  71.95  20.12      ?     22   25.034     1    4.455   29.489
	Wed Aug 14 23:21:40 EDT 2019

We can see that the first ``FGC`` resulted in reducing the ``"O"`` by almost 7GB, from 15GB down to 8GB (from 88% to 20% full). The number of Dataset classes has not budged at all - it has grown by the same 160 objects as before (very suspicious!). To complicate matters, ``FGC`` does not **guarantee** to free everything that can be freed - it will balance how much the system needs memory vs. how much it is willing to spend in terms of CPU cycles performing GC (remember, the application freezes while ``FGC`` is running!). So you should not assume that the "20% full" number above means that you have 20% of your stack already wasted and unrecoverable. Instead, look for the next **minium** value of ``"O"``; then for the next, etc. Now compare these consecutive miniums. With the above test (this is an output of a real experiment, a particularly memory-hungry feature added to the Dataverse installation page), the minimums sequence (of old. gen. usage, in %) was looking as follows: 


.. code-block:: none
   
   2.19
   2.53
   3.00
   3.13
   3.95
   4.03
   4.21
   4.40
   4.64
   5.06
   5.17
   etc. ...

It is clearly growing - so now we can conclude that indeed something there is using memory in a way that's not recoverable, and this is a clear problem. 
