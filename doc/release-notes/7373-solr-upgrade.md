### Solr Update

With this release we upgrade to the latest available stable release in the Solr 8.x branch. We recommend a fresh installation of Solr 8.8.1 (the index will be empty)
followed by an "index all".

Before you start the "index all", Dataverse will appear to be empty because
the search results come from Solr. As indexing progresses, partial results will 
appear until indexing is complete.


See http://guides.dataverse.org/installation/prerequisites.html#installing-solr

[for the additional upgrade steps section]

Run the script updateSchemaMDB.sh to generate updated solr schema files and preserve any other custom fields in your Solr configuration.
For example: (modify the path names as needed)
cd /usr/local/solr-8.8.1/server/solr/collection1/conf
wget https://github.com/IQSS/dataverse/releases/download/v5.4/updateSchemaMDB.sh
chmod +x updateSchemaMDB.sh
./updateSchemaMDB.sh -t .

See http://guides.dataverse.org/en/5.4/admin/metadatacustomization.html?highlight=updateschemamdb for more information.
