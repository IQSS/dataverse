====================================
Installers Guide
====================================

.. _introduction:

**Introduction**

This is our "new and improved" installation guide, it was first
released with the Dataverse Network application versions 2.2.4, when we
introduced the new, automated and much simplified installation process.
As of February 2012, it has been updated to reflect the changes made in
the newly released version 3.0 of the software. (Our existing users will
notice however, that the changes in the installation process have been
fairly minimal).

The guide is intended for anyone who needs to install the DVN app,
developers and Dataverse Network administrators alike.

The top-down organization of the chapters and sections is that of
increasing complexity. First a very basic, simple installation scenario
is presented. The instructions are straightforward and only the required
components are discussed. This use case will in fact be sufficient for
most DVN developers and many Dataverse Network administrators. Chances
are you are one of such users, so if brave by nature, you may stop
reading this section and go straight to the :ref:`“Quick Install” <quick-install>` chapter.

The “basic” installation process described in the first chapter is
fully automated, everything is performed by a single interactive script.
This process has its limitations. It will likely work only on the
supported platforms. Optional components need to be configured  outside
of the Installer (these are described in the "Optional Components"
section).

For an advanced user, we provide the detailed explanations of all the
steps performed by the Installer. This way he or she can experiment with
individual configuration options, having maximum flexibility and control
over the process. Yet we tried to organize the advanced information in
such a way that those who only need the most basic instructions would
not have to read through it unnecessarily.  Instead we provide them with
an easy way to get a bare-bones configuration of the DVN up and running.

If you are interested in practicing a DVN installation in a Vagrant
environment you can later throw away, please follow the instructions at
https://github.com/dvn/dvn-install-demo to spin up a Linux virtual
machine on your laptop with ``vagrant up``. When you are finished with
this temporary DVN installation, you can delete the virtual machine with
``vagrant destroy``.

If you encounter any problems during installation, please contact the
development team
at `support@thedata.org <mailto:support@thedata.org>`__
or our `Dataverse Users
Community <https://groups.google.com/forum/?fromgroups#!forum/dataverse-community>`__.

.. _quick-install:

Quick Install
++++++++++++++++++++++

For an experienced and/or rather bold user, this is a 1
paragraph version of the installation instructions: 

This should work on RedHat and its derivatives, and MacOS X. If this
does not describe your case, you will very likely have to install and
configure at least some of the components manually. Meaning, you may
consider reading through the chapters that follow! Still here? Great.
Prerequisites: Sun/Oracle Java JDK 1.6\_31+ and a “virgin” installation
of Glassfish v3.1.2; PostgreSQL v8.3+, configured to listen to network
connections and support password authentication on the localhost
interface; you may need R as well. See the corresponding sections under
“2. Prerequisites”, if necessary. Download the installer package from
SourceForge:

`http://sourceforge.net/projects/dvn/files/dvn <http://sourceforge.net/projects/dvn/files/dvn>`__

Choose the latest version and download the dvninstall zip file.

Unzip the package in a temp location of your choice (this will create
the directory ``dvninstall``). Run the installer, as root: 

          ``cd dvninstall``
           ./ ``install``

Follow the installation prompts. If it all works as it should, you
will have a working DVN instance running in about a minute from now.

Has it worked? Awesome! Now you may read the rest of the guide
chapters at your own leisurely pace, to see if you need any of the
optional components described there. And/or if you want to understand
what exactly has just been done to your system.

SYSTEM REQUIREMENTS
++++++++++++++++++++++++++++++++++

Or rather, recommendations. The closer your configuration is to what’s
outlined below, the easier it will be for the DVN team to provide
support and answer your questions.

-  Operating system - The production version of the Dataverse Network at
   IQSS (dvn.iq.harvard.edu) runs on RedHat Linux 5. Most of the DVN
   development is currently done on MacOS X. Because of our experience
   with RedHat and MacOS X these  are the recommended platforms. You
   should be able to deploy the application .ear file on any other
   platform that supports Java. However, the automated installer we
   provide will likely work on RedHat and MacOS only. Some information
   provided in this guide is specific to these 2 operating systems. (Any
   OS-specific instructions/examples will be clearly marked, for
   example:\ ``[MacOS-specific:]``)

-  CPU - The production IQSS Dataverse Network runs on generic,
   multi-core 64-bit processors. 

-  Memory - The application servers currently in production at the IQSS
   have 64 GB of memory each.  Development and testing systems require a
   minimum of 2 gigabyte of memory.

-  Disk space - How much disk space is required depends on the amount of
   data that you expect to serve. The IQSS Dataverse Network file system
   is a standalone NetApp with 2 TB volume dedicated to the DVN data.

-  Multiple servers – All the DVN components can run on the same server.
   On a busy, hard-working production network the load can be split
   across multiple servers. The 3 main components, the application
   server (Glassfish), the database (Postgres) and R can each run on its
   own host. Furthermore, multiple application servers sharing the same
   database and R server(s)  can be set up behind a load balancer.
   Developers would normally run Glassfish and Postgres on their
   workstations locally and use a shared R server.

-  If it actually becomes a practical necessity to bring up more servers
   to handle your production load, there are no universal instructions
   on how to best spread it across extra CPUs. It will depend on the
   specifics of your site, the nature of the data you serve and the
   needs of your users, whether you’ll benefit most from dedicating
   another server to run the database, or to serve R requests. Please
   see the discussion in the corresponding sections of the Prerequisites
   chapter.

.. _prerequisites:

PREREQUISITES
++++++++++++++++++++++++++

In this chapter, an emphasis is made on clearly identifying those
components that are absolutely required for  every installation and
marking any advanced, optional instructions as such.

Glassfish
=======================

Version 3.1.2 is required.

Make sure you have **Sun/Oracle**\ **Java JDK version 1.6, build 31**
or newer\. It is available from
`http://www.oracle.com/technetwork/java/javase/downloads/index.html <http://www.oracle.com/technetwork/java/javase/downloads/index.html>`__.


**[note for developers:]**

If you are doing this installation as part of your DVN software
development setup: The version of NetBeans currently in use by the DVN
team is 7.0.1, and it is recommended that you use this same version if
you want to participate in the development. As of writing of this
manual, NetBeans 7.0.1 installer bundle comes with an older version of
Glassfish. So you will have to install Glassfish version 3.1.2
separately, and then select it as the default server for your NetBeans
project.

**[/note for developers]**

We **strongly** recommend that you install GlassFish Server 3.1.2,
Open Source Edition, **Full Platform**. You are very likely to run into
installation issues if you attempt to run the installer and get the
application to work with a different version! Simply transitioning from
3.1.1 to 3.1.2 turned out to be a surprisingly complex undertaking,
hence this recommendation to all other installers and developers to stay
with the same version.

It can be obtained from

`http://glassfish.java.net/downloads/3.1.2-final.html <http://glassfish.java.net/downloads/3.1.2-final.html>`__

The page contains a link to the installation instructions. However,
the process is completely straightforward. You are given 2 options for
the format of the installer package. We recommend that you choose to
download it as a shell archive; you will need to change its executable
permission, with **chmod +x**, and then run it, as root:

./**installer-filename.sh**

[**Important:]**

Leave the admin password fields blank. This is not a security risk,
since out of the box, Glassfish will only be accepting admin connections
on the localhost interface. Choosing password at this stage however will
complicate the installation process unnecessarily\ **.**\ If this is a
developers installation, you can probably keep this configuration
unchanged (admin on localhost only). If you need to be able to connect
to the admin console remotely, please see the note in the Appendix
section of the manual.

**[/Important]**

| **[Advanced:]**
| **[Unix-specific:`]**

The installer shell script will normally attempt to run in a graphic
mode. If you are installing this on a remote Unix server, this will
require X Windows support on your local workstation. If for whatever
reason it's not available, you have an option of running it in a *silent
mode* - check the download page, above, for more information.

| **[/Unix-specific]**
| **[/Advanced]**

.. _postgresql:

PostgreSQL
=======================

| **Version 8.3 or higher is required.**
| Installation instructions specific to RedHat Linux and MacOS X are
| provided below.
| Once the database server is installed, you'll need to configure access
| control to suit your installation.
| Note that any modifications to the configuration files above require you to restart Postgres:
| ``service postgresql restart`` (RedHat)

| or
| "Restart Server" under Applications -> PostgreSQL (MacOS X)

By default, most Postgres distributions are configured to listen to network connections on the localhost interface only; and to only support ident for authentication. (The MacOS installer may ask you if network connections should be allowed - answer "yes"). At a minimum, if GlassFish is running on the same host, it will also need to allow password authentication on localhost. So you will need to modify the "``host all all 127.0.0.1``\ " line in your ``/var/lib/pgsq1/data/pg_hba.conf`` so that it looks like this:

|         ``host all all 127.0.0.1/32 password``

Also, the installer script needs to have direct access to the local PostgresQL server via Unix domain sockets. So this needs to be set to either "trust" or "ident". I.e., your **pg\_hba.conf** must contain either of the 2 lines below:

| **local   all  all   ident    sameuser**
| or
| **local   all  all  trust**

("ident" is the default setting; but if it has been changed to
"password" or "md5", etc. on your system, Postgres will keep prompting
you for the master password throughout the installation)

**[optional:]**

If GlassFish will be accessing the database remotely, add or modify the following line in your ``<POSTGRES DIR>/data/postgresql.conf``:

| ``listen_addresses='*'``

to enable network connections on all interfaces; and add the following
line to ``pg_hba.conf``:

| host       all      all      ``[ADDRESS]      255.255.255.255 password``

| where ``[ADDRESS]`` is the numeric IP address of the GlassFish server.
| Using the subnet notation above you can enable authorization for multiple hosts on | your network. For example,

| ``host all all 140.247.115.0 255.255.255.0 password``

| will permit password-authenticated connections from all hosts on the ``140.247.115.*`` subnet.
| **[/optional:]**

| 
| **[RedHat-specific:]**
| **[Advanced:]**

Please note that the instructions below are meant for users who have some experience with basic RedHat admin tasks. You should be safe to proceed if an instruction such as “uninstall the postgres rpms” makes sense to you immediately. I.e., if you already know how to install or uninstall an rpm package. Otherwise we recommend that you contact your systems administrator.

For RedHat (and relatives), version 8.4 is now part of the distribution. As of RedHat 5, the default ``postgresql`` rpm is still version 8.1. So you may have to un-install the ``postgresql`` rpms, then get the ones for version 8.4:

|         ``yum install postgresql84 postgresql84-server``

Before you start the server for the first time with

| ``service postgresql start``

You will need to populate the initial database with


| ``service postgresql initdb``


| **[/advanced]**
| **[/RedHat-specific]**


**[MacOS-specific:]**


Postgres Project provides a one click installer for Mac OS X 10.4 and
above at
`http://www.postgresql.org/download/macosx <http://www.postgresql.org/download/macosx>`__.
Fink and MacPorts packages are also available.


**[/MacOS-specific]`**


| **[advanced:]**
| **[optional:]**

See the section :ref:`PostgresQL setup <postgresql-setup>` in the Appendix for the description of the steps that the automated installer takes to set up PostgresQL for use with the DVN.  

| **[/optional]**
| **[/advanced]**

.. _r-and-rserve:

R and RServe
=======================

Strictly speaking, R is an optional component. You can bring up a
running DVN instance without it. The automated installer will allow such
an installation, with a warning. Users of this Dataverse Network will be
able to upload and share some data. Only the advanced modes of serving
quantitative data to the users require R ``[style?]``. Please consult
the :ref:`"Do you need R?" <do-you-need-r>` section in the Appendix for an extended discussion of this.


| **Installation instructions:** 

Install the latest version of R from your favorite CRAN mirror (refer to `http://cran.r-project.org/ <http://cran.r-project.org/>`__ for more information). Depending on your OS distribution, this may be as simple as typing

| **[RedHat/Linux-specific:]**

``yum install R R-devel``

(for example, the above line will work in CentOS out of the box; in RedHat, you will have to add support for EPEL repository -- see
`http://fedoraproject.org/wiki/EPEL <http://fedoraproject.org/wiki/EPEL>`__
-- then run the ``yum install`` command)

| **[/RedHat/Linux-specific]**

Please make sure to install the "devel" package too! you will need it
to build the extra R modules.

Once you have R installed, download the package ``dvnextra.tar`` from this location:

`http://dvn.iq.harvard.edu/dist/R/dvnextra.tar <http://dvn.iq.harvard.edu/dist/R/dvnextra.tar>`__

Unpack the archive:

``tar xvf dvnextra.tar``

then run the supplied installation shell script as root:

|  ``cd dvnextra``
| ``./installModules.sh``

This will install a number of R modules needed by the DVN to run statistics and analysis, some from CRAN and some supplied in the bundle; it will also configure Rserve to run locally on your system and install some startup files that the DVN will need.

**Please note that the DVN application requires specific versions of the 3rd-party R packages. For example, if you obtain and install the version of Zelig package currently available from CRAN, it will not work with the application. This is why we distribute the sources of the correct versions in this tar package.**


| **[advanced:]**
| We haven’t had much experience with R on any platforms other than RedHat-and-the-like. Our developers use MacOS X, but point their DVN instances to a shared server running Rserve under RedHat.

The R project ports their distribution to a wide range of platforms. However, the installer shell script above will only run on Unix; and is not really guaranteed to work on anything other than RedHat. If you have some experience with either R or system administration, you should be able to use the script as a guide to re-create the configuration steps on any other platform quite easily. You will, however, be entirely on your own while embarking on that adventure.
**[/advanced]**



System Configuration
================================

**[Advanced/optional:]**

Many modern OS distributions come pre-configured so that all the
network ports are firewalled off by default.

Depending on the configuration of your server, you may need to open some
of the following ports.

On a developers personal workstation, the user would normally access his
or her DVN instance on the localhost interface. So no open ports are
required unless you want to give access to your DVN to another
user/developer.

When running a DVN that is meant to be accessible by network users: At a
minimum, if all the components are running on the same server, the HTTP
port 80 needs to be open. You may also want to open TCP 443, to be able
to access Glassfish admin console remotely.

If the DVN is running its own HANDLE.NET server (see Chapter 4.
"Optional Components"), the TCP port 8000 and TCP/UDP ports 2641 are
also needed.

If the DVN application needs to talk to PostgreSQL and/or Rserve running
on remote hosts, the TCP ports 5432 and 6311, respectively, need to be
open there.

**[/Advanced/optional]**



RUNNING THE INSTALLER
+++++++++++++++++++++++++++++++++++++++++

Once the :ref:`Prerequisites <prerequisites>` have been take care of, the DVN application can be installed.

The installer package can be downloaded from our repository on SourceForge at

`http://sourceforge.net/projects/dvn/files/dvn/3.0/dvninstall\_v3\_0.zip <http://sourceforge.net/projects/dvn/files/dvn/3.0/dvninstall_v3_0.zip>`_

| Unzip the package in a temp location of your choice (this will create the directory | ``dvninstall``). Run the installer, as root:
|         ``cd dvninstall``
|         ``./install``

Follow the installation prompts. The installer will first verify the contents of the package and check if the required components
(in :ref:`Prerequisites <prerequisites>`) are present on the system. Then it will lead you through the application setup.

| **[Advanced:]**

The limitations of the installer package:

Some extra configuration steps will be required if the PostgreSQL database is being set up on a remote server.

It will most likely only work on the supported platforms, RedHat and Mac OS X.

It is only guaranteed to work on a fresh Glassfish installation. If you already have more than one Glassfish domains created and/or have applications other than the DVN running under Glassfish, please consult the “Manual Installation and Configuration” in the Addendum.

It does not install any of the optional components (:ref:`see Chapter 4<optional-components>`.) 

For the detailed explanation of the tasks performed by the Installer, see the “Manual Installation and Configuration” in the Addendum.

| **[/Advanced]**

.. _optional-components:

Optional Components
++++++++++++++++++++++++++

``[The sections on ImageMagick, Google Analytics and Captcha have been rewritten and, hopefully, made less confusing. The Handles instructions have also been modified, but I would like to work on it some more. Namely I'd like to read their own technical manual, and see if we should provide our own version of installation instructions, similarly to what we do with some other packages; we've heard complaints from users about their manual not being very easy to follow]``

reCAPTCHA bot blocker
=================================

We found that our “email us” feature can be abused to send spam
messages. You can choose to use the reCAPTCHA filter to help prevent
this. Configure the filter as follows:

#. | Go to reCAPTCHA web site at
   | `http://recaptcha.net/ <http://recaptcha.net/>`_ 
   | and sign up for an account.
   | Register your website domain to acquire a public/private CAPTCHA key pair.
   | Record this information in a secure location.
#. Insert the the public/private key pair and domain for your reCAPTCHA
   account into the ``captcha`` table of the DVN PostgreSQL database.
   Use ``psql``, ``pgadmin`` or any other database utility; the SQL
   query will look like this:
   ``INSERT INTO captcha (publickey, domainname, privatekey) VALUES ('sample', 'sample.edu', 'sample')``
#. Verify that the Report Issue page is now showing the reCAPTCHA
   challenge.

Google Analytics
================================

Network Admins can use the Google Analytics tools to view Dataverse Network website usage statistics.

Note: It takes about 24 hours for Google Analytics to start monitoring
your website after the registration.

| 
| To enable the use of Google Analytics:

#. Go to the Google Analytics homepage at
   `http://www.google.com/analytics/indexu.html <http://www.google.com/analytics/indexu.html>`__.
#. Set up a Google Analytics account and obtain a tracking code for your Dataverse Network installation.
#. Use the Google Analytics Help Center to find how to add the tracking code to the content you serve.
#. Configure the DVN to use the tracking key (obtained in Step 2,
    above), by setting | the ``dvn.googleanalytics.key`` JVM option in
    Glassfish.
    
    This can be done by adding the following directly to the
    ``domain.xml`` config file (for example: ``/usr/local/glassfish/domains/domain1/confi/domain.xml``):
    ``<jvm-options>-Ddvn.googleanalytics.key=XX-YYY</jvm-options>`` (this will require Glassfish restart)

    Or by using the Glassfish Admin Console configuration GUI. Consult the “Glassfish Configuration” section in the Appendix. 

Once installed and activated, the usage statistics can be accessed from
the Network Options of the DVN.

ImageMagick
=======================

When image files are ingested into a DVN, the application
automatically creates small "thumbnail" versions to display on the
Files View page. These thumbnails are generated once, then cached for
future use.

Normally, the standard Java image manipulation libraries are used to
do the scaling. If you have studies with large numbers of large
images, generating the thumbnails may become a time-consuming task. If
you notice that the Files view takes a long time to load for the first
time because of the images, it is possible | to improve the
performance by installing the ``ImageMagick`` package. If it is
installed, the application will automatically use its
``/usr/bin/convert`` utility to do the resizing, which appears to be
significantly faster than the Java code.

``ImageMagick`` is available for, or even comes with most of the popular OS distributions.

 
| **<RedHat-Specific:>**

It is part of the full RedHat Linux distribution, although it is not
included in the default "server" configuration. It can be installed on a
RedHat server with the ``yum install ImageMagick`` command.

**</RedHat-Specific>**

Handle System
===========================

DVN administrators may choose to set up a `HANDLE.NET <http://www.handle.net/>`_ server to issue and register persistent, global identifiers for their studies. The DVN app can be modified to support other naming services, but as of now it comes
pre-configured to use Handles.

To install and set up a local HANDLE.NET server:

#. Download HANDLE.NET.
   Refer to the HANDLE.NET software download page at
   `http://handle.net/download.html <http://handle.net/download.html>`__.
#. Install the server on the same host as GlassFish.
   Complete the installation and setup process as described in the
   HANDLE.NET Technical Manual:
   `http://www.handle.net/tech_manual/Handle_Technical_Manual.pdf <http://www.handle.net/tech_manual/Handle_Technical_Manual.pdf>`__.
#. Accept the default settings during installation, **with one
   exception:** do not encrypt private keys (this will make it easier to
   manage the service). **Note** that this means answer 'n' when
   prompted "Would you like to encrypt your private key?(y/n). [y]:" If
   you accept the default 'y' and then hit return when prompted for
   passphrase, this **will** encrypt the key, with a blank pass phrase!
#. During the installation you will be issued an "authority prefix".
   This is an equivalent of a domain name. For example, the prefix
   registered to the IQSS DVN is "1902.1". The IDs issued to IQSS
   studies are of a form "1902.1/XXXX", where "XXXX" is some unique
   identifier.
#. Use ``psql`` or ``pgAdmin`` to execute the following SQL command:
   ``insert into handleprefix (prefix) values( '<your HANDLE.NET prefix>')``;
#. ``(Optional/advanced)`` If you are going to be assigning HANDLE.NET
   ids in more than 1 authority prefix (to register studies harvested
   from remote sources): Once you obtain the additional HANDLE.NET
   prefixes, add each to the ``handleprefix`` table, using the SQL
   command from step 3.
#. Use ``psql`` or ``pgAdmin`` to execute the following SQL
   command: ``update vdcnetwork set handleregistration=true, authority='<your HANDLE.NET prefix>';``

 

Note: The DVN app comes bundled with the HANDLE.NET client libraries.
You do not need to install these separately.

Twitter setup
======================

To set up the ability for users to enable Automatic Tweets in your
Dataverse Network:

#. You will first need to tell twitter about you Dataverse Network Application. Go to `https://dev.twitter.com/apps <https://dev.twitter.com/apps>`_ and login (or create a new Twitter account).
#. Click "Create a new application".
#. Fill out all the fields. For callback URL, use your Dataverse Network Home Page URL.
#. Once created, go to settings tab and set Application Type to "Read and Write". You can optionally also upload an Application
   Icon and fill out Organization details (the end user will see these.
#. Click details again. You will need both the Consumer key and secret as JVM Options. Add via Glassfish console:
      -Dtwitter4j.oauth.consumerKey=***


      -Dtwitter4j.oauth.consumerSecret=***
#. Restart Glassfish.
#. To verify that Automatic Tweets are now properly set up, you can go to the Dataverse Network Options page or any Dataverse Options page and see that their is a new option, "Enable Twitter".

Digital Object Identifiers
==========================

Beginning with version 3.6, DVN will support the use of Digital Object Identifiers.  Similar to the currently enabled Handle System, these DOIs will enable a permanent link to studies in a DVN network.  

DVN uses the EZID API (`www.n2t.net/ezid <http://www.n2t.net/ezid>`__) to facilitate the creation and maintenance of DOIs.  Network administrators will have to arrange to get their own account with EZID in order to implement creation of DOIs.  Once an account has been set up the following settings must be made in your DVN set-up:

Update your database with the following query:

Use ``psql`` or ``pgAdmin`` to execute the following SQL command: 
``update vdcnetwork set handleregistration=true,  protocol = 'doi', authority='<the namespace associated with your EZID account> where id = 0;``

Add the following JVM options:

``-Ddoi.username=<username of your EZID account>``

``-Ddoi.password=<password of your EZID account>``

Note: The DVN app comes bundled with the EZID API client libraries. You do not need to install these separately.

Appendix
+++++++++++++++++++++++

.. _do-you-need-r:

Do you need R?
==========================

This is a more detailed explanation of the statement made earlier in the "Prerequisites" section: "Only the advanced modes of serving quantitative data to the users require R." ``[style?]``

In this context, by “quantitative data” we mean data sets for which
machine-readable, variable-level metadata has been defined in the DVN
database. “Subsettable data” is another frequently used term, in the
DVN parlance. The currently supported sources of subsettable data are
SPSS and STATA files, as well as row tabulated or CSV files, with
extra control cards defining the data structure and variable
metadata. (See full documentation in User Guide for :ref:`Finding and Using Data <finding-and-using-data>`

Once a “subsettable” data set is create, users can run online statistics and analysis on it. That’s where R is used. In our experience, most of the institutions who have installed the DVN did so primarily in order to share and process quantitative data. When this is the case, R must be considered a required component. But a DVN network built  to serve a collection of strictly human-readable (text, image, etc.) data, R will not be necessary at all.

.. _what-does-the-intstaller-do:

What does the Installer do?
===================================

The Installer script (chapters Quick Install, Running the Installer.) automates the following tasks:

#. Checks the system for required components;
#. Prompts the user for the following information:

   a) Location of the Glassfish directory;

   b) Access information (host, port, database name, username, password) for PostgresQL;

   c) Access information (host, port, username, password) for Rserve;

#. Attempts to create the PostgreSQL user (role) and database, from :ref:`prerequisiste PostgreSQL setup step <postgresql>` above; see the :ref:`"PostgreSQL configuration"<postgresql-setup>` Appendix section for details.
#. Using the :ref:`Glassfish configuration template (section the Appendix) <glassfish-configuration-template>` and the information collected in step 2.b. above, creates the config file domain.xml and installs it the Glassfish domain directory.
#. Copies additional configuration files (supplied in the dvninstall/config directory of the Installer package) into the config directory of the Glassfish domain.
#. Installs Glassfish Postgres driver (supplied in the dvninstall/pgdriver directory of the Installer package) into the lib directory in the Glassfish installation tree.
#. Attempts to start Glassfish. The config file at this point contains the configuration settings that the DVN will need to run (see section :ref:`Glassfish Configuration, individual settings section<glassfish-configuration-individual-settings>` of the Appendix), but otherwise it is a "virgin", fresh config. Glassfish will perform some initialization tasks on this first startup and deploy some internal apps.
#. If step 5. succeeds, the Installer attempts to deploy the DVN application (the Java archive DVN-EAR.ear supplied with the installer).
#. Stops Glassfish, populates the DVN database with the initial content (section :ref:`"PostgreSQL configuration"<postgresql-setup>`" of the Appendix), starts Glassfish.
#. Attempts to establish connection to Rserve, using the access information obtained during step 2.c. If this fails, prints a warning message and points the user to the Prerequisites section of this guide where R installation is discussed.
#. Finally, prints a message informing the user that their new DVN should be up and running, provides them with the server URL and suggests that they visit it, to change the default passwords and perhaps start  setting up their Dataverse Network.

Throughout the steps above, the Installer attempts to diagnose any
potential issues and give the user clear error messages when things go
wrong ("version of Postgres too old", "you must run this as root",
etc.).

Enough information is supplied in this manual to enable a user (a
skilled and rather patient user, we may add) to perform all the steps
above without the use of the script.

.. _glassfish-configuration-template:

Glassfish configuration template
====================================

The configuration template (``domain.xml.TEMPLATE``) is part of the
installer zip package. The installer replaces the placeholder
configuration tokens (for example, ``%POSTGRES_DATABASE%``) with the
real values provided by the user to create the Glassfish configuration
file ``domain.xml``.

``[I was thinking of copy-and-pasting the entire template file here;
but it is 30K of XML, so I decided not to. The above explains where it
can be found, if anyone wants to look at it, for reference or
whatever]``

.. _glassfish-configuration-individual-settings:

Glassfish Configuration, individual settings
=====================================================

As explained earlier in the Appendix, the Installer configures Glassfish
by cooking a complete domain configuration file (``domain.xml``) and
installing it in the domain directory.

All of the settings and options however can be configured individually
by an operator, using the Glassfish Admin Console.

The Console can be accessed at the network port 4848 when Glassfish is
running, by pointing a browser at

     ``http://[your host name]:4848/``

and logging in as ``admin``. The initial password is ``adminadmin``. It
is of course strongly recommended to log in and change it first thing
after you run the Installer.

The sections below describe all the configuration settings that would
need to be done through the GUI in order to replicate the configuration
file produced by the Installer. This information is provided for the
benefit of an advanced user who may want to experiment with individual
options. Or to attempt to install DVN on a platform not supported by our
installer; although we wish sincerely that nobody is driven to such
desperate measures ever.

.. _jvm-options:

JVM options
-----------------------

Under Application Server->JVM Settings->JVM Options:

If you are installing Glassfish in a production environment, follow
these steps:

#. | Delete the following options: -Dsun.rmi.dgc.server.gcInterval=3600000
   | -Dsun.rmi.dgc.client.gcInterval=3600000
#. | Add the following options:
   | -XX:MaxPermSize=192m
   | -XX:+AggressiveHeap
   | -Xss128l
   | -XX:+DisableExplicitGC
   | -Dcom.sun.enterprise.ss.ASQuickStartup=false
#. | To install on a multi-processor machine, add the following:
   | ``-XX:+UseParallelOldGC``
#. | To enable the optional HANDLE.NET installation and provide access to
   | study ID registration, add the following (see the "Handles System"
   | section in the "Optional Components" for
   | details):
   | ``-Ddvn.handle.baseUrl=<-Dataverse Network host URL>/dvn/study?globalId=hdl:``
   | ``-Ddvn.handle.auth=<authority>``
   | ``-Ddvn.handle.admcredfile=/hs/svr_1/admpriv.bin``
#. | To enable the optional Google Analytics option on the Network Options
   | page and provide access to site usage reports, add the following (see
   | the "Google Analytics" section in the "Optional Components" for
   | details):
   |  ``-Ddvn.googleanalytics.key=<googleAnalyticsTrackingCode>``
#. | Configure the following option only if you run multiple instances
   | of the GlassFish server for load balancing. This option controls
   | which GlassFish instance runs scheduled jobs, such as harvest or
   | export.
   | For the server instance that will run scheduled jobs, include the
   | following JVM option:
   | ``-Ddvn.timerServer=true``
   | For all other server instances, include this JVM option:
   | ``-Ddvn.timerServer=false``
   | If you are installing Glassfish in either a production or development
   | environment, follow these steps:

   -  | Change the following options’ settings:
      | Change ``-client`` to ``-server``.
      | Change ``-Xmx512m`` to whatever size you can allot for the maximum 
      | Java heap  space.
      | Set `` –Xms512m`` to the same value to which you set ``–Xmx512m``.
   -  | To configure permanent file storage (data and documentation files
      | uploaded to studies) set the following:
      | ``-Dvdc.study.file.dir=${com.sun.aas.instanceRoot}/config/files/studies``
   -  | To configure the temporary location used in file uploads add the
      | following:
      | ``-Dvdc.temp.file.dir=${com.sun.aas.instanceRoot}/config/files/temp``
   -  | To configure export and import logs (harvesting and importing),
      | add the following:
      | -Dvdc.export.log.dir=${com.sun.aas.instanceRoot}/logs/export
      | -Dvdc.import.log.dir=${com.sun.aas.instanceRoot}/logs/import
   -  | Add the following:
      | -Djhove.conf.dir=${com.sun.aas.instanceRoot}/config
      | -Ddvn.inetAddress=<host or fully qualified domain name of server
      | on which Dataverse Network runs>
      | -Ddvn.networkData.libPath=${com.sun.aas.instanceRoot}/applications/j2ee-  
      |  apps/DVN-EAR
   -  | To manage calls to RServe and the R host (analysis and file upload), add 
      | the following:
      | ``-Dvdc.dsb.host=<RServe server hostname>``
      | ``-Dvdc.dsb.rserve.user=<account>``
      | ``-Dvdc.dsb.rserve.pwrd=<password>``
      | ``-Dvdc.dsb.rserve.port=<port number>``
     
      
      | For Installing R, see: 
      | :ref:`R and R-Serve <r-and-rserve>`
      | for information about configuring these values in the ``Rserv.conf``
      | file.
      | These settings must be configured for subsetting and analysis to
      | work.
   -  | To configure search index files set the following:
      | ``-Ddvn.index.location=${com.sun.aas.instanceRoot}/config``
   -  | To use the optional customized error logging and add more information 
      | to your log files, set the following:
      | ``-Djava.util.logging.config.file= ${com.sun.aas.instanceRoot} /config/logging.properties``
      | **Note**: To customize the logging, edit the ``logging.properties`` file
   -  | The default size limit for file downloads is 100MB.  To override this
      | default add the following JVM option:
      | ``-Ddvn.batchdownload.limit=<max download bytes>``

EJB Container
-----------------------------

Under Configuration->EJB Container->EJB Timer Service:

#. | Set the Timer Datasource to the following:
   | ``jdbc/VDCNetDS``
#. | Save the configuration.

HTTP Service
-----------------------------

The HTTP Service configuration settings described in this section are suggested defaults. These settings are very important. There are no right values to define; the values depend on the specifics of your web traffic, how many requests you get, how long they take to process on average, and your hardware. For detailed the 
| Sun Microsystems Documentation web site at the following URL:

`http://docs.sun.com/ <http://docs.sun.com/>`_


| **Note**: If your server becomes so busy that it drops connections,
| adjust the Thread Counts to improve performance.

#. Under Configuration->HTTP Service->HTTP
   Listeners->\ ``http-listener-1``:

   -  Listener Port: 80
   -  Acceptor Threads: The number of CPUs (cores) on your server

#. Under Configuration->HTTP Service, in the RequestProcessing tab:

   -  Thread Count: Four times the number of CPUs (cores) on your server
   -  Initial Thread Count: The number of CPUs (cores)

#. Under Configuration->HTTP Service->Virtual Servers->server: add new property ``allowLinking`` with the value ``true``.

    #. | Under Configuration->HTTP Service, configure Access Logging: 

    |              format=%client.name% %auth-user-name% %datetime% %request%        %status%
    |              %response.length%             
    |              rotation-enabled=true            
    |              rotation-interval-in-minutes=15               
    |              rotation-policy=time               
    |              rotation-suffix=yyyy-MM-dd

JavaMail Session
------------------------------------

Under Resources->JavaMail Sessions\ ``->mail/notifyMailSession:``

-  | Mail Host: ``<your mail server>``
   | **Note**: The Project recommends that you install a mail server on the same machine as GlassFish and use ``localhost`` for this entry. Since email notification is used for workflow events such as creating a dataverse or study, these functions may not work properly if a valid mail server is not configured.
-  Default User: ``dataversenotify``
    This does not need to be a real mail account.
-  Default Return Address: ``do-not-reply@<your mail server>``

JDBC Resources
------------------------------------

**Under Resources->JDBC->Connection Pools:**


| Add a new Connection Pool entry:

-  entryName: ``dvnDbPool``
-  Resource Type: ``javax.sql.DataSource``
-  Database Vendor: ``PostgreSQL``
-  DataSource ClassName: ``org.postgresql.ds.PGPoolingDataSource``
-  Additional Properties:

   -  ConnectionAttributes: ``;create=true``
   -  User: ``dvnApp``
   -  PortNumber: ``5432`` (Port 5432 is the PostgreSQL default port.)
   -  Password: ``<Dataverse Network application database password>``
   -  DatabaseName: ``<your database name>``
   -  ServerName: ``<your database host>``
   -  JDBC30DataSource: ``true``

| 

**Under Resources->JDBC->JDBC Resources:**

| Add a new JDBC Resources entry:

-  JNDI Name: ``jdbc/VDCNetDS``
-  Pool Name: ``dvnDbPool``

JMS Resources
-----------------------------------------

Under Resources->JMS Resources:

#. Add a new Connection Factory for the DSB Queue:

   -  JNDI Name: ``jms/DSBQueueConnectionFactory``
   -  Resource Type: ``javax.jms.QueueConnectionFactory``

#. Add a new Connection Factory for the Index Message:

   -  JNDI Name: ``jms/IndexMessageFactory``
   -  Resource Type: ``javax.jms.QueueConnectionFactory``

#. Add a new Destination Resource for the DSB Queue:

   -  JNDI Name: ``jms/DSBIngest``
   -  Physical Destination Name: ``DSBIngest``
   -  Resource Type: ``javax.jms.Queue``

#. Add a new Destination Resource for the Index Message:

   -  JNDI Name: ``jms/IndexMessage``
   -  Physical Destination Name: ``IndexMessage``
   -  Resource Type: ``javax.jms.Queue``

.. _postgresql-setup:

PostgreSQL setup
=======================

The following actions are normally performed by the automated installer
script. These steps are explained here for reference, and/or in case
your need to perform them manually:

1. Start as root, then change to user postgres:
   
   ``su postgres``

  Create DVN database usert (role):

  ``createuser -SrdPE [DB_USERNAME]``

  (you will be prompted to choose a user password).

  Create DVN database:

  ``createdb [DB_NAME] --owner=[DB_USERNAME]``
  
  ``[DB_NAME]`` and ``[USER_NAME]`` are the names you choose for your DVN database and database user. These, together with the password you have assigned, will be used in the Glassfish configuration so that the application can talk to the database.

2. Before Glassfish can be configured for the DVN app, the Postgres driver needs to be installed in the <GLASSFISH ROOT>/lib directory. We supply a version of the driver known to work with the DVN in the dvninstall/pgdriver directory of the Installer bundle. (This is the :ref:`"What does the Installer do?" <what-does-the-intstaller-do>` section of this appendix) An example of the installed location of the driver:

  ``/usr/local/glassfish/lib/postgresql-8.3-603.jdbc4.jar``

3. Finally, after the DVN application is deployed under Glassfish for the first time, the database needs to be populated with the initial content:

  ``su postgres``
  ``psql -d [DB_NAME] -f referenceData.sql``
  
  The file referenceData.sql is provided as part of the installer zip package.

RedHat startup file for glassfish, example
====================================================

Below is an example of a glassfish startup file that you may want to
install on your RedHat (or similar) system to have glassfish start
automatically on boot.

| Install the file as ``/etc/init.d/glassfish``, then run ``chkconfig glassfish on``

Note that the extra configuration steps before the domain start line,
for increasing the file limit and allowing "memory overcommit". These
are useful settings to have on a production server.

| You may of course add extra custom configuration specific to your
  setup.

.. code-block:: guess

	#! /bin/sh 
	# chkconfig: 2345 99 01 
	# description: GlassFish App Server 
	set -e 
	ASADMIN=/usr/local/glassfish/bin/asadmin 
	case "$1" in 
	  start) 
		echo -n "Starting GlassFish server: glassfish" 
		# Increase file descriptor limit: 
		ulimit -n 32768 
		# Allow "memory overcommit": 
		# (basically, this allows to run exec() calls from inside the 
		# app, without the Unix fork() call physically hogging 2X 
		# the amount of memory glassfish is already using) 
		echo 1 > /proc/sys/vm/overcommit_memory 
		$ASADMIN start-domain domain1 echo "." 
		;; 
	  stop) 
		echo -n "Stopping GlassFish server: glassfish" 
		$ASADMIN stop-domain domain1 
		echo "."
		 ;; 
	  *) 
		echo "Usage: /etc/init.d/glassfish {start|stop}" 

		exit 1 
	esac 
	exit 0


Enabling secure remote access to Asadmin
========================================

As was mentioned in the Glassfish section of the manual, in version
3.1.2 admin interface (asadmin) is configured to be accessible on the
localhost interface only. If you need to be able to access the admin
console remotely, you will have to enable secure access to it. (It will
be accessible over https only, at ``https://<YOUR HOST>:4848``; connections
to ``http://<YOUR HOST>:4848`` will be automatically redirected to the https
interface)

The following must be done as root:

#. First you need to configure the admin password: 

   ``<GF LOCATION>/glassfish3/bin/asadmin change-admin-password`` 

   (since you didn't create one when you were installing Glassfish, leave the "current password" blank, i.e., hit ENTER)
    
#. Enable the secure access: 

  ``<GF LOCATION>/glassfish3/bin/asadmin enable-secure-admin`` 

  (Note that you will need to restart Glassfish after step 2. above)

.. _using-lockss-with-dvn:

Using LOCKSS with DVN
=======================================

DVN holdings can be crawled by LOCKSS servers (`www.lockss.org <http://www.lockss.org>`__). It is made possible by the special plugin developed and maintained by the DVN project, which a LOCKSS daemon utilizes to crawl and access materials served by a Dataverse network.

The current stable version of the plugin is available at the following location:

`http://lockss.hmdc.harvard.edu/lockss/plugin/DVNOAIPlugin.jar <http://lockss.hmdc.harvard.edu/lockss/plugin/DVNOAIPlugin.jar>`__


As of January 2013 and DVN version 3.3, the plugin is compatible with the  LOCKSS daemon version 1.55. The plugin sources can be found in the main DVN source tree in `https://dvn.svn.sourceforge.net/svnroot/dvn/dvn-app/trunk/src/DVN-lockss <https://dvn.svn.sourceforge.net/svnroot/dvn/dvn-app/trunk/src/DVN-lockss>`_ (please note that the DVN project is currently **in the process of moving to gitHub!** The preserved copy of the 3.3 source will be left at the URL above, together with the information on the current location of the source repository).

In order to crawl a DVN, the following steps need to be performed:

#. Point your LOCKSS daemon to the plugin repository above. (Refer to the LOCKSS documentation for details);
#. Create a LOCKSS Archival Unit for your target DVN:

   In the LOCKSS Admin Console, go to **Journal Configuration** -> **Manual Add/Edit** and click on **Add Archival Unit**.

   On the next form, select **DVNOAI** in the pull down menu under **Choose a publisher plugin** and click **Continue**.

   Next configure the parameters that define your DVN Archival Unit. LOCKSS daemon can be configured to crawl either the entire holdings of a DVN (no OAI set specified), or a select Dataverse.

Note that LOCKSS crawling must be authorized on the DVN side. Refer to
the :ref:`"Edit LOCKSS Settings" <edit-lockss-harvest-settings>`
section of the DVN Network Administrator Guide for the instructions on
enabling LOCKSS crawling on the network level, and/or to the
:ref:`Enabling LOCKSS access to the Dataverse <enabling-lockss-access-to-the-dataverse>`
of the Dataverse Administration Guide. Once you allow LOCKSS crawling of
your Dataverse(s), you will need to enter the URL of the "LOCKSS
Manifest" page provided by the DVN in the configuration above. For the
network-wide archival unit this URL will be
``http``\ ``://<YOUR SERVER>/dvn/faces/ManifestPage.xhtml``; for an
individual dataverse it is
``http``\ ``://<YOUR SERVER>/dvn/dv/<DV ALIAS>/faces/ManifestPage.xhtml.``

| The URL of the DVN OAI server is ``http``\ ``://<YOUR DVN HOST>/dvn/OAIHandler``.

Read Only Mode
===================

A Read Only Mode has been established in DVN to allow the application to remain available while deploying new versions or patches.  Users will be able to view data and metadata, but will not be able to add or edit anything.  Currently there is no way to switch to Read Only Mode through the application. 
In order to change the application mode you must apply the following queries through ``psql`` or ``pgAdmin``:

To set to Read Only Mode:

      | ``BEGIN;``
      | ``SET TRANSACTION READ WRITE;``
      | ``-- Note database and user strings may have to be modified for your particular installation;``
      | ``-- You may also customize the status notice which will appear on all pages of the application;``
      | ``update vdcnetwork set statusnotice = "This network is currently in Read Only state. No saving of data will be allowed.";``
      | ``ALTER DATABASE "dvnDb" set default_transaction_read_only=on;``
      | ``Alter user "dvnApp" set default_transaction_read_only=on;``
      | ``update vdcnetwork set statusnotice = "";``
      | ``END;``

To return to regular service:

      | ``BEGIN;``
      | ``SET TRANSACTION READ WRITE;``
      | ``-- Note database and user strings may have to be modified for your particular installation;``
      | ``ALTER DATABASE "dvnDb" set default_transaction_read_only=off;``
      | ``Alter user "dvnApp" set default_transaction_read_only=off;``
      | ``update vdcnetwork set statusnotice = "";``
      | ``END;``

Backup and Restore
================================

**Backup**

| The PostgreSQL database and study files (contained within the Glassfish directory by default but this is :ref:`configurable via JVM options <jvm-options>`) are the most critical components to back up. The use of standard PostgreSQL tools (i.e. pg\_dump) is recommended.

Glassfish configuration files (i.e. domain.xml, robots.txt) and local
customizations (i.e. images in the docroot) should be backed up as well.
In practice, it is best to simply back up the entire Glassfish directory
as other files such as logs may be of interest.

| **Restore**

Restoring DVN consists of restoring the PostgreSQL database and the
Glassfish directory.
