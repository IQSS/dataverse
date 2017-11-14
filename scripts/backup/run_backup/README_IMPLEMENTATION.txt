The backup script is implemented in Python. The following extra
modules are needed:

(versions tested as of the writing of this doc, 11.14.2017)

psycopg2    [2.7.3.2] - PostgresQL driver 
boto3       [1.4.7]   - AWS sdk, for accessing S3 storage
paramiko    [2.2.1]   - SSH client, for transferring files via SFTP
swiftclient [2.7.0]   - for reading [not yet implemented] and writing swift objects. 

1. Database access.

The module uses psycopg2 to access the Dataverse database, to obtain
the lists of files that have changed since the last backup that need
to be copied over. Additionally, it maintains its own database for
keeping track of the backup status of individual files. As of now,
this extra database must reside on the same server as the main
Dataverse database and is owned by the same Postgres user.

Consult README_HOWTO.txt on how to set up this backup database (needs
to be done prior to running the backup script)

2. Storage access

Currently implemented storage access methods, for local filesystem and
S3 are isolated in the files storage_filesystem.py and storage_s3.py,
respectively. To add support for swift a similar fragment of code will
need to be provided, with an open_storage_object... method that can go
to the configured swift end node and return the byte stream associated
with the datafile. Use storage_filesystem.py as the model. Then the
top-level storage.py class will need to be modified to import and use
the extra storage method.

3. Backup (write) acces.

Similarly, storage type-specific code for writing backed up objects is
isolated in the backup_...py files. The currently implemented storage
methods are ssh/ftp (backup_ssh.py, default) and swift
(backup_swift.py; experimental, untested). To add support for other
storage systems, use backup_ssh.py as the model to create your own
backup_... classes, implementing similar methods, that a) copy the
byte stream associated with a Dataverse datafile onto this storage
system and b) verify the copy against the checksum (MD5 or SHA1)
provided by the Dataverse.  In the SSH/SFTP implementation, we can do
the verification step by simply executing md5sum/sha1sum on the remote
server via ssh, once the file is copied. With swift, the only way to
verify against the checksum is to read the file *back* from the swift
end note, and calculate the checksum on the obtained stream.

4. Keeping track of the backup status

The module uses the table datafilestatus in the "backup database" to
maintain the backup status information for the individual
datafiles. For the successfully backed up files the 'OK' status is
stored. If the module fails to read the file from the Dataverse
storage, the status 'FAIL_READ' is stored; if it fails to copy over or
verify the backup copy against the checksum, the status 'FAIL_WRITE'
is stored. The Dataverse "createdate" timestamp of the Datafile is
also stored in the database; this way, for incremental backups, the
script tries to retrieve only the Datafiles created after the latest
createdate timestamp currently in the backup db.

5. TODOs

It could be useful to  make the script automatically generate a report
of some kind, once it completes a backup run (and maybe send it to the
email address specified?). Currently, to  produce a report of how many
files have  been backed up, how  many failed, etc., you  need to query
the backup database.
