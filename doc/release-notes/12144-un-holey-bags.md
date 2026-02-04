This release contains multiple updates to the OAI-ORE metadata export and archival Bag output:

OAI-ORE
- now uses URI for checksum algorithms
- a bug causing failures with deaccessioned versions when the deaccession note ("Deaccession Reason" in the UI) was null (which has been allowed via the API). 
- the "https://schema.org/additionalType" is updated to "Dataverse OREMap Format v1.0.2" to indicate that the out has changed 

Archival Bag 
- for dataset versions with no files, the (empty) manifest-<alg>.txt file created will now use the default algorithm defined by the "FileFixityChecksumAlgorithm" setting rather than always defaulting to "md5"
- a bug causing the bag-info.txt to not have information on contacts when the dataset version has more than one contact has been fixed
- values used in the bag-info.txt file that may be multi-line (with embedded CR or LF characters) are now properly indented/formatted per the BagIt specification (i.e. Internal-Sender-Identifier, External-Description, Source-Organization, Organization-Address).
- the name of the dataset is no longer used as a subdirectory under the data directory (dataset names can be long enough to cause failures when unzipping)
- a new key, "Dataverse-Bag-Version" has been added to bag-info.txt with a value "1.0", allowing tracking of changes to Dataverse's arhival bag generation
- improvements to file retrieval w.r.t. retries on errors or throttling
- retrieval of files for inclusion in the bag is no longer counted as a download by Dataverse
- the size of data files and total dataset size that will be included in an archival bag can now be limited. Admins can choose whether files above these limits are transferred along with the zipped bag (creating a complete archival copy) or are just referenced (using the concept of a "holey" bag and just listing the oversized files and the Dataverse urls from which they can be retrieved. In the holey bag case, an active service on the archiving platform must retrieve the oversized files (using appropriate credentials as needed) to make a complete copy

### New JVM Options (MicroProfile Config Settings)
dataverse.bagit.zip.holey
dataverse.bagit.zip.max-data-size
dataverse.bagit.zip.max-file-size 