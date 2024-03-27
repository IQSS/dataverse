Work in progress!

to build: 

cd scripts/zipdownload
mvn clean compile assembly:single

to install: 

install cgi-bin/zipdownload and ZipDownloadService-v1.0.0.jar in your cgi-bin directory (/var/www/cgi-bin standard). 

Edit the config lines in the shell script (zipdownload) as needed. 

You may need to make extra Apache configuration changes to make sure /cgi-bin/zipdownload is accessible from the outside. 
For example, if this is the same Apache that's in front of your Dataverse Payara instance, you'll need to add another pass through statement to your configuration: 

``ProxyPassMatch ^/cgi-bin/zipdownload !``

(see the "Advanced" section of the Installation Guide for some extra troubleshooting tips)

To activate in Dataverse: 

curl -X PUT -d '/cgi-bin/zipdownload' http://localhost:8080/api/admin/settings/:CustomZipDownloadServiceUrl

How it works:
=============

(This is an ongoing design discussion - other developers are welcome to contribute)

The goal: to move this potentially long-running task out of the
Application Server. This is the sole focus of this implementation. It
does not attempt to make it faster.

The rationale here is a zipped download of a large enough number of
large enough files will always be slow. Zipping (compressing) itself
is a fairly CPU-intensive task. This will most frequently be the
bottleneck of the service. Although with a slow storage location (S3
or Swift, with a slow link to the share) it may be the speed at which
the application accesses the raw bytes. The exact location of the
bottleneck is in a sense irrelevant. On a very fast system, with the
files stored on a very fast local RAID, the bottleneck for most users
will likely shift to the speed of their internet connection to the
server. The bottom line is, downloading this multi-file compressed
stream will take a long time no matter how you slice it. So this hack
addresses it by moving the task outside Payara, where it's not going
to hog any threads. 

A quick, somewhat unrelated note: attempting to download a multi-GB
stream over http will always have its own inherent risks. If the
download has to take hours or days to complete, it is very likely that
it'll break down somewhere in the middle. Do note that for a zipped
download our users will not be able to utilize `wget --continue`, or
any similar "resume" functionality - because it's impossible to resume
generating a zipped stream from a certain offset.

The implementation is a hack. It relies on direct access to everything - storage locations (filesystem or S3) and the database.

There are no network calls between the application (Dataverse) and the zipper (an
implementation relying on such a call was discussed early
on). Dataverse issues a "job key" and sends the user's browser to the
zipper (to, for ex., /cgi-bin/zipdownload?<job key>) instead of
/api/access/datafiles/<file ids>). To authorize the zipdownload for
the "job key", and inform the zipper on which files to zip and where
to find them, the application relies on a database table, that the
zipper also has access to. In other words, there is a saved state
information associated with each zipped download request. Zipper may
be given a limited database access - for example, via a user
authorized to access that one table only. After serving the files, the
zipper removes the database entries. Job records in the database have
time stamps, so on the application side, as an added level of cleanup,
it automatically deletes any records older than 5 minutes (can be
further reduced) every time the service adds new records; as an added
level of cleanup for any records that got stuck in the db because the
corresponding zipper jobs never completed. A paranoid admin may choose
to give the zipper read-only access to the database, and rely on a
cleanup solely on the application side.

I have explored ways to avoid maintaining this state information. A
potential implementation we discussed early on, where the application
would make a network call to the zipper before redirecting the user
there, would NOT solve that problem - the state would need to somehow
be maintained on the zipper side. The only truly stateless
implementation would rely on including all the file information WITH
the redirect itself, with some pre-signed URL mechanism to make it
secure. Mechanisms for pre-signing requests are readily available and
simple to implement. We could go with something similar to how S3
presigns their access URLs. Jim Myers has already speced out how this
could be done for Dataverse access urls in a design document
(https://docs.google.com/document/d/1J8GW6zi-vSRKZdtFjLpmYJ2SUIcIkAEwHkP4q1fxL-s/edit#). (Basically,
you hash the product of your request parameters, the issue timestamp
AND some "secret" - like the user's API key - and send the resulting
hash along with the request. Tampering with any of the parameters, or
trying to extend the life span of the request, becomes impossible,
because it would invalidate the hash). What stopped me from trying
something like that was the sheer size of information that would need
to be included with a request, for a potentially long list of files
that need to be zipped. When serving a zipped download from a page
that would be doable - we could javascript together a POST call that
the browser could make to send all that info to the zipper. But if we
want to implement something similar in the API, I felt like I really
wanted to be able to simply issue a quick redirect to a manageable url
- which with the implementation above is simply
/cgi-bin/zipdownload?<job key>, with the <job key> being just a 16
character hex string in the current implementation.
