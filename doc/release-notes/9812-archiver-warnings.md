# Potential Archiver Incompatibilities with Payara6
The Google Cloud and DuraCloud Archivers (see https://guides.dataverse.org/en/latest/installation/config.html#bagit-export) may not work in v6.0.
This is due to their dependence on libraries that include classes in javax.* packages that are no longer available.
If these classes are actually used when the archivers run, the archivers would fail.
As these two archivers require additional setup, they have not been tested in v6.0.
Community members using these archivers or considering their use are encouraged to test them with v6.0 and report any errors and/or provide fixes for them that can be included in future releases.

