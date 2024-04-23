Consuming Configuration
=======================

.. contents:: |toctitle|
	:local:

The Dataverse Software uses different types of configuration:

1. JVM system properties
2. Simple database value settings
3. Complex database stored data structures

1 and 2 are usually simple text strings, boolean switches or digits. All of those can be found in :doc:`/installation/config`.

Anything for 3 is configured via the API using either TSV or JSON structures. Examples are metadata blocks,
authentication providers, harvesters and others.

Simple Configuration Options
----------------------------

Developers can access simple properties via:

1. ``JvmSettings.<SETTING NAME>.lookup(...)`` for JVM system property settings.
2. ``SettingsServiceBean.get(...)`` for database settings.
3. ``SystemConfig.xxx()`` for specially treated settings, maybe mixed from 1 and 2 and other sources.
4. ``SettingsWrapper`` for use in frontend JSF (xhtml) pages to obtain settings from 2 and 3. Using the wrapper is a must for performance as explained in :ref:`avoid common efficiency issues with JSF render logic expressions
   <avoid-efficiency-issues-with-render-logic-expressions>`.
5. ``System.getProperty()`` only for very special use cases not covered by ``JvmSettings``.

As of Dataverse Software 5.3, we start to streamline our efforts into using a more consistent approach, also bringing joy and
happiness to all the system administrators out there. This will be done by adopting the use of
`MicroProfile Config <https://github.com/eclipse/microprofile-config>`_ over time.

So far we streamlined configuration of these Dataverse Software parts:

- ✅ Database Connection

Complex Configuration Options
-----------------------------

We should enable variable substitution in JSON configuration. Example: using substitution to retrieve values from
MicroProfile Config and insert into the authentication provider would allow much easier provisioning of secrets
into the providers.

Why should I care about MicroProfile Config API?
------------------------------------------------

Developers benefit from:

- A streamlined API to retrieve configuration, backward-compatible renaming strategies and easier testbed configurations.
- Config API is also pushing for validation of configuration, as it's typesafe and converters for non-standard types
  can be added within our codebase.
- Defaults in code or bundled in ``META-INF/microprofile-config.properties`` allow for optional values without much hassle.
- A single place to lookup any existing JVM setting in code, easier to keep in sync with the documentation.

System administrators benefit from:

- Lots of database settings have been introduced in the past, but should be more easily configurable and not rely on a
  database connection.
- Running a Dataverse installation in containers gets much easier when configuration can be provisioned in a
  streamlined fashion, mitigating the need for scripting glue and distinguishing between setting types.
- Classic installations have a profit, too: we can enable using a single config file, e.g. living in
  ``/etc/dataverse/config.properties`` by adding our own, hot-reload config source.
- Features for monitoring resources and others are easier to use with this streamlined configuration, as we can
  avoid people having to deal with ``asadmin`` commands and change a setting with comfort instead.

Adopting MicroProfile Config API
---------------------------------

This technology is introduced on a step-by-step basis. There will not be a big shot, crashing upgrades for everyone.
Instead, we will provide backward compatibility by deprecating renamed or moved config options, while still
supporting the old way of setting them.

- Introducing a new setting or moving an old one should result in a scoped key
  ``dataverse.<scope/task/module/...>.<setting>``. That way we enable sys admins to recognize the meaning of an option
  and avoid name conflicts.
  Starting with ``dataverse`` makes it perfectly clear that this is a setting meant for this application, which is
  important when using environment variables, system properties or other MPCONFIG sources.
- Replace ``System.getProperty()`` calls with ``JvmSettings.<SETTING NAME>.lookup(...)``, adding the setting there first.
  This might be paired with renaming and providing backward-compatible aliases.
- Database settings need to be refactored in multiple steps and it is not yet clear how this will be done.
  Many Database settings are of very static nature and might be moved to JVM settings (in backward compatible ways).

Adding a JVM Setting
^^^^^^^^^^^^^^^^^^^^

Whenever a new option gets added or an existing configuration gets migrated to
``edu.harvard.iq.dataverse.settings.JvmSettings``, you will attach the setting to an existing scope or create new
sub-scopes first.

- Scopes and settings are organised in a tree-like structure within a single enum ``JvmSettings``.
- The root scope is "dataverse".
- All sub-scopes are below that.
- Scopes are separated by dots (periods).
- A scope may be a placeholder, filled with a variable during lookup. (Named object mapping.)
- The setting should be in kebab case (``signing-secret``) rather than camel case (``signingSecret``).

Any consumer of the setting can choose to use one of the fluent ``lookup()`` methods, which hides away alias handling,
conversion etc from consuming code. See also the detailed Javadoc for these methods.

Moving or Replacing a JVM Setting
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

When moving an old key to a new (especially when doing so with a former JVM system property setting), you should
add an alias to the ``JvmSettings`` definition to enable backward compatibility. Old names given there are capable of
being used with patterned lookups.

Another option is to add the alias in ``src/main/resources/META-INF/microprofile-aliases.properties``. The format is
always like ``dataverse.<scope/....>.newname...=old.property.name``. Note this doesn't provide support for patterned
aliases.

Details can be found in ``edu.harvard.iq.dataverse.settings.source.AliasConfigSource``

Adding a Feature Flag
^^^^^^^^^^^^^^^^^^^^^

Some parts of our codebase might be opt-in only. Experimental or optional feature previews can be switched on using our
usual configuration mechanism, a JVM setting.

Feature flags are implemented in the enumeration ``edu.harvard.iq.dataverse.settings.FeatureFlags``, which allows for
convenient usage of it anywhere in the codebase. When adding a flag, please add it to the enum, think of a default
status, add some Javadocs about the flagged feature and add a ``@since`` tag to make it easier to identify when a flag
has been introduced.

We want to maintain a list of all :ref:`feature flags <feature-flags>` in the :ref:`configuration guide <feature-flags>`,
please add yours to the list.