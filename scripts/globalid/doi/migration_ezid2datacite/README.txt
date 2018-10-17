This is	the migration instruction for moving a Dataverse installation using EZID to DataCite. 

The corresponding GitHub issue is https://github.com/IQSS/dataverse/issues/5024

The migration is necessary since non-University of
California Dataverse installations will no longer be able to use EZID
to mint DOIs
(https://www.cdlib.org/cdlinfo/2017/08/04/ezid-doi-service-is-evolving/).

EZID has provided a document outlining the conversion process: https://docs.google.com/document/d/1rsjFl6CvyiGaCE1SMpougZaFc135oBV_CkypoQx9x8g/edit

Key issues and steps: 

1. [This issue was most likely unique to the Harvard Dataverse only]
When the concept of the global id for a DataFile was added, Harvard
Dataverse registered DOIs for all the published files (but only for
such files that belonged to the datasets with DOIs for the global ids
of their own; files in the datasets with handles are a pending
issue). Because of a problem with the original version of the
registration API, all these DOIs were registered as "reserved",
instead of public. Reserved EZID DOIs (as explained in the document
above) CANNOT BE MIGRATED, they are only known to the EZID. So all
these file DOIs had to be changed to "public". We used a script found
in scripts/globalid/doi/ezid in this source tree.

2. You need to register with DataCite and obtain an account. The login
name and password for the account will replace the current EZID
credentials specified in the JVM options doi.username and doi.password
respectively.

Once you obtain this account, you will be able to register DOIs in the
TEST NAME SPACE ONLY (10.5072), and NOT in your currently configured
DOI space. (but read on, on how to move your current name space and
existing DOIs to the DataCite account)

3. In addition to the login name and password above, the following
configuration settings will need to be changed:

- the jvm option doi.baseurlstring needs to be changed to https://mds.datacite.org
- the database setting DoiProvider needs to be changed to DataCite

4. Presumably you will want to continue using your current registration name space and shoulder; 
(these are specified as database settings). 

The name space, and all your existing DOIs in it, MUST BE EXPLICITLY
CONVERTED, in order to move them under the "jurisdiction" of your new
DataCite account.

Note that the migration document, above, does not really specify how
to do it - it only says that they have "a process" for that. The
process is quite simple though. You contact the DataCite support (not
the EZID), give them your name space, your old EZID account name and
the new DataCite account name - and they do the rest.

Few important things:

- They will want a few days of advance notice;

- The transfer is NOT instant; it requires changing the records for
  individual DOIs, and reindexing them. For Harvard Dataverse, with
  roughly 200,000 published DOIs the time estimate was "about 6
  hours";

- Most of the DataCite staff is in Europe, so assume that this will
  happen during European business hours;

- They ask that NO NEW DOIs ARE REGISTERED during the migration
  window!  Here at Harvard we decided that bringing the entire system
  down for the duration of the transfer was an overkill. So we are
  planning to put up a site notice warning the users that they will
  not be able to publish their datasets for a few hours. And then
  blank the JVM options with the registration credentials as the
  migration is initiated. (So if any user misses the warning and still
  tries to publish, it will fail with an error message). We assume
  that everything else should still function during that window.


5. It was pointed out that the new DataCite API now supports the
notion of a "draft" DOI. Which is somewhat similar to a "reserved"
EZID DOI. We are planning to *eventually* add this to our DataCite
implementation; and create these "draft" DOIs for new, unpublished
draft datasets (similarly to how the EZID implementation creates
reserved DOIs now). For now however, we are simply letting the
reserved EZID DOIs of draft datasets go. They will still be reserved
in the Dataverse database of course; and eventually registered, when
the users publish the datasets. It would be possible to extract these
DOIs of unpublished datasets from the database, and put together a
script that would register them as draft DOIs with DataCite, using
their new API... But we are not doing this for our DOIs here.


6. The Harvard Dataverse has carried out the migration on Oct. 17.
The process was started at 9AM European time, 3AM local.  As specified
above, DataCite people requested that we don't update any existing
DOIs during the transfer window. So on the day before the migration we put 
the following announcements on our pages: 

curl -X PUT -d "Warning: Users may not be able to PUBLISH their 
datasets, between 3AM-9AM Wed. Oct. 17" \
http://localhost:8080/api/admin/settings/:StatusMessageHeader

curl -X PUT -d "Harvard Dataverse is in the process of switching from
EZID to DataCite, as the provider for registering the persistent
identifiers for Datasets and Datafiles. The migration transfer of the
authority and the existing identifiers will happen between 3AM and
approx. 9AM on Wed. Oct 17 (i.e. late tonight). During the migration
window updating existing identifiers is likely not to work
properly. So we recommend not to attempt to publish your Datasets
during the hours between 3AM and until this message disappears from
the main page." \
http://localhost:8080/api/admin/settings/:StatusMessageText


Right before the start of the migration process we changed the old
(EZID) configuration to the new (DataCite).

The Database "provider" setting: 

curl -X PUT -d "DataCite" http://localhost:8080/api/admin/settings/:DoiProvider

The JVM options: 

removed the old ones: 

asadmin delete-jvm-options "\-Ddoi.baseurlstring=https\://ezid.cdlib.org"
asadmin delete-jvm-options "\-Ddoi.username=[OUR EZID ACCOUNT USERNAME]"
asadmin delete-jvm-options "\-Ddoi.password=[OUR EZID ACCOUNT PASSWORD]"

and added the new ones: 

asadmin create-jvm-options "\-Ddoi.baseurlstring=https\://mds.datacite.org"
asadmin create-jvm-options "\-Ddoi.username=[OUR DATACITE ACCOUNT USERNAME]"
asadmin delete-jvm-options "\-Ddoi.password=PLACEHOLDER'

- NOTE THE FAKE PLACEHOLDER password - that was to keep the new configuration disabled during the migration. 

Once the prefix was transferred to DataCite and they told us that it
was safe to mint DOIs again, we re-enabled the configuration by adding
the real password to the configuration:

asadmin delete-jvm-options "\-Ddoi.password="'PLACEHOLDER'
asadmin create-jvm-options "\-Ddoi.password=[OUR DATACITE ACCOUNT PASSWORD]'

And then removed the warning messages from the page: 

curl -X DELETE http://localhost:8080/api/admin/settings/:StatusMessageText
curl -X DELETE http://localhost:8080/api/admin/settings/:StatusMessageHeader


We have a large number of public DOIs (240K as of the day of the
migration). It took about 12 hours (not 6, as originally anticipated)
to reindex them all in the DataCite database. So crude math suggests
it's about 20K DOIs/hour.

We chose to re-enable the registration setup as soon as we had heard
from DataCite that it was safe to mint new DOIs again. Even though
some of the existing DOIs were still being reindexed. That meant that,
for a few more hours, it was still possible for some user to try and
re-publish a previously published dataset and get an error; because it
would still be under the EZID authority. It didn't actually shappen,
to the best of our knowledge. And if it had, we would have simply
advised them to wait a couple of hours and try again.
