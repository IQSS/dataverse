# Multiple Store Support
Dataverse can now be configured to store files in more than one place at the same time (multiple file, s3, and/or swift stores). 

General information about this capability can be found in the <a href="http://guides.dataverse.org/en/latest/installation/config.html">Configuration Guide</a> - File Storage section.

**Upgrade Information:** 

**Existing installations will need to make configuration changes to adopt this version, regardless of whether additional stores are to be added or not.**

Multistore support requires that each store be assigned a label, id, and type - see the documentation for a more complete explanation. For an existing store, the recommended upgrade path is to assign the store id based on it's type, i.e. a 'file' store would get id 'file', an 's3' store would have the id 's3'. 

With this choice, no changes to datafile 'storageidentifier' entries are needed in the database. (If you do not name your existing store using this convention, you will need to edit the database to maintain access to existing files!).

The following set of commands to change the Glassfish JVM options will adapt an existing file or s3 store for this upgrade:
For a file store:

    ./asadmin create-jvm-options "\-Ddataverse.files.file.type=file"
    ./asadmin create-jvm-options "\-Ddataverse.files.file.label=file"
    ./asadmin create-jvm-options "\-Ddataverse.files.file.directory=<your directory>"
    
For an s3 store:

    ./asadmin create-jvm-options "\-Ddataverse.files.s3.type=s3"
    ./asadmin create-jvm-options "\-Ddataverse.files.s3.label=s3"
    ./asadmin delete-jvm-options "-Ddataverse.files.s3-bucket-name=<your_bucket_name>"
    ./asadmin create-jvm-options "-Ddataverse.files.s3.bucket-name=<your_bucket_name>"
    
Any additional S3 options you have set will need to be replaced as well, following the pattern in the last two lines above - delete the option including a '-' after 's3' and creating the same option with the '-' replaced by a '.', using the same value you currently have configured.  

Once these options are set, restarting the glassfish service is all that is needed to complete the change.  

Note that the "\-Ddataverse.files.directory", if defined, continues to control where temporary files are stored (in the /temp subdir of that directory), independent of the location of any 'file' store defined above.
