=========
Upgrading
=========

.. contents:: |toctitle|
	:local:

When upgrading within Dataverse Software 4.x, you will need to follow the upgrade instructions for each intermediate 4.x. Similarly, when upgrading within Dataverse Software 5.x, you will need to follow the upgrade instructions for each intermediate 5.x version.

Upgrades always involve deploying the latest war file but may also include updating the schema used by Solr or other manual steps. Running database migration scripts was once required but this has been automated (see the ``flyway_schema_history`` database table to see migrations that have been run).

Please consult the release notes associated with each release at https://github.com/IQSS/dataverse/releases for more information.

Upgrading from DVN 3.x is actually a migration due to the many changes. Migration scripts have been checked into the source tree but as of this writing it is expected that people will require assistance running them. Please reach out per the :doc:`intro` section.
