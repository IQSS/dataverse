The sample script register_files.sh can be used to register global ids
for legacy files (existing Dataverse datafiles that do not yet have
global ids assigned).

The global id will be assigned following the global id registration
rules configured for this Dataverse. (see the guides for more
information).

The script relies on the Dataverse registration API to obtain the
global ids. (i.e., it does not attempt to talk to your global id
provider directly!)

Run it as 

./register_files.sh FILE_CONTAINING_DATAFILE_IDs

Note that you want to run the script on the released files only. 
For the production Dataverse at IQSS Harvard we generated the 
list of the datafile ids with the following query: 

SELECT f.id
FROM filemetadata m, dvobject f, datasetversion v, dataset s, dvobject d
WHERE m.datafile_id = f.id 
   AND m.datasetversion_id = v.id 
   AND v.versionstate = 'RELEASED'
   AND v.dataset_id = s.id 
   AND s.harvestingclient_id IS null 
   AND f.identifier IS null
   AND d.id = s.id
   AND d.protocol != 'hdl'
   AND (f.identifier IS null OR f.identifier = '')

NOTE: We had reasons to exclude the datafiles in the legacy datasets with
Handlenet (d.protocol = 'hdl') identifiers. I.e., we only ran the
script above on the files from datasets with DOIs - this is our
currently configured naming authority. The script and the query above 
are provided AS EXAMPLES, FOR REFERENCE ONLY! Adjust as needed, according 
to the specific needs of your system. 
