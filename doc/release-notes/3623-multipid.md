This release adds support for using multiple PID (DOI, Handle, PermalLink) providers, multiple PID provider accounts
(managing a given protocol, authority,separator, shoulder combination), assigning PID provider accounts to specific collections,
and supporting transferred PIDs (where a PID is managed by an account when it's authority, separator, and/or shoulder don't match
the combination where the account can mint new PIDs). It also adds the ability for additional provider services beyond the existing
DataCite, EZId, Handle, and PermaLink providers to be dynamically added as separate jar files.

These changes require per-provider settings rather than the global PID settings previously supported. While backward compatibility 
for installations using a single PID Provider account is provided, updating to use the new microprofile settings is highly recommended.

New microprofile settings (where * indicates a provider id indicating which provider the setting is for):

dataverse.pid.providers
dataverse.pid.default-provider
dataverse.pid.provider-implementations-directory
dataverse.pid.*.type
dataverse.pid.*.label
dataverse.pid.*.authority
dataverse.pid.*.shoulder
dataverse.pid.*.identifier-generation-style
dataverse.pid.*.datafile-pid-format
dataverse.pid.*.managed-list
dataverse.pid.*.excluded-list
dataverse.pid.*.datacite.mds-api-url
dataverse.pid.*.datacite.rest-api-url
dataverse.pid.*.datacite.username
dataverse.pid.*.datacite.password
dataverse.pid.*.ezid.api-url
dataverse.pid.*.ezid.username
dataverse.pid.*.ezid.password
dataverse.pid.*.permalink.base-url
dataverse.pid.*.permalink.separator
dataverse.pid.*.handlenet.index
dataverse.pid.*.handlenet.independent-service
dataverse.pid.*.handlenet.auth-handle
dataverse.pid.*.handlenet.key.path
dataverse.pid.*.handlenet.key.passphrase

