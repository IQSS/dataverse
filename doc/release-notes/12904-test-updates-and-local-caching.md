## Local Caching of xsd and json-ld contexts

Dataverse now has local copies of XML schema files and JSON-LD contexts, which are used in validation of DataCite XML and Linked Data Notification messages. This avoids issues that have been seen when the files were temporarily not available from the original URLs.