Introduction
============

The script, run_backup.py is run on schedule (by a crontab, most
likely). It will back up the files stored in your Dataverse on a
remote storage system. 

As currently implemented, the script can read Dataverse files stored
either on the filesystem or S3; and back them up on a remote storage
server via ssh/scp. It can be easily expanded to support other storage
and backup types (more information is provided below).

Requirements
============

The backup script is written in Python. It was tested with Python v. 2.6 and 2.7. 
The following extra modules are required:

psycopg2    [2.7.3.2] - PostgreSQL driver 
boto3       [1.4.7]   - AWS sdk, for accessing S3 storage
paramiko    [2.2.1]   - SSH client, for transferring files via SFTP

(see below for the exact versions tested)

Also, an incomplete implementation for backing up files on a remote
swift node is provided. To fully add swift support (left as an
exercise for the reader) an additional module, swiftclient will be
needed.

Test platforms: 

MacOS 10.12
-----------

Python: 2.7.2 - part of standard distribution
paramiko: 2.2.1 - standard 
psycopg2: 2.7.3.2 - built with "pip install psycopg2"
boto3: 1.4.7 - built with "pip install boto3"

CentOS 6
--------

Python: 2.6.6 (from the base distribution for CentOS 6; default /usr/bin/python)
paramiko: 1.7.5 (base distribution)

distributed as an rpm, python-paramiko.noarch, via the yum repo "base". 
if not installed:
 yum install python-paramiko 

psycopg2: 2.0.14 (base distribution)
distributed as an rpm, python-psycopg2.x86_64, via the yum repo "base". 
if not installed:
 yum install python-psycopg2

boto3: 1.4.8 (built with "pip install boto3")

- quick and easy build; 
make sure you have pip installed. ("yum install python-pip", if not)

NOTE: v. 2.6 of Python is considered obsolete; the only reason we are
using it is that it is the default version that comes with an equally
obsolete distribution v.6 of CentOS; which just happened to be what we
had available to test this setup on.  Similarly, the versions of
paramiko and psycopg2, above, are quite old too. But everything
appears to be working.

CentOS 7:
---------

(TODO)


Usage
=====

In the default mode, the script will attempt to retrieve and back up
only the files that have been created in the Dataverse since the
createdate timestamp on the most recent file already in the backup
database; or all the files, if this is the first run (see the section
below on what the "backup databse" is).

When run with the "--rerun" option (python run_backup.py --rerun) the
script will retrieve the list of ALL the files currently in the
dataverse, but will only attempt to back up the ones not yet backed up
successfully. (i.e. it will skip the files already in the backup
database with the 'OK' backup status)


Configuration
=============

Access credentials, for the Dataverse
and the remote storage system are configured in the file config.ini.

The following config.ini sections must be configured for the
whole thing to work:

1. Database. 

The script needs to be able to access the Dataverse database, in order to
obtain the lists of files that have changed since the last backup and
need to be copied. The script can use PostgreSQL running on a
remote server. Just make sure that the remote server is configured to
allow connections from the host running the backup script; and that
PostgreSQL is allowing database access from this host too.

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

Also, one table must be created *in this database* (NOT in the main
Dataverse database) before the script can be run. The script
backupdb.sql is provided in this directory. NOTE that the Postgres
user name dvnapp is hard-coded in the script; change it to reflect the
name of the database user on your system, if necessary.

You can use the standard psql command to create the table; for example:

    	psql -d backupdb -f backupdb.sql

(please note that the example above assumes "backupdb" as the name of
the backup database)

2. Repository

This section configures access to the datafiles stored in your
Dataverse. In its present form, the script can read files stored on
the filesystem and S3. There is no support for reading files stored
via swift as of yet. Adding swift support should be straightforward,
by supplying another storage module - similarly to the existing
storage_filesystem.py and storage_s3.py. If you'd like to work on 
this, please get in touch.

For the filesystem storage: the assumption is that the script has
direct access to the filesystem where the files live. Meaning that in
order for the script to work on a server that's different from the one
running the Dataverse application, the filesystem must be readable by
the server via NFS, or similarly shared with it.

The filesystem access requires the single configuration setting, as in
the example below:

[Repository]
FileSystemDirectory: /usr/local/glassfish4/glassfish/domains/domain1/files

For S3, no configuration is needed in the config.ini. But AWS
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

4. Email notifications

Once the script completes a backup run it will send a (very minimal)
status report to the email address specified in the config.ini file;
for example:

[Notifications]
Email: xxx@yyy.zzz.edu

As currently implemented, the report will only specify how many files
have been processed, and how many succeeded or failed. In order to get
more detailed information about the individual files you'll need to
consult the datafilestatus table in the backup database.

