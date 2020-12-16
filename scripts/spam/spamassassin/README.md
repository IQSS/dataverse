SpamAssassin-based spam detection script(s), HOWTO

You need to install spamassassin (and all its dependencies).
On a RedHat-like box `yum install spamassassin` will likely  do. 

Make sure SpamAssassin TextCat plugin is loaded (this is for detecting the language and similar text manipulation; it's normally not enabled in SA distributions by default), by adding `loadplugin Mail::SpamAssassin::Plugin::TextCat` to `/etc/mail/spamassassin/init.pre`.

You most likely want to enable auto-updates for SpamAssassin. See the documentation. 

Spamassassin is written in Perl, so the external script used by Dataverse is also written in perl.
You will need the following additional modules:
perl-JSON (`yum install perl-JSON`);
Text::SpamAssassin (get it here: https://metacpan.org/pod/Text::SpamAssassin).

Install the dataverse validation scripts (`dataverse-spam-validator.pl`, and the wrapper scripts `dataverse-spam-validator` and `dataset-spam-validator`) somewhere; for example,
`/usr/local/dataverse-admin/spam`. Note that the rules file (`dataverse_spam_prefs.cf`; maintained separately, outside of the main github source tree) needs to be installed in tehe same directory.
The directory above is used in the curl examples below. If installed in a different directory, adjust the commands, and edit the wrapper scripts to reflect that.

Configure external validation via the API:

For dataverses:

`curl -X PUT -d '/usr/local/dataverse-admin/spam/dataverse-spam-validator' http://localhost:8080/api/admin/settings/:DataverseMetadataValidatorScript`

and/or for datasets:

`curl -X PUT -d '/usr/local/dataverse-admin/spam/dataset-spam-validator' http://localhost:8080/api/admin/settings/:DatasetMetadataValidatorScript`

Configure custom rejection messages and/or the admin override, as needed. For example:

`curl -X PUT -d 'Uhm, this looks like spam. But nice try...' http://localhost:8080/api/admin/settings/:DataverseMetadataValidationFailureMsg`

(note that the "If you think this is in error, contact support at ..." will be automatically added to the custom message by the dataverse or dataset page).


`curl -X PUT -d true http://localhost:8080/api/admin/settings/:ExternalValidationAdminOverride`

When the override is enabled, the external check will be skipped for admin users.

The actual spam checking magic is defined in the rules file (dataverse_spam_prefs.cf). It can be tinkered with/fine-tuned by specifying what tests to perform and/or the penalities assigned for every detected violation. See the SpamAssassin documentation for how it all works. 




DataverseMetadataValidationFailureMsg,
        DatasetMetadataValidationFailureMsg,
        ExternalValidationAdminOverride


