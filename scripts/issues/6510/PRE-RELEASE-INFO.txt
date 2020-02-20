We recently discovered that *potential* data integrity issues in
Dataverse databases.  One manifests itself as duplicate DataFile
objects created for the same uploaded file; the other as duplicate
DataTable (tabular metadata) objects linked to the same
DataFile. (GitHub issues https://github.com/IQSS/dataverse/issues/6522
and https://github.com/IQSS/dataverse/issues/6510 respectively).

Please run the diagnostic script provided at
https://github.com/IQSS/dataverse/raw/6510-repeated-ingests/scripts/issues/6510/check_datafiles_6522_6510.sh
[NOTE!! the branch name must be changed to "develop" in the URL above before we merge!!]

The script relies on the PostgreSQL utility psql to access the
database. You will need to edit the credentials at the top of the script
to match your database configuration.

If neither of the two issues is present in your database, you will see
a message "... no duplicate DataFile objects in your database" and "no
tabular files affected by this issue in your database".

If either, or both kinds of duplicates are detected, the script will
provide further instructions. We will need you to send us the produced
output. We will then assist you in resolving the issues in your
database.

