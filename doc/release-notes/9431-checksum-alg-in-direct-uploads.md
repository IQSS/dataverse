Direct upload via the Dataverse UI will now support any algorithm configured via the :FileFixityChecksumAlgorithm setting.
External apps using the direct upload API can now query Dataverse to discover which algorithm should be used.

Sites that have been using an algorithm other than MD5 and direct upload and/or dvwebloader may want to use the /api/admin/updateHashValues call (see https://guides.dataverse.org/en/latest/installation/config.html?highlight=updatehashvalues#filefixitychecksumalgorithm) to replace any MD5 hashes on existing files.
