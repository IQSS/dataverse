Analogue to the addition of a termURI field to the keywords in the citation metadata blocks, the same idea should also be applied to the topicClassification fields:

add a new subfield topicClassTermURI to the citation metadata block
migrate contents from topicClassVocabURI to topicClassTermURI
change DataCite export format, so that the content of topicClassTermURI goes into the property valueURI of the subject element
old:

<subject schemeURI="https://github.com/tibonto/dfgfo/44" subjectScheme="DFGFO">
Computer Science, Systems and Electrical Engineering
</subject>

 

new:

<subject schemeURI="https://github.com/tibonto/dfgfo/" valueURI="https://github.com/tibonto/dfgfo/44" subjectScheme="DFGFO">
Computer Science, Systems and Electrical Engineering
</subject>

in the general case:

 

<subject schemeURI="topicClassVocabURI" valueURI="topicClassTermURI" subjectScheme="topicClassVocab">topicClassValue</subject>


The implementation expects a new metadata field called "topicClassTermURI". The old field was called "topicClassVocabURI".
The old field still holds an URL but the schemeUrl but not the TermURI of the metadata value.
This change has been adapted to the 
    DdiExportUtil.java
    OpenAireExportUtil.java
    XmlMetadataTemplate.java

In DdiExportUtil the field "topicClassVocabURI" is overwritten with "topicClassTermURI" because this is the new vocabUri.

In OpenAireExportUtil the changes are made and tested like in the explanation above.

In XmlMetadataTemplate there is just a new getter and an addition of the new "valueURI" attribute.

Preparations before DB upgrade are:

    curl http://localhost:8080/api/admin/datasetfield/load -H "Content-type: text/tab-separated-values" -X POST --upload-file citation.tsv


You can migrate your topicClassVocabURI data containing URIs to the new topicClassTermURI field.
In case of data migration, view the affected data with the following database query:

    SELECT value FROM datasetfieldvalue dfv
    INNER JOIN datasetfield df ON df.id = dfv.datasetfield_id
    WHERE df.datasetfieldtype_id = (SELECT id FROM datasetfieldtype WHERE name = 'topicClassVocabURI')
    AND value ILIKE 'http%';


If you wish to migrate your data, a database update is then necessary:


    UPDATE datasetfield df
    SET datasetfieldtype_id  = (SELECT id FROM datasetfieldtype WHERE name = 'topicClassTermURI')
    FROM datasetfieldvalue dfv
    WHERE dfv.datasetfield_id  = df.id
    AND df.datasetfieldtype_id = (SELECT id FROM datasetfieldtype WHERE name = 'topicClassVocabURI')
    AND dfv.value ILIKE 'http%';


After db update, reindex solr with:

    curl http://localhost:8080/api/admin/index/clear && curl "http://localhost:8983/solr/admin/cores?action=RELOAD&core=collection1" && curl http://localhost:8080/api/admin/index | jq