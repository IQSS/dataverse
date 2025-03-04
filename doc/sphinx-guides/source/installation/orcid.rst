ORCID Integration
=================

.. contents:: |toctitle|
	:local:

Introduction
------------

Dataverse leverages ORCIDs (and other types of persistent identifiers (PIDs)) to improve the findability of data and to simplify the process of adding metadata.
When ORCIDs are included as metadata about authors, Dataverse includes them in metadata exports, advertises them through :ref:`discovery-sign-posting` and via metadata embedded in dataset pages, and includes them in the metadata associated with dataset DOIs.

Dataverse can be configured to make it easier to include ORCIDs
- via use of an ORCID "External Vocabulary Script" that allows users to lookup authors, depositors, etc. based on their ORCID profile metadata and then records these ORCIDs automatically and adds links to ORCID profiles in metadata displays. With this configured, there is no need enter ORCIDs directly. See :ref:`using-external-vocabulary-services` in the Admin Guide.
- via association of ORCIDs with Dataverse user accounts, through the use of ORCID logins or, in addition or instead, a separate authenticated ORCID linking mechanism. When an ORCID is associated with a Dataverse account, it will automatically be added to the dataset metadata when a user creates a dataset and is added as an initial author.

See also :ref:`orcid-integration` in the User Guide.

Configuration
--------------

The steps needed to configure Dataverse to support lookup of ORCIDs for the author metadata field (and ROR identifiers for organizations as author affiliations) is described in the `Dataverse Author Field Example page <https://github.com/gdcc/dataverse-external-vocab-support/blob/main/examples/authorIDandAffilationUsingORCIDandROR.md>`_ in the `Dataverse External Vocabulary Suport Github Repository <https://github.com/gdcc/dataverse-external-vocab-support>`_. Briefly, this involves changing the :ref:`:CVocConf` setting and potentially creating local web-acessible copies of the relevant scripts.

To configure Dataverse to support adding ORCIDs to user profiles, one must configure ORCID as an OAuth2 provider as described in :doc:`oauth2`. The ability to link ORCIDs to user accounts is automatically enabled if an ORCID provider is configured. To avoid also enabling ORCID login, the provider can be registered with "enabled":false.

 
