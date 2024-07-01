## Release Highlights

### Updates on Support for External Vocabulary Services

#### Hidden HTML Fields

External Controlled Vocabulary scripts, configured via [:CVocConf](https://guides.dataverse.org/en/6.3/installation/config.html#cvocconf), can now access the values of managed fields as well as the term-uri-field for use in constructing the metadata view for a dataset.

Those values are hidden and can be found with the html attribute `data-cvoc-metadata-name`.

For more information, see [#10503](https://github.com/IQSS/dataverse/pull/10503).
