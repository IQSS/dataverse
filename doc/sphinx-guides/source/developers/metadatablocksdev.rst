===========================
Metadata Blocks Development
===========================

.. contents:: |toctitle|
    :local:

Introduction
------------

The idea behind Metadata Blocks in Dataverse is to have everything about the supported metadata fields configurable and customizable. Ideally, this should be accomplished by simply re-importing the updated tsv for the block via the API. In practice, when it comes to the core blocks that are distributed with Dataverse - such as the Citation and Social Science blocks - unfortunately, many dependencies exist in various parts of Dataverse, primarily import and export subsystems, on many specific fields being configured a certain way. This means that code changes may be required whenever a field from one of these core blocks is modified. 

Making a Field Multiple
-----------------------

Back in 2023, in order to accommodate specific needs of some community member institutions a few fields from Citation and Social Science were changed to support multiple values. (For example, the ``alternativeTitle`` field from the Citation block.) A number of code changes had to be made to accommodate this, plus a number of changes in the sample metadata files that are maintained in the Dataverse code tree. The checklist below is to help another developer should a similar change become necessary in the future. Note that some of the steps below may not apply 1:1 to a different metadata field, depending on how it is exported and imported in various formats by Dataverse. It may help to consult the PR `#9440 <https://github.com/IQSS/dataverse/pull/9440/files>`_ as a specific example of the changes that had to be made for the ``alternativeTitle`` field. 

- Change the value from ``FALSE`` to ``TRUE`` in the ``alowmultiples`` column of the .tsv file for the block (obviously).
- Change the value of the ``multiValued`` attribute for the search field in the Solr schema (``conf/solr/9.3.0/schema.xml`` as of writing this).
- Modify the DDI import code (``ImportDDIServiceBean.java``) to support multiple values. (you may be able to use the change in the PR above as a model.)
- Modify the DDI export utility (``DdiExportUtil.java``).
- Modify the OpenAire export utility (``OpenAireExportUtil.java``).
- Modify the following JSON source files in the Dataverse code tree to actually include multiple values for the field (two should be quite enough!): ``scripts/api/data/dataset-create-new-all-default-fields.json``, ``src/test/java/edu/harvard/iq/dataverse/export/dataset-all-defaults.txt``, ``src/test/java/edu/harvard/iq/dataverse/export/ddi/dataset-finch1.json`` and ``src/test/java/edu/harvard/iq/dataverse/export/ddi/dataset-create-new-all-ddi-fields.json``. (These are used as examples for populating datasets via the import API and by the automated import and export code tests).
- Similarly modify the following XML files that are used by the DDI export code tests: ``src/test/java/edu/harvard/iq/dataverse/export/ddi/dataset-finch1.xml`` and ``src/test/java/edu/harvard/iq/dataverse/export/ddi/exportfull.xml``.
- Make sure all the automated Unit and Integration tests are passing.
- Write a short release note to announce the change in the upcoming release.
