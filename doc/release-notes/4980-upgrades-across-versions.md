We now offer an *EXPERIMENTAL* upgrade method allowing users to skip
over a number of releases. E.g., it should be possible now to upgrade
a Dataverse database from v4.8.6 directly to v4.10, without having to
deploy the war files for the 5 releases between these 2 versions and
manually running the corresponding database upgrade scripts.

The upgrade script, upgrade.sh is provided in the scripts/database
directory of the Dataverse source tree. See the file
README_upgrade_across_versions.txt for the instructions.