## Mitigate Solr Schema Management Problems

With [release 5.5](https://github.com/IQSS/dataverse/releases/tag/v5.5), the `<copyField>` definitions had been
reincluded into `schema.xml` to fix searching for datasets.

This release includes a final update to `schema.xml` and an updated script `update-fields.sh` to manage your
custom metadata fields in the future. (It might get used for other purposes in the future, too.) The broken script 
`updateSchemaMDB.sh` has been removed.

Please replace your schema.xml with the one provided to make sure the new script can do its magic.
If you do not use any custom metadata blocks, that's it. Else, read on.

To include your custom metadata fields after updating schema.xml, you can use a simple `curl` command. (Please download
the script before or use it from the extracted installer.)

```
curl "https://<your dataverse installation>/api/admin/index/solr/schema" | update-fields.sh conf/schema.xml
```

Please adapt the above to point to your Dataverse installation and to the correct `schema.xml` file in your Solr
installation. (See the [installation guide](https://guides.dataverse.org/en/latest/installation/prerequisites.html#installing-solr)
for some hints about usual places or use `find / -name schema.xml`)

After upgrade, you need to restart (downtime!) or reload Solr (OK while running):
```
curl "http://localhost:8983/solr/admin/cores?action=RELOAD&core=collection1"
```
(Please adapt to your installations details. Should work as is for most.)

TODO: if we change any schemas in this release, a hint about reindexing might be necessary. Else delete this TODO. 