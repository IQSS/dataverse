================================
R, rApache and TwoRavens
================================

This document is intended to be part of the Dataverse 4.0 Installers Guide. 
It may end up being split into several parts, dealing with individual components - 
such as R, rApache and the TwoRavens applications. 

0. PREREQUISITS
+++++++++++++++

a. httpd (Apache): 
------------------

``yum install httpd``

Disable SELinux on httpd: 

``setenforce permissive``

``getenforce``

https strongly recommended (required?); signed cert recommended. 

b. R:
-----

yum install R R-devel

(EPEL distribution recommended; version 3.* required; 3.1.* recommended as of writing this)

c. rApache: 
-----------

rpm distribution from the HMDC systems group is recommended; 
download and install the latest available rpm (1.2.6, as of writing this): 

http://mirror.hmdc.harvard.edu/HMDC-Public/RedHat-6/rapache-1.2.6-rpm0.x86_64.rpm

d. Install libcurl-devel:
-------------------------

(provides /usr/bin/curl-config, needed by some 3rd-party R packages; package installation *will fail silently* if it's not found!): 

``yum install libcurl-devel``

Make sure you have the standard GNU compilers installed (needed for 3rd-party R packages to build themselves). 



1. Set Up R
+++++++++++

R is used both by the Dataverse application, directly, and the TwoRavens companion app. 

Two distinct interfaces are used to access R: Dataverse uses Rserve; and TwoRavens sends jobs to R running under rApache using Rook interface. 

We provide a shell script (found in ...) that will attempt to install the required 3rd party packages; it will also configure Rserve and rserve user. rApache configuration will be addressed in its own section.

The script will attempt to download the packages from CRAN (or a mirror), so the system must have access to the internet. On a server fully firewalled from the world, packages can be installed from downloaded sources. This is left as an exercise for the reader. Consult the script for insight. 


2. Install the TwoRavens Application
++++++++++++++++++++++++++++++++++++

a. download the app 
-------------------

from
https://github.com/IQSS/TwoRavens/archive/master.zip

b. unzip 
--------

and *rename the resulting directory* "dataexplore".
Place it in the web root directory of your apache server; so that
it is visible from the outside at 

``https://<rapache server>:<rapache port>/dataexplore``

We'll assume ``/var/www/html/dataexplore`` in the examples below. 

c. chown the directory 
----------------------
to user apache

d. Edit the file /var/www/html/dataexplore/app_ddi.js 
-----------------------------------------------------
find and edit the following 3 lines:

set the production toggle to true at the top of the script;

``hostname="localhost:8080";``

change this to the hostname of the dataverse app.

and

``var rappURL = "http://0.0.0.0:8000/custom/";``

to 

``"https://<rapacheserver>:<rapacheport>/custom/";``


3. Configure rApache:
+++++++++++++++++++++

rApache is a loadable httpd module that provides a link between Apache and R. 
When you installed the rApache rpm, under 0., it placed the module in the Apache library directory and added a configuration entry to the config file (``/etc/httpd/conf/httpd.conf``). 

Now we need to configure rApache to serve several R "mini-apps", from the R sources provided with the TwoRavens app. 

a. Edit the following files 
---------------------------
in dataexplore/rook:

``rookdata.R, rookzelig.R, rooksubset.R, rooktransform.R, rookselector.R, rooksource.R ``

and replace *every* instance of ``production<-FALSE`` line with ``production<-TRUE`` 
(TODO: there gotta be a simpler way of doing this...)

b. Edit the dataexplore/rook/rooksource.R
-----------------------------------------

and change the following line: 

``setwd("/usr/local/glassfish4/glassfish/domains/domain1/docroot/dataexplore/rook")``

to 

``setwd("/var/www/html/dataexplore/rook")``

(or your dataexplore directory, if different from the above)

c. Edit the following line in dataexplore/rook/rookutils.R: 
-----------------------------------------------------------

``url <- paste("https://dataverse-internal.iq.harvard.edu/custom/preprocess_dir/preprocessSubset_",sessionid,".txt",sep="")``

and change the URL to reflect the correct location of your rApache instance - make sure that the protocol and the port number are correct too, not just the host name!

d. Add the following lines to /etc/httpd/conf/httpd.conf: 
---------------------------------------------------------
(TODO: isolate this config in its own *.conf file?)

.. code-block:: none

   RSourceOnStartup "/var/www/html/dataexplore/rook/rooksource.R"
   <Location /custom/zeligapp>
      SetHandler r-handler
      RFileEval /var/www/html/dataexplore/rook/rookzelig.R:Rook::Server$call(zelig.app)
   </Location>
   <Location /custom/subsetapp>
      SetHandler r-handler
      RFileEval /var/www/html/dataexplore/rook/rooksubset.R:Rook::Server$call(subset.app)
   </Location>
   <Location /custom/transformapp>
      SetHandler r-handler
      RFileEval /var/www/html/dataexplore/rook/rooktransform.R:Rook::Server$call(transform.app)
   </Location>
   <Location /custom/dataapp>
      SetHandler r-handler
      RFileEval /var/www/html/dataexplore/rook/rookdata.R:Rook::Server$call(data.app)
   </Location>

e. Create the following directories and chown them user apache: 
---------------------------------------------------------------

.. code-block:: none

   mkdir --parents /var/www/html/custom/pic_dir
   chown -R apache.apache /var/www/html/custom
   
   mkdir --parents /var/www/html/custom/preprocess_dir
   chown -R apache.apache /var/www/html/custom

f. restart httpd
----------------

``service httpd restart``

