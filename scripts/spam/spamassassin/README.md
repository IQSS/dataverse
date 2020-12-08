SpamAssassin-based spam detection script(s), HOWTO

You need to install spamassassin (and all its dependencies).
On a RedHat-like box `yum install spamassassin` will likely  do. 

Make sure SpamAssassin TextCat plugin is loaded (this is for detecting the language and similar text manipulation; it's normally not enabled in SA distributions by default), by adding `loadplugin Mail::SpamAssassin::Plugin::TextCat` to `/etc/mail/spamassassin/init.pre`.

You most likely want to enable auto-updates for SpamAssassin. See the documentation. 

Spamassassin is written in Perl, so the external script used by Dataverse is also written in perl.
You will need the following additional modules:
perl-JSON (`yum install perl-JSON`);
Text::SpamAssassin (get it here: https://metacpan.org/pod/Text::SpamAssassin).

Install the dataverse validation script (`dataverse-spam-valiator.pl`)
and the rules file (`dataverse_spam_prefs.cf`) somewhere; for example,
`/usr/local/etc`. Edit the script to specify the path of the rules files,
so that the script can find it. (Alternatively, install the rules file
as ~/.spamassassin/user_prefs, for the user who will be running the script). 

Configure external validation via the API:

`curl -X PUT -d '/usr/local/etc/dataverse-spam-valiator.pl' http://localhost:8080/api/admin/settings/:DataverseMetadataValidatorScript`

Configure the custom rejection message and/or the admin override, as needed.

