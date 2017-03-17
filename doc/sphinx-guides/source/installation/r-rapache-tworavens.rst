.. role:: fixedwidthplain

TwoRavens Application
=====================

TwoRavens is a web application for tabular data exploration and statistical analysis.
It can be integrated with Dataverse, as an **optional** component.  The
software was created as a standalone tool, by an independent
development team at IQSS (outside of the main Dataverse
development). TwoRavens was successfully integrated with Dataverse and
made available to the users of the production Dataverse cluster at
Harvard. However, the original developers have since left IQSS. Plans
for the future of this collaboration are still being worked out. As
the result, **the support for TwoRavens is somewhat limited at the
moment (as of Spring of 2017).**

Any questions regarding the features of TwoRavens, bug reports and
such, should be addressed directly to the developers of the
application.  The `TwoRavens GitHub repository
<https://github.com/IQSS/TwoRavens>`_ and the `TwoRavens project page
<http://2ra.vn/community/index.html>`_ are good places to start.

At the same time the Dataverse project will try to continue providing the 
installation and integration support. We have created a new (as
of Dataverse v.4.6.1) version of the installer scripts and updated the
guide, below. We have tried to improve and somewhat simplify the
installation process. Specifically, the notoriously tricky part of getting the
correct versions of the required third party R packages installed.

Please be warned: 

- The process may still require some system administration skills. 
- The guide below is very Linux-specific. This process has been tested
  on RedHat/CentOS servers only. In some ways it *may* actually be
  easier to get it all installed on MacOS X (specifically because
  MacOS X versions of third party R packages are available
  pre-compiled), or even on Windows. But it hasn't been attempted, and
  is not supported by the Dataverse team.

Besides the TwoRavens web application proper, several required
components need to be installed and configured. This includes R,
rApache and a collection of required third-party R packages. The
installation steps for these components are described in the
individual sections of the document below.


0. OVERVIEW
+++++++++++

TwoRavens is itself a compact JavaScript application that **runs on the user's 
browser**. These JavaScript files, and the accompanying HTML, CSS, etc. files 
are served by an HTTP server (Apache) as static objects. 

The statistical calculations are performed by R programs that run **on the server**. 
`rApache <http://rapache.net/>`_ is used as the web front for R on the server, so 
that the browser application can talk to R over HTTP. 

When a user requests to run 
a statistical model on a datafile, TwoRavens will instruct the R code on the 
server to download the file **directly from the Dataverse application**. Access 
URLs need to be configured for this to work properly (this is done by the TwoRavens 
installer script in step ``3.``)  

The application itself will need to obtain some tabular data-specific metadata from 
the Dataverse - the DDI fragment that describes the variables and some pre-processed
summary statistics for the data vectors. In order to produce the latter, the Dataverse
application also needs to be able to execute some R code on the server. Instead of 
``rApache``, Dataverse uses `Rserve <https://rforge.net/Rserve/>`_ to 
communicate to R. Rserve is installed as a "contributor" R package. It runs as a 
daemon process on the server accepting network connections on a dedicated port. 
Dataverse project supplies an :fixedwidthplain:`init.d`-style startup file for the 
daemon. The R setup in step ``2.`` will set it up so that the daemon gets started
automatically when the system boots. 

In addition to Rserve, there are 14 more R library packages that the TwoRavens R 
code requires in order to run. These in turn require 30 more as their own dependencies. 
So the total of 45 packages must be installed. "Installed" in the 
context of an R package means R must download the **source code** from the `CRAN 
<https://cran.r-project.org/>`_ code repository and compile it locally. This
historically has been the trickiest, least stable part of the installation process, 
since the packages in question are being constantly (and independently) developed. 
Which means that every time you attempt to install these packages, you are building  
from potentially different versions of the source code. An incompatibility introduced 
between any two of the packages can result in a failure to install. In this release 
we have attempted to resolve this by installing the **specific  versions of the R 
packages that have been proven** to work together. If you have attempted to 
install TwoRavens in the past, and it didn't quite work, please see the part of 
section ``1.b.`` where we explain how to completely erase all the previously 
built packages.
 

1. PREREQUISITES
++++++++++++++++

a. httpd (Apache): 
------------------

It's probably installed already, but if not: 

``yum install httpd``

This rApache configuration does not work SELinux. Execute the following commands 
to disable SELinux: 

``setenforce permissive``

``getenforce``

(Note: If you can get rApache to work with SELinux, we encourage you to make a pull request! Please see the :doc:`/developers/selinux` section of the Developer Guide to get started.)

If you choose to to serve TwoRavens and run rApache under :fixedwidthplain:`https`, a "real", signed certificate (as opposed to self-signed) is recommended. 

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

Can be installed with :fixedwidthplain:`yum`::

       yum install R R-devel

EPEL distribution recommended; version 3.3.2 is **strongly** recommended.

If :fixedwidthplain:`yum` isn't configured to use EPEL repositories: 

CentOS users can install the RPM :fixedwidthplain:`epel-release`.

RHEL users will want to log in to their organization's respective RHN interface, find the particular machine in question and:

• click on "Subscribed Channels: Alter Channel Subscriptions"
• enable EPEL, Server Extras, Server Optional

If you are upgrading an existing installation of TwoRavens; or if you have attempted to 
install it in the past, and it didn't quite work, **we strongly recommend reinstalling 
R completely**, erasing all the extra R packages that may have been already built. 

Uninstall R::

        yum erase R R-devel

Wipe clean any R packages that were left behind:: 

        rm -rf /usr/lib64/R/library/*
        rm -rf /usr/share/R/library/*

... then install R with :fixedwidthplain:`yum`.  

c. rApache: 
-----------

For RHEL/CentOS 6, we recommend the rpm built by the HMDC systems group:: 

install rApache as follows:: 

	rpm -ivh http://mirror.hmdc.harvard.edu/HMDC-Public/RedHat-6/rapache-1.2.6-rpm0.x86_64.rpm

If you are using RHEL/CentOS 7, you can download our experimental :download:`rapache-1.2.7-rpm0.x86_64.rpm <../_static/installation/files/home/rpmbuild/rpmbuild/RPMS/x86_64/rapache-1.2.7-rpm0.x86_64.rpm>` and install it with::

	rpm -ivh rapache-1.2.7-rpm0.x86_64.rpm

Both distributions require libapreq2. You should be able to install it with :fixedwidthplain:`yum`::

        yum install libapreq2 

d. Install the build environment for R:
---------------------------------------

Once again, extra R packages will need to be built from sources. Make sure you have the standard GNU compilers installed: ``gcc``, ``gcc-c++`` and ``gcc-fortran``. 

One of the required packages needed :fixedwidthplain:`/bin/ed`. The R package build script needs :fixedwidthplain:`/usr/bin/wget`. If these are missing, the rpms can be installed with::

        yum install ed wget

Depending on how your system was originally set up, you may end up needing to install some other missing rpms. We'll explain how to troubleshoot compiler errors caused by missing libraries and/or executables. 

2. Install Extra R Packages
+++++++++++++++++++++++++++

We provide a shell script (``r-setup.sh``) that will try to install all the needed packages. **Note:** the script is now part of the TwoRavens distribution (it **used to be** in the Dataverse source tree). 

The script will attempt to download the packages from CRAN (or a mirror), so the system must have access to the Internet.

In order to run the script: 

Download the TwoRavens distribution from `https://github.com/IQSS/TwoRavens/archive/master.zip <https://github.com/IQSS/TwoRavens/archive/master.zip>`_.
Unpack the zip file, then run the script::

        unzip master.zip
        cd TwoRavens/r-setup
        chmod +x r-setup.sh
        ./r-setup.sh


See the section ``II.`` of the Appendix for trouble-shooting tips. 

For the Rserve package the setup script will also create a system user :fixedwidthplain:`rserve`, and install the startup script for the daemon (``/etc/init.d/rserve``). 
The script will skip this part, if this has already been done on this system (i.e., it should be safe to run it repeatedly). 

Note that the setup will set the Rserve password to :fixedwidthplain:`"rserve"`. 
Rserve daemon runs under a non-privileged user id, and there appears to be a 
very limited potential for security damage through unauthorized access. It is however 
still a good idea **to change the it**. The password is specified in ``/etc/Rserve.pwd``. 
Please see `Rserve documentation <https://rforge.net/Rserve/doc.html>`_ for more 
information on password encryption and access security. 
 
Make sure the rserve password is correctly specified in the ``domain.xml`` of your Dataverse::

        <jvm-options>-Ddataverse.rserve.password=...</jvm-options>


3. Install the TwoRavens Application
++++++++++++++++++++++++++++++++++++

a. download the application:
----------------------------

(though you may have already done so, in step 2., above). 

For example::

        wget https://github.com/IQSS/TwoRavens/archive/master.zip

b. unzip...  
-----------

...and **rename the resulting directory** ``dataexplore``.
Place it in the web root directory of your apache server. We'll assume ``/var/www/html/dataexplore`` in the examples below::

        unzip master.zip
        mv TwoRavens /var/www/html/dataexplore


c. run the installer
--------------------

a scripted, interactive installer is provided at the top level of the TwoRavens 
distribution. Run it as::

   cd /var/www/html/dataexplore
   chmod +x install.pl
   ./install.pl

The installer will ask you to provide the following:

===================== ================================    ===========  
Setting               default                             Comment
===================== ================================    ===========  
TwoRavens directory   ``/var/www/html/dataexplore``       File directory where TwoRavens is installed.
Apache config dir.    ``/etc/httpd``                      rApache config file for TwoRavens will be placed under ``conf.d/`` there.
Apache web dir.       ``/var/www/html``                   
rApache/TwoRavens URL ``http://{local hostname}:80``      (**see the Appendix for the discussion on ports!**)
Dataverse URL         ``http://{local hostname}:8080``    URL of the Dataverse from which TwoRavens will be receiving metadata and data files.
===================== ================================    =========== 


Once everything is installed and configured, the installer script will print out a confirmation message with the URL of the TwoRavens application. For example: 

The application URL is 
https://server.dataverse.edu/dataexplore/gui.html

d. Enable TwoRavens' Explore Button in Dataverse
------------------------------------------------

Now that you have installed TwoRavens, the following must be done in order to 
integrate it with your Dataverse. 

First, enable the Data Explore option:: 

        curl -X PUT -d true http://localhost:8080/api/admin/settings/:TwoRavensTabularView
 
Once enabled, the 'Explore' button will appear next to ingested tabular data files; clicking it will redirect
the user to the instance of TwoRavens, initialized with the data variables from the selected file. 

Then, the TwoRavens URL must be configured in the settings of your Dataverse application - so that it knows where to redirect the user. 
This can be done by issuing the following API call::

        curl -X PUT -d {TWORAVENS_URL} http://localhost:8080/api/admin/settings/:TwoRavensUrl

where :fixedwidthplain:`{TWORAVENS_URL}` is the URL reported by the installer script (as in the example at the end of step ``c.``, above).

4. Appendix
+++++++++++


I. Ports configuration discussion
---------------------------------

By default, Glassfish will install itself on ports 8080 and 8181 (for
http and https, respectively), and Apache - on port 80 (the default
port for http). Under this configuration, your Dataverse will be
accessible at ``http://{your host}:8080``,
and rApache - at ``http://{your host}/``. The TwoRavens installer, above,
will default to these values (and assume you are running both the
Dataverse and TwoRavens/rApache on the same host).

This configuration may be the easiest to set up if you are simply
trying out/testing the Dataverse and TwoRavens. Accept all the
defaults, and you should have a working installation in no
time. However, if you are planning to use this installation to
actually serve data to real users, you'll probably want to run
Glassfish on ports 80 and 443. This way, there will be no non-standard
ports in the Dataverse url visible to the users. Then you'll need to
configure the Apache to run on some other port - for example, 8080,
instead of 80. This port will only appear in the URL for the TwoRavens
app. If you want to use this configuration - or any other that is not
the default one described above! - it is your job to reconfigure
Glassfish and Apache to run on the desired ports **before** you run
the TwoRavens installer.

Furthermore, while the default setup assumes http as the default
protocol for both the Dataverse and TwoRavens, https is strongly
recommended for a real production system. Again, this will be your
responsibility, to configure https in both Glassfish and
Apache. Glassfih comes pre-configured to run https on port 8181, with
a *self-signed certificiate*. For a production system, you will most
certainly will want to obtain a properly signed certificate and
configure Glassfish to use it. Apache does not use https out of the
box at all. Again, it is the responsibility of the installing user, to
configure Apache to run https, and, providing you are planning to run
rApache on the same host as the Dataverse, use the same SSL
certificate as your Glassfish instance. Again, it will need to be done
before you run the installer script above. All of this may involve
some non-trivial steps and will most likely require help from your
local network administrator - unless you happen to be your local
sysadmin. Unfortunately, we cannot provide step-by-step instructions
for these tasks. As the actual steps required will likely depend on
the specifics of how your institution obtains signed SSL certificates,
the format in which you receive these certificates, etc. **Good
luck!**

Finally: If you choose to have your Dataverse support secure
**Shibboleth authentication**, this require an arrangement Glassfish
instance is running on a high local port unaccessible from the
outside, and is "hidden" behind Apache. With the latter running on the
default https port, accepting and proxying the incoming connections to
the former. This is described in the :doc:`shibboleth` section of the
Installation Guide. It is possible to have TwoRavens hosted on the
same APache server. In fact, with this proxying setup in place, the
TwoRavens and rApache configuration becomes somewhat simpler. As both
the Dataverse and TwoRavens will be served on the same port - 443 (the
default port for https). So when running the installer script above,
enter "https", your host name and "443" for the rApache protocol, host
and port, respectively. The base URL of the Dataverse app will be
simply https://{your host name}/.



II. What the r-setup.sh script does:
------------------------------------

The script uses the list of 45 R library packages and specified
package versions, supplied in ``TwoRavens/r-setup/package-versions.txt`` to 
replicate the library environment that has been proven to work on the Dataverse
servers. 

If any packages fail to build, the script will alert the user. 

For every package, the (potentially verbose) output of the build process is saved in 
its own file, ``RINSTALL.{PACKAGE NAME}.LOG``. So if, for example, the package 
Zelig fails to install, the log file :fixedwidthplain:`RINSTALL.Zelig.LOG` should 
be consulted for any error messages that may explain the reason for the failure; 
such as a missing library, or a missing compiler, etc. Be aware that diagnosing 
compiler errors will require at least some programming and/or system administration 
skills. 


III. What the install.pl script does:
-------------------------------------

The steps below are performed by the ``install.pl`` script. **Provided for reference only!** 
The instruction below could be used to configure it all by hand, if necessary, or 
to verify that the installer has done it correctly. 
Once again: normally you **would NOT need to individually perform the steps below**!

TwoRavens is distributed with a few hard-coded host and directory names. So these 
need to be replaced with  the values specific to your system. 


**In the file** ``/var/www/html/dataexplore/app_ddi.js`` **the following 3 lines need to be 
edited:**

1. ``var production=false;``

   changed to ``true``;

2. ``hostname="localhost:8080";``

   changed to point to the dataverse app, from which TwoRavens will be obtaining the metadata and data files. (don't forget to change 8080 to the correct port number!)

3. ``var rappURL = "http://0.0.0.0:8000/custom/";``

   changed to the URL of your rApache server, i.e.

   ``"http(s)://<rapacheserver>:<rapacheport>/custom/";``

**In** ``dataexplore/rook`` **the following files need to be edited:**

``rookdata.R, rookzelig.R, rooksubset.R, rooktransform.R, rookselector.R, rooksource.R``

replacing *every* instance of ``production<-FALSE`` line with ``production<-TRUE``.
 
(yeah, that's why we provide that installer script...)


**In** ``dataexplore/rook/rooksource.R`` **the following line:**

``setwd("/usr/local/glassfish4/glassfish/domains/domain1/docroot/dataexplore/rook")``

needs to be changed to: 

``setwd("/var/www/html/dataexplore/rook")``

(or your :fixedwidthplain:`dataexplore` directory, if different from the above)

**In** ``dataexplore/rook/rookutils.R`` **the following lines need to be edited:**

``url <- paste("https://beta.dataverse.org/custom/preprocess_dir/preprocessSubset_",sessionid,".txt",sep="")``

and 

``imageVector[[qicount]]<<-paste("https://beta.dataverse.org/custom/pic_dir/", mysessionid,"_",mymodelcount,qicount,".png", sep = "")``

changing the URL to reflect the correct location of your rApache instance. make sure that the protocol (http vs. https) and the port number are correct too, not just the host name!


**Next, in order to configure rApache to serve several TwoRavens "mini-apps",** 

the installer creates the file ``tworavens-rapache.conf`` in the Apache's ``/etc/httpd/conf.d`` directory with the following configuration:

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

**The following directories are created by the installer to store various output files produced by TwoRavens:**

.. code-block:: none

   mkdir --parents /var/www/html/custom/pic_dir
   
   mkdir --parents /var/www/html/custom/preprocess_dir
   
   mkdir --parents /var/www/html/custom/log_dir

**The ownership of the TwoRavens directories is changed to user** ``apache``:

.. code-block:: none

   chown -R apache.apache /var/www/html/custom

   chown -R apache /var/www/html/dataexplore

**Finally, the installer restarts Apache, for all the changes to take effect:**

``service httpd restart``
