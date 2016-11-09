================================
R, rApache and TwoRavens
================================

Eventually, this document may be split into several parts, dedicated to individual components - 
such as R, rApache and the TwoRavens applications. Particularly, if the TwoRavens team creates an "official" distribution with their own installation manual. 

0. PREREQUISITES
++++++++++++++++

a. httpd (Apache): 
------------------

``yum install httpd``

Disable SELinux on httpd: 

``setenforce permissive``

``getenforce``

(Note: a pull request to get rApache working with SELinux is welcome! Please see the :doc:`/developers/selinux` section of the Developer Guide to get started.)


https strongly recommended; signed certificate (as opposed to self-signed) is recommended. 

Directory listing needs to be disabled on the web documents folder served by Apache: 

In the main Apache configuration file (``/etc/httpd/conf/httpd.conf`` in the default setup), find the section that configures your web directory. For example, if the ``DocumentRoot``, defined elsewhere in the file, is set to the default ``"/var/www/html"``, the opening line of the section will look like this:

``<Directory "/var/www/html">`` 

Find the ``Options`` line in that section, and make sure that it doesn't contain the ``Indexes`` statement. 
For example, if the options line in your configuration is 

``Options Indexes FollowSymLinks``

change it to 

``Options FollowSymLinks``

b. R:
-----

yum install R R-devel

(EPEL distribution recommended; version 3.* required; 3.1.* recommended as of writing this)

To pick up any needed dependencies, CentOS users may simply install the epel-release RPM.

RHEL users will want to log in to their organization's respective RHN interface, find the particular machine in question and:

• click on "Subscribed Channels: Alter Channel Subscriptions"
• enable EPEL, Server Extras, Server Optional

c. rApache: 
-----------

rpm distribution from the HMDC systems group is recommended (latest available version is 1.2.6, as of writing this). The rpm requires Apache libapreq2, that should be available via yum. 

install rApache as follows:: 

	yum install libapreq2
	rpm -ivh http://mirror.hmdc.harvard.edu/HMDC-Public/RedHat-6/rapache-1.2.6-rpm0.x86_64.rpm

If you are using RHEL/CentOS 7, you can `download an experimental rapache-1.2.7-rpm0.x86_64.rpm <../_static/installation/files/home/rpmbuild/rpmbuild/RPMS/x86_64/rapache-1.2.7-rpm0.x86_64.rpm>`_ and install it with::

	rpm -ivh rapache-1.2.7-rpm0.x86_64.rpm

d. Install system depencies:
----------------------------

The r-setup.sh script launches child processes which log to RINSTALL.* files. Once the script exits, search these files for the word "error" and be sure to install any missing dependencies and run the script again. At present, at minimum it needs:

``yum install libcurl-devel openssl-devel libxml2-devel ed libX11-devel libpng-devel mesa-libGL-devel mesa-libGLU-devel libpqxx-devel``

Make sure you have the standard GNU compilers installed (needed for 3rd-party R packages to build themselves). CentOS 6 users will need gcc-fortran 4.6 or greater, available from the CentOS devtools repo. 

Again, without these rpms, R package devtools was failing to install, silently or with a non-informative error message. 
Note: this package ``devtools`` has proven to be very flaky; it is being very actively maintained, new dependencies are being constantly added and new bugs introduced... however, it is only needed to install the package ``Zelig``, the main R workhorse behind TwoRavens. It cannot be installed from CRAN, like all the other 3rd party packages we use - becase TwoRavens requires version 5, which is still in beta. So devtools is needed to build it from sources downloaded directly from github. Once Zelig 5 is released, we'll be able to drop the requirement for devtools - and that will make this process much simpler. For now, be prepared for it to be somewhat of an adventure. 


1. Set Up R
+++++++++++

R is used both by the Dataverse application, directly, and the TwoRavens companion app.

Two distinct interfaces are used to access R: Dataverse uses Rserve; and TwoRavens sends jobs to R running under rApache using Rook interface. 

We provide a shell script (``conf/R/r-setup.sh`` in the Dataverse source tree; you will need the other 3 files in that directory as well - `https://github.com/IQSS/dataverse/tree/master/conf/R <https://github.com/IQSS/dataverse/tree/master/conf/R>`__) that will attempt to install the required 3rd party packages; it will also configure Rserve and rserve user. rApache configuration will be addressed in its own section.

The script will attempt to download the packages from CRAN (or a mirror) and GitHub, so the system must have access to the internet. On a server fully firewalled from the world, packages can be installed from downloaded sources. This is left as an exercise for the reader. Consult the script for insight.

See the Appendix for the information on the specific packages, and versions that the script will attempt to install. 

2. Install the TwoRavens Application
++++++++++++++++++++++++++++++++++++

a. download the app 
-------------------

from
https://github.com/IQSS/TwoRavens/archive/master.zip

b. unzip 
--------

and **rename the resulting directory** ``dataexplore``.
Place it in the web root directory of your apache server. We'll assume ``/var/www/html/dataexplore`` in the examples below. 

c. run the installer
--------------------

a scripted, interactive installer is provided at the top level of the TwoRavens 
distribution. Run it as::

   cd /var/www/html/dataexplore
   chmod +x install.pl
   ./install.pl

The installer will ask you to provide the following:

===================  ================================    ===========  
Setting              default                             Comment
===================  ================================    ===========  
TwoRavens directory  ``/var/www/html/dataexplore``       File directory where TwoRavens is installed.
Apache config dir.   ``/etc/httpd``                      rApache config file for TwoRavens will be placed under ``conf.d/`` there.
Apache web dir.      ``/var/www/html``
Apache host address  local hostname                      rApache host
Apache host port     ``80``                              rApache port (**see the next section** for the discussion on ports!)
Apache web protocol  ``http``                            http or https for rApache (https recommended)
Dataverse URL        ``http://{local hostname}:8080``    URL of the Dataverse from which TwoRavens will be receiving metadata and data files.
===================  ================================    =========== 


Once everything is installed and configured, the installer script will print out a confirmation message with the URL of the TwoRavens application. For example: 

The application URL is 
https://server.dataverse.edu/dataexplore/gui.html

This URL **must** be configured in the settings of your Dataverse application!
This can be done by issuing the following settings API call: 

``curl -X PUT -d {TWORAVENS_URL} http://localhost:8080/api/admin/settings/:TwoRavensUrl``

where "{TWORAVENS_URL}" is the URL reported by the installer script (as in the example above).

d. Ports configuration
-----------------------

By default, Glassfish will install itself on ports 8080 and 8181 (for http and https, respectively), and Apache - on port 80 (the default port for http). Under this configuration, your Dataverse will be accessible at http://{your host}:8080 and https://{your host}:8181; and rApache - at http://{your host}/. The TwoRavens installer, above, will default to these values (and assume you are running both the Dataverse and TwoRavens/rApache on the same host). 

This configuration may be the easiest to set up if you are simply trying out/testing the Dataverse and TwoRavens. Accept all the defaults, and you should have a working installation in no time. However, if you are planning to use this installation to actually serve data to real users, you'll probably want to run Glassfish on ports 80 and 443. This way, there will be no non-standard ports in the Dataverse url visible to the users. Then you'll need to configure the Apache to run on some other port - for example, 8080, instead of 80. This port will only appear in the URL for the TwoRavens app. If you want to use this configuration - or any other that is not the default one described above! - it is your job to reconfigure Glassfish and Apache to run on the desired ports **before** you run the TwoRavens installer. 

Furthermore, while the default setup assumes http as the default protocol for both the Dataverse and TwoRavens, https is strongly recommended for a real production system. Again, this will be your responsibility, to configure https in both Glassfish and Apache. Glassfih comes pre-configured to run https on port 8181, with a *self-signed certificiate*. For a production system, you will most certainly will want to obtain a properly signed certificate and configure Glassfish to use it. Apache does not use https out of the box at all. Again, it is the responsibility of the installing user, to configure Apache to run https, and, providing you are planning to run rApache on the same host as the Dataverse, use the same SSL certificate as your Glassfish instance. Again, it will need to be done before you run the installer script above. All of this may involve some non-trivial steps and will most likely require help from your local network administrator - unless you happen to be your local sysadmin. Unfortunately, we cannot provide step-by-step instructions for these tasks. As the actual steps required will likely depend on the specifics of how your institution obtains signed SSL certificates, the format in which you receive these certificates, etc. **Good luck!**

Finally: If you choose to have your Dataverse support secure
**Shibboleth authentication**, it will require a server and port
configuration that is different still. Under this arrangement
Glassfish instance is running on a high local port unaccessible from
the outside, and is "hidden" behind Apache. With the latter running on
the default https port, accepting and proxying the incoming
connections to the former. This is described in the `Shibboleth <shibboleth.html>`_
section of the Installation Guide (please note that, at the moment,
this functionality is offered as "experimental"). With this proxying
setup in place, the TwoRavens and rApache configuration actually
becomes simpler. As both the Dataverse and TwoRavens will be served on
the same port - 443 (the default port for https). So when running the
installer script above, and providing you are planning to run both on
the same server, enter "https", your host name and "443" for the
rApache protocol, host and port, respectively. The base URL of the
Dataverse app will be simply https://{your host name}/.


Appendix
++++++++

Explained below are the steps needed to manually install and configure the required R packages, and to configure TwoRavens to run under rApache (these are performed by the ``r-setup.sh`` and ``install.pl`` scripts above).  Provided for reference. 

r-setup.sh script:
++++++++++++++++++

TwoRavens requires the following R packages and versions to be installed:

=============== ================
R Package       Version Number
=============== ================
Zelig           5.0.5
Rook            1.1.1
rjson           0.2.13
jsonlite        0.9.16
DescTools       0.99.11
=============== ================

Note that some of these packages have their own dependencies, and additional installations are likely necessary. TwoRavens is not compatible with older versions of these R packages.

install.pl script:
++++++++++++++++++

I. Configure the TwoRavens web (Javascript) application
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

``url <- paste("https://demo.dataverse.org/custom/preprocess_dir/preprocessSubset_",sessionid,".txt",sep="")``

and 

``imageVector[[qicount]]<<-paste("https://dataverse-demo.iq.harvard.edu/custom/pic_dir/", mysessionid,"_",mymodelcount,qicount,".png", sep = "")``

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
   
   mkdir --parents /var/www/html/custom/log_dir

   chown -R apache.apache /var/www/html/custom

f. chown the dataexplore directory 
**********************************
to user apache: 

``chown -R apache /var/www/html/dataexplore``

g. restart httpd
****************

``service httpd restart``

III. Enable TwoRavens' Explore Button in Dataverse
--------------------------------------------------

The final step of TwoRavens' installation is to tell Dataverse to display its Explore button alongside tabular datafiles by executing the following command on the Glassfish host:

``curl -X PUT -d true http://localhost:8080/api/admin/settings/:TwoRavensTabularView``
