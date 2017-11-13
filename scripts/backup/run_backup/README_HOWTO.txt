The script, run_backup.py is run on schedule (by a crontab, most
likely). It will back up the files stored in your Dataverse on the
remote storage system specified. 

The backup script is written in Python. The following extra modules are required:

psycopg2    - PostgresQL driver 
boto3       - AWS sdk, for accessing S3 storage
paramiko    - SSH client, for transferring files via SFTP
swiftclient - [optional] for reading Datafiles stored on swift [not implemented yet] and/or backing up files on swift [untested, experimental (?) implementation]

Access credentials, for the Dataverse
and the remote storage system are configured in the file config.ini.

The following config.ini sections must be configured for the
whole thing to work:

1. Database. 

The script needs to be able to access the Dataverse database, in order to
obtain the lists of files that have changed since the last backup and
need to be copied. The script can use PostgresQL running on a
remote server. Just make sure that the remote server is configured to
allow connections from the host running the backup script; and that
PostgresQL is allowing database access from this host too.

Configure the access credentials as in the example below:

[Database]
Host: localhost
Port: 5432
Database: dvndb
Username: dvnapp
Password: xxxxx

In addition to the main Dataverse database, the script maintains its
own database for keeping track of the backup status of individual
files. The name of the database is specified in the following setting:

BackupDatabase: backupdb

The database must be created prior to running of the script. For
example, on the command line: 
	 createdb -U postgres backupdb --owner=dvnapp

NOTE that the current assumption is that this Postgres database lives
on the same server as the main Dataverse database and is owned by the
same user.

Also, one table must be created in this database before the script can
be run. The script backupdb.sql is provided in this directory. NOTE
that the Postgres user name dvnapp is hard-coded in the script; change
it to reflect the name of the database user on your system, if
necessary.

2. Repository

This section configures access to the datafiles stored in your
Dataverse. In its present form, the script can read files stored on
the filesystem and S3. There is no support for reading files stored
via swift as of yet. Adding swift support should be straightforward,
by supplying another storage module - similarly to the existing
storage_filesystem.py and storage_s3.py.

For the filesystem storage: the assumption is that the script has
direct access to the filesystem where the files live. Meaning that in
order for the script to work on a server that's different from the one
running the Dataverse application, the filesystem must be readable by
the server via NFS, or similarly shared with it.

The filesystem access requires the single configuration setting, as in
the example below:

[Repository]
FileSystemDirectory: /usr/local/glassfish4/glassfish/domains/domain1/files

For the S3, no configuration is needed in the config.ini. But AWS
access must be properly configured for the user running the backup
module, in the standard ~/.aws location.


3. Backup section. 

This section specifies the method for storing the files on the remote
("secondary") storage subsystem:

[Backup]
StorageType: ssh

The currently supported methods are "ssh" (the files are transferred
to the remote location via SSH/SFTP) and "swift" (untested, and
possibly incomplete implementation is provided; see
README_IMPLEMENTATION.txt for more details).

For ssh access, the following configuration entries are needed: 

SshHost: yyy.zzz.edu
SshPort: 22
SshUsername: xxxxx

Additionally, SSH access to the remote server (SshHost, above) must be
provided for the user specified (SshUsername) via ssh keys.

