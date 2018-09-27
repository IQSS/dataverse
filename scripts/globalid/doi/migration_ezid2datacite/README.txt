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
