================================
R, rApache and TwoRavens
================================

Eventually, this document may be split into several parts, dedicated to individual components - 
such as R, rApache and the TwoRavens applications. Particularly, if the TwoRavens team creates an "official" distribution with their own installation manual. 

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

We provide a shell script (``conf/R/r-setup.sh`` in the Dataverse source tree; you will need the other 3 files in that directory as well - `https://github.com/IQSS/dataverse/conf/R/r-setup.sh <https://github.com/IQSS/dataverseconf/R/r-setup.sh>`__) that will attempt to install the required 3rd party packages; it will also configure Rserve and rserve user. rApache configuration will be addressed in its own section.

The script will attempt to download the packages from CRAN (or a mirror) and GitHub, so the system must have access to the internet. On a server fully firewalled from the world, packages can be installed from downloaded sources. This is left as an exercise for the reader. Consult the script for insight. 


2. Install the TwoRavens Application
++++++++++++++++++++++++++++++++++++

a. download the app 
-------------------

from
https://github.com/IQSS/TwoRavens/archive/master.zip

b. unzip 
--------

and **rename the resulting directory** ``dataexplore``.
Place it in the web root directory of your apache server; so that
it is visible from the outside at 

``https://<rapache server>:<rapache port>/dataexplore``

We'll assume ``/var/www/html/dataexplore`` in the examples below. 

c. run the installer
--------------------

a scripted, interactive installer is provided at the top level of the TwoRavens 
distribution (`https://github.com/IQSS/TwoRavens/blob/master/install.pl <https://github.com/IQSS/TwoRavens/blob/master/install.pl>`__). Run it as 

   ``./install.pl``

The installer will ask you to provide the following:

===================  =============================  ===========  
Setting              default                        Comment
===================  =============================  ===========  
TwoRavens directory  ``/var/www/html/dataexplore``  File directory where TwoRavens is installed.
Apache config dir.   ``/etc/httpd``                 rApache config file for TwoRavens will be placed under ``conf.d/`` there.
Apache web dir.      ``/var/www/html``
Apache host address  local hostname             
Apache host port     ``443``
Apache web protocol  ``https``                      http or https (https recommended)
Dataverse URL                                       URL of the Dataverse from which TwoRavens will be receiving metadata and data files. For example, ``https://thedata.harvard.edu``.
===================  =============================  =========== 


This should be it!


Appendix
++++++++

Explained below are the steps needed to manually configure TwoRavens to run under rApache (these are performed by the ``install.pl`` script above).  Provided for reference. 

I. Configure the TwoRavens web (Javascript) application.
-------------------------------------------------------

Edit the file ``/var/www/html/dataexplore/app_ddi.js``.

find and edit the following 3 lines:

1. ``var production=false;``

   and change it to ``true``;

2. ``hostname="localhost:8080";``

   so that it points to the dataverse app, from which TwoRavens will be obtaining the metadata and data files. (don't forget to change 8080 to the correct port number!)

   and

3. ``var rappURL = "http://0.0.0.0:8000/custom/";``

   set this to the URL of your rApache server, i.e.

   ``"https://<rapacheserver>:<rapacheport>/custom/";``

II. Configure the R applications to run under rApache
-----------------------------------------------------

rApache is a loadable httpd module that provides a link between Apache and R. 
When you installed the rApache rpm, under 0., it placed the module in the Apache library directory and added a configuration entry to the config file (``/etc/httpd/conf/httpd.conf``). 

Now we need to configure rApache to serve several R "mini-apps", from the R sources provided with TwoRavens. 

a. Edit the following files:
****************************
in ``dataexplore/rook``:

``rookdata.R, rookzelig.R, rooksubset.R, rooktransform.R, rookselector.R, rooksource.R``

and replace *every* instance of ``production<-FALSE`` line with ``production<-TRUE``.
 
(yeah, that's why we provide that installer script...)

b. Edit dataexplore/rook/rooksource.R
*****************************************


and change the following line: 

``setwd("/usr/local/glassfish4/glassfish/domains/domain1/docroot/dataexplore/rook")``

to 

``setwd("/var/www/html/dataexplore/rook")``

(or your dataexplore directory, if different from the above)

c. Edit the following lines in dataexplore/rook/rookutils.R: 
************************************************************

``url <- paste("https://dataverse-internal.iq.harvard.edu/custom/preprocess_dir/preprocessSubset_",sessionid,".txt",sep="")``

and 

``imageVector[[qicount]]<<-paste("https://dataverse-internal.iq.harvard.edu/custom/pic_dir/", mysessionid,"_",mymodelcount,qicount,".png", sep = "")``

and change the URL to reflect the correct location of your rApache instance - make sure that the protocol and the port number are correct too, not just the host name!

d. Add the following lines to /etc/httpd/conf/httpd.conf: 
*********************************************************
(This configuration is now supplied in its own config file ``tworavens-rapache.conf``, it can be dropped into the Apache's ``/etc/httpd/conf.d``. Again, the scripted installer will do this for you automatically.)

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
***************************************************************


.. code-block:: none

   mkdir --parents /var/www/html/custom/pic_dir
   
   mkdir --parents /var/www/html/custom/preprocess_dir

   chown -R apache.apache /var/www/html/custom

f. chown the dataexplore directory 
**********************************
to user apache: 

``chown -R apache /var/www/html/dataexplore``

g. restart httpd
****************


``service httpd restart``


