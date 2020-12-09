Consuming Configuration
=======================

.. contents:: |toctitle|
	:local:

Dataverse uses different types of configuration:

1. JVM system properties
2. Simple database value settings
3. Complex database stored data structures

1 and 2 are usually simple text strings, boolean switches or digits. All of those can be found in :doc:`/installation/config`.

Anything for 3 is configured via the API using either TSV or JSON structures. Examples are metadata blocks,
authentication providers, harvesters and others.

Simple Configuration Options
----------------------------

Developers have accessed the simple properties via

1. ``System.getProperty(...)`` for JVM system property settings
2. ``SettingsServiceBean.get(...)`` for database settings and
3. ``SystemConfig.xxx()`` for specially treated settings, maybe mixed from 1 and 2 and other sources.
4. ``SettingsWrapper``, reading from 2 and 3 for use in frontend pages based on JSF

As of Dataverse 5.3, we start to streamline our efforts into using a more consistent approach, also bringing joy and
happiness to all the system administrators out there. This will be done by adopting the use of
`MicroProfile Config <https://github.com/eclipse/microprofile-config>`_ over time.

So far we streamlined configuration of these Dataverse parts:

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

System administrators benefit from:

- Lots of database settings have been introduced in the past, but should be more easily configurable and not rely on a
  database connection.
- Running Dataverse in containers gets much easier when configuration can be provisioned in a
  streamlined fashion, mitigating the need for scripting glue and distinguishing between setting types.
- Classic installations have a profit, too: we can enable using a single config file, e.g. living in
  ``/etc/dataverse/config.properties``.
- Features for monitoring resources and others are easier to use with this streamlined configuration, as we can
  avoid people having to deal with ``asadmin`` commands and change a setting comfortably instead.

Adopting MicroProfile Config API
---------------------------------

This technology is introduced on a step-by-step basis. There will not be a big shot, crashing upgrades for everyone.
Instead, we will provide backward compatibility by deprecating renamed or moved config options, while still
supporting the old way of setting them.

- Introducing a new setting or moving and old one should result in a key ``dataverse.<scope/task/module/...>.<setting>``.
  That way we enable sys admins to recognize the meaning of an option and avoid name conflicts.
  Starting with ``dataverse`` makes it perfectly clear that this is a setting meant for this application, which is
  important when using environment variables, system properties or other MPCONFIG sources.
- Replace ``System.getProperty()`` calls with either injected configs or retrieve programmatically if more complex
  handling is necessary. If you rename the property, you should provide an alias. See below.
- Database settings need to be refactored in multiple steps. First you need to change the code retrieving it to use
  MicroProfile Config API instead (just like above). Then you should provide an alias to retain backward compatibility.
  See below.

Moving or Replacing a JVM Setting
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

When moving an old key to a new (especially when doing so with a former JVM system property setting), you should
add an alias to ``src/main/resources/META-INF/microprofile-aliases.properties`` to enable backward compatibility.
The format is always like ``dataverse.<scope/....>.newname...=old.property.name``.

Details can be found in ``edu.harvard.iq.dataverse.settings.source.AliasConfigSource``

Aliasing Database Setting
^^^^^^^^^^^^^^^^^^^^^^^^^

When moving a database setting (``:ExampleSetting``), configure an alias
``dataverse.my.example.setting=dataverse.settings.fromdb.ExampleSetting`` in
``src/main/resources/META-INF/microprofile-aliases.properties``. This will enable backward compatibility.

A database setting with an i18n attribute using *lang* will have available language codes appended to the name.
Example: ``dataverse.settings.fromdb.ExampleI18nSetting.en``, ``dataverse.settings.fromdb.ExampleI18nSetting.de``

More details in ``edu.harvard.iq.dataverse.settings.source.DbSettingConfigSource``
