============
Introduction
============

Welcome! `Dataverse <http://dataverse.org>`_ is an `open source <https://github.com/IQSS/dataverse/blob/master/LICENSE.md>`_ project that loves `contributors <https://github.com/IQSS/dataverse/blob/develop/CONTRIBUTING.md>`_!

.. contents:: |toctitle|
	:local:

Intended Audience
-----------------

This guide is intended primarily for developers who want to work on the main Dataverse code base at https://github.com/IQSS/dataverse but see "Related Projects" below for other code you can work on!

To get started, you'll want to set up your :doc:`dev-environment` and make sure you understand the branching strategy described in the :doc:`version-control` section and how to make a pull request. :doc:`testing` is expected. Opinions about :doc:`coding-style` are welcome!

Getting Help
------------

If you have any questions at all, please reach out to other developers via the channels listed in https://github.com/IQSS/dataverse/blob/develop/CONTRIBUTING.md such as http://chat.dataverse.org (#dataverse on freenode), the `dataverse-dev <https://groups.google.com/forum/#!forum/dataverse-dev>`_ mailing list, `community calls <https://dataverse.org/community-calls>`_, or support@dataverse.org.

Core Technologies
-----------------

Dataverse is a `Java EE <http://en.wikipedia.org/wiki/Java_Platform,_Enterprise_Edition>`_ application that is compiled into a war file and deployed to an application server (Glassfish) which is configured to work with a relational database (PostgreSQL) and a search engine (Solr).

We make use of a variety of Java EE technologies such as JPA, JAX-RS, JMS, and JSF. The front end is built using PrimeFaces and Bootstrap.

Roadmap
-------

For the Dataverse development roadmap, please see https://dataverse.org/goals-roadmap-and-releases

Kanban Board
------------

You can get a sense of what's currently in flight (in dev, in QA, etc.) by looking at https://waffle.io/IQSS/dataverse

Issue Tracker
-------------

We use GitHub Issues as our issue tracker: https://github.com/IQSS/dataverse/issues

Related Guides
--------------

If you are a developer who wants to make use of Dataverse APIs, please see the :doc:`/api/index`. If you have front-end UI questions, please see the :doc:`/style/index`.

If you are a sysadmin who likes to code, you may be interested in hacking on installation scripts mentioned in the :doc:`/installation/index`. We validate the installation scripts with :doc:`/developers/tools` such as `Vagrant <http://vagrantup.com>`_ and Docker (see the :doc:`containers` section).

Related Projects
----------------

As a developer, you also may be interested in these projects related to Dataverse:

- External Tools - add additional features to Dataverse: See the :doc:`/installation/external-tools` section of the Installation Guide.
- Dataverse API client libraries - use Dataverse APIs from various languages: :doc:`/api/client-libraries`
- Miniverse - expose metrics from a Dataverse database: https://github.com/IQSS/miniverse
- Configuration management scripts - Ansible, Puppet, etc.: See "Advanced Installation" in the :doc:`/installation/prep` section of the Installation Guide.
- :doc:`/developers/unf/index` (Java) -  a Universal Numerical Fingerprint: https://github.com/IQSS/UNF
- GeoConnect (Python) - create a map by uploading files to Dataverse: https://github.com/IQSS/geoconnect
- `DataTags <https://github.com/IQSS/DataTags>`_ (Java and Scala) - tag datasets with privacy levels: https://github.com/IQSS/DataTags
- `TwoRavens <http://2ra.vn>`_ (Javascript) - a `d3.js <http://d3js.org>`_ interface for exploring data and running Zelig models: https://github.com/IQSS/TwoRavens
- `Zelig <http://zeligproject.org>`_ (R) - run statistical models on files uploaded to Dataverse: https://github.com/IQSS/Zelig
- Third party apps - make use of Dataverse APIs: :doc:`/api/apps`
- chat.dataverse.org - chat interface for Dataverse users and developers: https://github.com/IQSS/chat.dataverse.org
- [Your project here] :)

----

Next: :doc:`dev-environment`
