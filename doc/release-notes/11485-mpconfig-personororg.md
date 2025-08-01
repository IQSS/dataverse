The settings `dataverse.personOrOrg.assumeCommaInPersonName` and `dataverse.personOrOrg.orgPhraseArray` now support configuration via MicroProfile Config.

They have been renamed to `dataverse.person-or-org.assume-comma-in-person-name` and `dataverse.person-or-org.org-phrase-array` for consistency with naming conventions.

In addition to the existing `asadmin` JVM option method, any [supported MicroProfile Config API source](https://docs.payara.fish/community/docs/Technical%20Documentation/MicroProfile/Config/Overview.html) can now be used to set their values.

For backwards compatibility, `dataverse.personOrOrg.assumeCommaInPersonName` is still supported. However, `dataverse.personOrOrg.orgPhraseArray` is not, due to a change in the expected value format. `dataverse.person-or-org.org-phrase-array` now expects a comma-separated list of phrases as a value instead of a JsonArray of strings. Please update both the name and value format if using the old setting.