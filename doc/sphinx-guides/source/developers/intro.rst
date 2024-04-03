============
Introduction
============

Welcome! `The Dataverse Project <https://dataverse.org>`_ is an `open source <https://github.com/IQSS/dataverse/blob/master/LICENSE.md>`_ project that loves `contributors <https://github.com/IQSS/dataverse/blob/develop/CONTRIBUTING.md>`_!

.. contents:: |toctitle|
	:local:

Intended Audience
-----------------

This guide is intended primarily for developers who want to work on the main Dataverse Software code base at https://github.com/IQSS/dataverse but see "Related Projects" below for other code you can work on!

To get started, you'll want to set up your :doc:`dev-environment` and make sure you understand the branching strategy described in the :doc:`version-control` section and how to make a pull request. :doc:`testing` is expected. Opinions about :doc:`coding-style` are welcome!

.. _getting-help-developers:

Getting Help
------------

If you have any questions at all, please reach out to other developers via the channels listed in https://github.com/IQSS/dataverse/blob/develop/CONTRIBUTING.md such as https://chat.dataverse.org, the `dataverse-dev <https://groups.google.com/forum/#!forum/dataverse-dev>`_ mailing list, `community calls <https://dataverse.org/community-calls>`_, or support@dataverse.org.

.. _core-technologies:

Core Technologies
-----------------

The Dataverse Software is a `Jakarta EE <https://en.wikipedia.org/wiki/Jakarta_EE>`_ application that is compiled into a WAR file and deployed to an application server (app server) which is configured to work with a relational database (PostgreSQL) and a search engine (Solr).

We make use of a variety of Jakarta EE technologies such as JPA, JAX-RS, JMS, and JSF. The front end is built using PrimeFaces and Bootstrap.

In addition, we start to adopt parts of Eclipse MicroProfile, namely `MicroProfile Config <https://github.com/eclipse/microprofile-config>`_.

Roadmap
-------

For the Dataverse Software development roadmap, please see https://www.iq.harvard.edu/roadmap-dataverse-project

.. _kanban-board:

Kanban Board
------------

You can get a sense of what's currently in flight (in dev, in QA, etc.) by looking at https://github.com/orgs/IQSS/projects/34

Issue Tracker
-------------

We use GitHub Issues as our issue tracker: https://github.com/IQSS/dataverse/issues

Related Guides
--------------

If you are a developer who wants to make use of the Dataverse Software APIs, please see the :doc:`/api/index`. If you have front-end UI questions, please see the :doc:`/style/index`.

If you are a sysadmin who likes to code, you may be interested in hacking on installation scripts mentioned in the :doc:`/installation/index`.

If you are a Docker enthusiasts, please check out the :doc:`/container/index`.

Related Projects
----------------

As a developer, you also may be interested in these projects related to Dataverse:

- External Tools - add additional features to the Dataverse Software without modifying the core: :doc:`/api/external-tools`
- Dataverse Software API client libraries - use Dataverse Software APIs from various languages: :doc:`/api/client-libraries`
- DVUploader - a stand-alone command-line Java application that uses the Dataverse Software API to support upload of files from local disk to a Dataset: https://github.com/IQSS/dataverse-uploader 
- dataverse-sample-data - populate your Dataverse installation with sample data: https://github.com/IQSS/dataverse-sample-data
- dataverse-metrics - aggregate and visualize metrics for Dataverse installations around the world: https://github.com/IQSS/dataverse-metrics
- Configuration management scripts - Ansible, Puppet, etc.: See :ref:`advanced` section in the Installation Guide.
- :doc:`/developers/unf/index` (Java) -  a Universal Numerical Fingerprint: https://github.com/IQSS/UNF
- `DataTags <https://github.com/IQSS/DataTags>`_ (Java and Scala) - tag datasets with privacy levels: https://github.com/IQSS/DataTags
- `Matrix <https://github.com/rindataverse/matrix>`_ - a visualization showing the connectedness and collaboration between authors and their affiliations.
- Third party apps - make use of Dataverse installation APIs: :doc:`/api/apps`
- chat.dataverse.org - chat interface for Dataverse Project users and developers: https://github.com/IQSS/chat.dataverse.org
- [Your project here] :)
