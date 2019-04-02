===========
Preparation
===========

::

> "What are you preparing? You're always preparing! Just go!" -- Spaceballs

We'll try to get you up and running as quickly as possible, but we thought you might like to hear about your options. :)

.. contents:: |toctitle|
	:local:

Choose Your Own Installation Adventure
--------------------------------------

NDS Labs Workbench (for Testing Only)
+++++++++++++++++++++++++++++++++++++

The National Data Service (NDS) is community-driven effort guided by the National Data Service Consortium. NDS Labs has packaged Dataverse as `one of many data management tools <https://github.com/nds-org/ndslabs-specs>`_ that can be quickly deployed for evaluation purposes in their tool based on Kubernetes called NDS Labs Workbench. To get started, visit http://www.nationaldataservice.org/projects/labs.html .

Please note that the version of Dataverse in NDS Labs Workbench may lag behind the latest release. Craig Willis from NDS Labs did an excellent job of adding Dataverse 4 to NDS Labs Workbench and the Dataverse team hopes to some day take over the creation of Docker images so the latest version of Dataverse can be evaluated in the workbench.

Vagrant (for Testing Only)
++++++++++++++++++++++++++

If you are looking to simply kick the tires on installing Dataverse and are familiar with Vagrant, you are welcome to read through the "Vagrant" section of the :doc:`/developers/tools` section of the Developer Guide. Checking out a tagged release is recommended rather than running ``vagrant up`` on unreleased code.

Pilot Installation
++++++++++++++++++

Vagrant is not a bad way for a sysadmin to get a quick sense of how an application like Dataverse is put together in a sandbox (a virtual machine running on a laptop for example), but to allow end users to start playing with Dataverse, you'll need to install Dataverse on a server.

Installing Dataverse involves some system configuration followed by executing an installation script that will guide you through the installation process as described in :doc:`installation-main`, but reading about the :ref:`architecture` of Dataverse is recommended first.

.. _advanced:

Advanced Installation
+++++++++++++++++++++

There are some community-lead projects to use configuration management tools such as Ansible and Puppet to automate Dataverse installation and configuration, but support for these solutions is limited to what the Dataverse community can offer as described in each project's webpage:

- https://github.com/IQSS/dataverse-ansible
- https://github.com/IQSS/dataverse-puppet

(Please note that the "dataverse-ansible" repo is used in a script that allows Dataverse to be installed on Amazon Web Services (AWS) from arbitrary GitHub branches as described in the :doc:`/developers/deployment` section of the Developer Guide.)

The Dataverse development team is happy to "bless" additional community efforts along these lines (i.e. Docker, Chef, Salt, etc.) by creating a repo under https://github.com/IQSS and managing team access.

Dataverse permits a fair amount of flexibility in where you choose to install the various components. The diagram below shows a load balancer, multiple proxies and web servers, redundant database servers, and offloading of potentially resource intensive work to a separate server. 

|3webservers|

A setup such as this is advanced enough to be considered out of scope for this guide (apart from a stub in the :doc:`advanced` section) but you are welcome to ask questions about similar configurations via the support channels listed in the :doc:`intro`.

.. _architecture:

Architecture and Components
---------------------------

Dataverse is a Java Enterprise Edition (EE) web application that is shipped as a war (web archive) file.

Required Components
+++++++++++++++++++

When planning your installation you should be aware of the following components of the Dataverse architecture:

- Linux: RHEL/CentOS is highly recommended since all development and QA happens on this distribution.
- Glassfish: a Java EE application server to which the Dataverse application (war file) is to be deployed.
- PostgreSQL: a relational database.
- Solr: a search engine. A Dataverse-specific schema is provided.
- SMTP server: for sending mail for password resets and other notifications.
- Persistent identifier service: DOI and Handle support are provided. Production use requires a registered DOI or Handle.net authority.

Optional Components
+++++++++++++++++++

There are a number of optional components you may choose to install or configure, including:

- R, rApache, Zelig, and TwoRavens: :doc:`/user/data-exploration/tworavens` describes the feature and :doc:`r-rapache-tworavens` describes how to install these components. :doc:`external-tools` explains how third-party tools like TwoRavens can be added to Dataverse.
- Dropbox integration :ref:`dataverse.dropbox.key`: for uploading files from the Dropbox API.
- Apache: a web server that can "reverse proxy" Glassfish applications and rewrite HTTP traffic.
- Shibboleth: an authentication system described in :doc:`shibboleth`. Its use with Dataverse requires Apache.
- OAuth2: an authentication system described in :doc:`oauth2`.
- Geoconnect: a system that allows users to create maps from geospatial files, described in :doc:`geoconnect`.

See also the :doc:`/admin/integrations` section of the Admin Guide.

System Requirements
-------------------

Hardware Requirements
+++++++++++++++++++++

A basic installation of Dataverse runs fine on modest hardware. For example, as of this writing the test installation at http://phoenix.dataverse.org is backed by a single virtual machine with two 2.8 GHz processors, 8 GB of RAM and 50 GB of disk.

In contrast, before we moved it to the Amazon Cloud, the production installation at https://dataverse.harvard.edu was backed by six servers with two Intel Xeon 2.53 Ghz CPUs and either 48 or 64 GB of RAM. The three servers with 48 GB of RAM run were web frontends running Glassfish and Apache and were load balanced by a hardware device. The remaining three servers with 64 GB of RAM were the primary and backup database servers and a server dedicated to running Rserve. Multiple TB of storage were mounted from a SAN via NFS.

Currently, Harvard Dataverse is served by four AWS server nodes: two "m4.4xlarge" instances (64GB/16 vCPU) as web frontends, one 32GB/8 vCPU ("m4.2xlarge") instance for the Solr search engine, and one 16GB/4 vCPU ("m4.xlarge") instance for R and TwoRavens. The PostgreSQL database is served by Amazon RDS, and physical files are stored on Amazon S3.

The Dataverse installation script will attempt to give Glassfish the right amount of RAM based on your system.

Experimentation and testing with various hardware configurations is encouraged, or course, but do reach out as explained in the :doc:`intro` as needed for assistance.

Software Requirements
+++++++++++++++++++++

See :ref:`architecture` for an overview of required and optional components. The :doc:`prerequisites` section is oriented toward installing the software necessary to successfully run the Dataverse installation script. Pages on optional components contain more detail of software requirements for each component.

Clients are expected to be running a relatively modern browser.

Decisions to Make
-----------------

Here are some questions to keep in the back of your mind as you test and move into production:

- How much storage do I need?
- Which features do I want based on :ref:`architecture`?
- How do I want my users to log in to Dataverse? With local accounts? With Shibboleth/SAML? With OAuth providers such as ORCID, GitHub, or Google?
- Do I want to to run Glassfish on the standard web ports (80 and 443) or do I want to "front" Glassfish with a proxy such as Apache or nginx? See "Network Ports" in the :doc:`config` section.
- How many points of failure am I willing to tolerate? How much complexity do I want?
- How much does it cost to subscribe to a service to create persistent identifiers such as DOIs or handles?

Next Steps
----------

Proceed to the :doc:`prerequisites` section which will help you get ready to run the Dataverse installation script.

.. |3webservers| image:: ./img/3webservers.png
   :class: img-responsive
