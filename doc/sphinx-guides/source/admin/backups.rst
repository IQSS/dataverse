Backups
=======

.. contents:: Contents:
	:local:

Running tape, or similar backups to ensure the long term preservation of all the data stored in the Dataverse Repository is an implied responsibility that should be taken most seriously. 

*In addition* to running these disk-level backups, we have provided an experimental script that can be run on schedule (via a cron job or something similar) to create extra archival copies of all the Datafiles stored in the Dataverse Repository on a remote storage server, accessible via an ssh connection. The script and some documentation can be found in ``scripts/backup/run_backup`` in the Dataverse Software source tree at https://github.com/IQSS/dataverse . Some degree of knowledge of system administration and Python is required. 

Once again, the script is experimental and NOT a replacement of regular and reliable disk backups!
