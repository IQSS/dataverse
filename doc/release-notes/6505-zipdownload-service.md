### A multi-file, zipped download optimization

In this release we are offering an experimental optimization for the
multi-file, download-as-zip functionality. If this option is enabled,
instead of enforcing size limits, we attempt to serve all the files
that the user requested (that they are authorized to download), but
the request is redirected to a standalone zipper service running as a
cgi executable. Thus moving these potentially long-running jobs
completely outside the Application Server (Payara); and preventing
service threads from becoming locked serving them. Since zipping is
also a CPU-intensive task, it is possible to have this service running
on a different host system, thus freeing the cycles on the main
Application Server. (The system running the service needs to have
access to the database as well as to the storage filesystem, and/or S3
bucket).

Please consult the scripts/zipdownload/README.md in the Dataverse 5
source tree.

The components of the standalone "zipper tool" can also be downloaded
here:
(my plan is to build the executable and to add it to the v5
release files on github: - L.A.)
https://github.com/IQSS/dataverse/releases/download/v5.0/zipper.zip.