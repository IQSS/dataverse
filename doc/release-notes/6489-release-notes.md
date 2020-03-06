# S3 Direct Upload support

S3 stores can now optionally be configured to support direct upload of files, as one option for supporting upload of larger files.

General information about this capability can be found in the <a href="http://guides.dataverse.org/en/latest/developers/big-data-support.html">Big Data Support Guide</a> with specific information about how to enable it in the <a href="http://guides.dataverse.org/en/latest/installation/config.html">Configuration Guide</a> - File Storage section.

**Upgrade Information:** 

Direct upload to S3 is enabled per store by one new jvm option:

    ./asadmin create-jvm-options "\-Ddataverse.files.<id>.upload-redirect=true"
    
The existing :MaxFileUploadSizeInBytes property and ```dataverse.files.<id>.url-expiration-minutes``` jvm option for the same store also apply to direct upload.

Direct upload via the Dataverse web interface is transparent to the user and handled automatically by the browser. Some minor differences in file upload exist: directly uploaded files are not unzipped and Dataverse does not scan their content to help in assigning a MIME type. Ingest of tabular files and metadata extraction from FITS files will occur, but can be turned off for files above a specified size limit through the new dataverse.files.<id>.ingestsizelimit jvm option.

API calls to support direct upload also exist, and, if direct upload is enabled for a store in Dataverse, the latest DVUploader (v1.0.8) provides a'-directupload' flag that enables its use. 
