## Release Highlights

### Updates on Support for External Vocabulary Services

#### HTTP Headers

You are now able to add HTTP request headers required by the service you are implementing (#10331)

#### Flexible params in retrievalUri

You can now use `managed-fields` field names `term-uri-field` field name as parameters of `retrieval-uri`.
`{0}` default parameter is still working for a question of backward compatibility.
Also you can specify if the value must be url encoded with `encodeUrl:`. (#10404)

For example : `"retrieval-uri": "https://data.agroportal.lirmm.fr/ontologies/{keywordVocabulary}/classes/{encodeUrl:keywordTermURL}"`