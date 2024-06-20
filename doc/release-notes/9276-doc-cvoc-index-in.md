## Release Highlights

### Updates on Support for External Vocabulary Services

Multiple extensions of the External Vocabulary mechanism have been added. These extensions allow interaction with services based on the Ontoportal software and are expected to be generally useful for other service types.

These changes include:

#### Improved Indexing with Compound Fields

When using an external vocabulary service with compound fields, you can now specify which field(s) will include additional indexed information, such as translations of an entry into other languages. This is done by adding the `indexIn` in `retrieval-filtering`. (#10505)
For more information, please check [GDCC/dataverse-external-vocab-support documentation](https://github.com/gdcc/dataverse-external-vocab-support/tree/main/docs).

#### Broader Support for Indexing Service Responses

Indexing of the results from `retrieval-filtering` responses can now handle additional formats including Json Arrays of Strings and values from arbitrary keys within a JSON Object. (#10505)

**** This documentation must be merged with 9276-allow-flexible-params-in-retrievaluri-cvoc.md (#10404)