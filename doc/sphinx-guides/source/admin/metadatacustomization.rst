Metadata Customization
======================

The Dataverse Software has a flexible data-driven metadata system powered by "metadata blocks" that are listed in the :doc:`/user/appendix` section of the User Guide. In this section we explain the customization options.

.. contents:: |toctitle|
	:local:

Introduction
------------

Before you embark on customizing metadata in your Dataverse installation you should make sure you are aware of the modest amount of customization that is available with your Dataverse installation's web interface. It's possible to hide fields and make fields required or conditionally required by clicking "Edit" at the Dataverse collection level, clicking "General Information" and making adjustments under "Metadata Fields" as described in the :ref:`create-dataverse` section of the Dataverse Collection Management page in the User Guide.

Much more customization of metadata is possible, but this is an advanced topic so feedback on what is written below is very welcome. The possibilities for customization include:

-  Editing and adding metadata fields

-  Editing and adding instructional text (field label tooltips and text box watermarks)

-  Editing and adding controlled vocabularies

-  Changing which fields depositors must use in order to save datasets (see also :ref:`dataset-templates` section of the User Guide.)

-  Changing how saved metadata values are displayed in the UI

Generally speaking it is safer to create your own custom metadata block rather than editing metadata blocks that ship with the Dataverse Software, because changes to these blocks may be made in future releases. If you'd like to make improvements to any of the metadata blocks shipped with the  Dataverse Software, please open an issue at https://github.com/IQSS/dataverse/issues so it can be discussed before a pull request is made. Please note that the metadata blocks shipped with the Dataverse Software are based on standards (e.g. DDI for social science) and you can learn more about these standards in the :doc:`/user/appendix` section of the User Guide. If you have developed your own custom metadata block that you think may be of interest to the Dataverse community, please create an issue and consider making a pull request as described in the :doc:`/developers/version-control` section of the Developer Guide.

In current versions of the Dataverse Software, custom metadata are no longer defined as individual
fields, as they were in Dataverse Network (DVN) 3.x, but in metadata blocks.
The Dataverse Software now ships with a citation metadata block, which includes
mandatory fields needed for assigning persistent IDs to datasets, and
domain specific metadata blocks. For a complete list, see the
:doc:`/user/appendix` section of the User Guide.

Definitions of these blocks are loaded into a Dataverse installation in
tab-separated value (TSV). [1]_\ :sup:`,`\  [2]_ While it is technically
possible to define more than one metadata block in a TSV file, it is
good organizational practice to define only one in each file.

The metadata block TSVs shipped with the Dataverse Software are in `/scripts/api/data/metadatablocks
<https://github.com/IQSS/dataverse/tree/develop/scripts/api/data/metadatablocks>`__ with the corresponding ResourceBundle property files in `/src/main/java/propertyFiles <https://github.com/IQSS/dataverse/tree/develop/src/main/java/propertyFiles>`__ of the Dataverse Software GitHub repo. Human-readable copies are available in `this Google Sheets
document <https://docs.google.com/spreadsheets/d/13HP-jI_cwLDHBetn9UKTREPJ_F4iHdAvhjmlvmYdSSw/edit#gid=0>`__ but they tend to get out of sync with the TSV files, which should be considered authoritative. The Dataverse Software installation process operates on the TSVs, not the Google spreadsheet.

About the metadata block TSV
----------------------------

Here we list and describe the purposes of each section and property of
the metadata block TSV.

1. metadataBlock

   -  Purpose: Represents the metadata block being defined.

   -  Cardinality:

      -  0 or more per Dataverse installation

      -  1 per Metadata Block definition

2. datasetField

   -  Purpose: Each entry represents a metadata field to be defined
      within a metadata block.

   -  Cardinality: 1 or more per metadataBlock

3. controlledVocabulary

   -  Purpose: Each entry enumerates an allowed value for a given
      datasetField.

   -  Cardinality: zero or more per datasetField

Each of the three main sections own sets of properties:

#metadataBlock properties
~~~~~~~~~~~~~~~~~~~~~~~~~

+----------------+---------------------------------------------------------+---------------------------------------------------------+
| **Property**   | **Purpose**                                             | **Allowed values and restrictions**                     |
+----------------+---------------------------------------------------------+---------------------------------------------------------+
| name           | A user-definable string used to identify a              | \• No spaces or punctuation, except underscore.         |
|                | #metadataBlock                                          |                                                         |
|                |                                                         | \• By convention, should start with a letter, and use   |
|                |                                                         | lower camel case [3]_                                   |
|                |                                                         |                                                         |
|                |                                                         | \• Must not collide with a field of the same name in    |
|                |                                                         | the same or any other #datasetField definition,         |
|                |                                                         | including metadata blocks defined elsewhere. [4]_       |
+----------------+---------------------------------------------------------+---------------------------------------------------------+
| dataverseAlias | If specified, this metadata block will be available     | Free text. For an example, see custom_hbgdki.tsv.       |
|                | only to the Dataverse collection designated here by     |                                                         |
|                | its alias and to children of that Dataverse collection. |                                                         |
+----------------+---------------------------------------------------------+---------------------------------------------------------+
| displayName    | Acts as a brief label for display related to this       | Should be relatively brief. The limit is 256 character, |
|                | #metadataBlock.                                         | but very long names might cause display problems.       |
+----------------+---------------------------------------------------------+---------------------------------------------------------+
| displayFacet   | Label displayed in the search area when this            | Should be brief. Long names will cause display problems |
|                | #metadataBlock is configured as a search facet          | in the search area.                                     |
|                | for a collection. See                                   |                                                         |
|                | :ref:`the API <metadata-block-facet-api>`.              |                                                         |
+----------------+---------------------------------------------------------+---------------------------------------------------------+
| blockURI       | Associates the properties in a block with an external   | The citation #metadataBlock has the blockURI            |
|                | URI.                                                    | https://dataverse.org/schema/citation/ which assigns a  |
|                | Properties will be assigned the                         | default global URI to terms such as                     |
|                | global identifier blockURI<name> in the OAI_ORE         | https://dataverse.org/schema/citation/subtitle          |
|                | metadata and archival Bags                              |                                                         |
+----------------+---------------------------------------------------------+---------------------------------------------------------+

#datasetField (field) properties
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

+---------------------------+--------------------------------------------------------+----------------------------------------------------------+-----------------------+
| **Property**              | **Purpose**                                            | **Allowed values and restrictions**                      |                       |
+---------------------------+--------------------------------------------------------+----------------------------------------------------------+-----------------------+
| name                      | A user-definable string used to identify a             | \• (from DatasetFieldType.java) The internal DDI-like    |                       |
|                           | #datasetField. Maps directly to field name used by     | name, no spaces, etc.                                    |                       |
|                           | Solr.                                                  |                                                          |                       |
|                           |                                                        | \• (from Solr) Field names should consist of             |                       |
|                           |                                                        | alphanumeric or underscore characters only and not start |                       |
|                           |                                                        | with a digit. This is not currently strictly enforced,   |                       |
|                           |                                                        | but other field names will not have first class          |                       |
|                           |                                                        | support from all components and back compatibility       |                       |
|                           |                                                        | is not guaranteed.                                       |                       |
|                           |                                                        | Names with both leading and trailing underscores         |                       |
|                           |                                                        | (e.g. \_version_) are reserved.                          |                       |
|                           |                                                        |                                                          |                       |
|                           |                                                        | \• Must not collide with a field of                      |                       |
|                           |                                                        | the same same name in another #metadataBlock             |                       |
|                           |                                                        | definition or any name already included as a             |                       |
|                           |                                                        | field in the Solr index.                                 |                       |
+---------------------------+--------------------------------------------------------+----------------------------------------------------------+-----------------------+
| title                     | Acts as a brief label for display                      | Should be relatively brief.                              |                       |
|                           | related to this #datasetField.                         |                                                          |                       |
+---------------------------+--------------------------------------------------------+----------------------------------------------------------+-----------------------+
| description               | Used to provide a description of the                   | Free text                                                |                       |
|                           | field.                                                 |                                                          |                       |
+---------------------------+--------------------------------------------------------+----------------------------------------------------------+-----------------------+
| watermark                 | A string to initially display in a field               | Free text                                                |                       |
|                           | as a prompt for what the user should enter.            |                                                          |                       |
+---------------------------+--------------------------------------------------------+----------------------------------------------------------+-----------------------+
| fieldType                 | Defines the type of content that the                   |                                                          | \• none               |
|                           | field, if not empty, is meant to contain.              |                                                          | \• date               |
|                           |                                                        |                                                          | \• email              |
|                           |                                                        |                                                          | \• text               |
|                           |                                                        |                                                          | \• textbox            |
|                           |                                                        |                                                          | \• url                |
|                           |                                                        |                                                          | \• int                |
|                           |                                                        |                                                          | \• float              |
|                           |                                                        |                                                          | \• See below for      |
|                           |                                                        |                                                          | fieldtype definitions |
+---------------------------+--------------------------------------------------------+----------------------------------------------------------+-----------------------+
| displayOrder              | Controls the sequence in which the fields              | Non-negative integer.                                    |                       |
|                           | are displayed, both for input and                      |                                                          |                       |
|                           | presentation.                                          |                                                          |                       |
+---------------------------+--------------------------------------------------------+----------------------------------------------------------+-----------------------+
| displayFormat             | Controls how the content is displayed                  | See below for displayFormat                              |                       |
|                           | for presentation (not entry). The value of             | variables                                                |                       |
|                           | this field may contain one or more                     |                                                          |                       |
|                           | special variables (enumerated below).                  |                                                          |                       |
|                           | HTML tags, likely in conjunction with one              |                                                          |                       |
|                           | or more of these values, may be used                   |                                                          |                       |
|                           | to control the display of content in                   |                                                          |                       |
|                           | the web UI.                                            |                                                          |                       |
+---------------------------+--------------------------------------------------------+----------------------------------------------------------+-----------------------+
| advancedSearchField       | Specify whether this field is available in             | TRUE (available) or                                      |                       |
|                           | advanced search.                                       | FALSE (not available)                                    |                       |
+---------------------------+--------------------------------------------------------+----------------------------------------------------------+-----------------------+
| allowControlledVocabulary | Specify whether the possible values of                 | TRUE (controlled) or FALSE (not                          |                       |
|                           | this field are determined by values                    | controlled)                                              |                       |
|                           | in the #controlledVocabulary section.                  |                                                          |                       |
+---------------------------+--------------------------------------------------------+----------------------------------------------------------+-----------------------+
| allowmultiples            | Specify whether this field is repeatable.              | TRUE (repeatable) or FALSE (not                          |                       |
|                           |                                                        | repeatable)                                              |                       |
+---------------------------+--------------------------------------------------------+----------------------------------------------------------+-----------------------+
| facetable                 | Specify whether the field is facetable                 | TRUE (controlled) or FALSE (not                          |                       |
|                           | (i.e., if the expected values for                      | controlled)                                              |                       |
|                           | this field are themselves useful                       |                                                          |                       |
|                           | search terms for this field). If a field is            |                                                          |                       |
|                           | "facetable" (able to be faceted on), it                |                                                          |                       |
|                           | appears under "Browse/Search                           |                                                          |                       |
|                           | Facets" when you edit                                  |                                                          |                       |
|                           | "General Information" for a Dataverse                  |                                                          |                       |
|                           | collection.                                            |                                                          |                       |
|                           | Setting this value to TRUE generally makes             |                                                          |                       |
|                           | sense for enumerated or controlled                     |                                                          |                       |
|                           | vocabulary fields, fields representing                 |                                                          |                       |
|                           | identifiers (IDs, names, email                         |                                                          |                       |
|                           | addresses), and other fields that are                  |                                                          |                       |
|                           | likely to share values across                          |                                                          |                       |
|                           | entries. It is less likely to make sense               |                                                          |                       |
|                           | for fields containing descriptions,                    |                                                          |                       |
|                           | floating point numbers, and other                      |                                                          |                       |
|                           | values that are likely to be unique.                   |                                                          |                       |
+---------------------------+--------------------------------------------------------+----------------------------------------------------------+-----------------------+
| displayoncreate [5]_      | Designate fields that should display during            | TRUE (display during creation) or FALSE                  |                       |
|                           | the creation of a new dataset, even before             | (don’t display during creation)                          |                       |
|                           | the dataset is saved.                                  |                                                          |                       |
|                           | Fields not so designated will not                      |                                                          |                       |
|                           | be displayed until the dataset has been                |                                                          |                       |
|                           | saved.                                                 |                                                          |                       |
+---------------------------+--------------------------------------------------------+----------------------------------------------------------+-----------------------+
| required                  | For primitive fields, specify whether or not the       | For primitive fields, TRUE                               |                       |
|                           | field is required.                                     | (required) or FALSE (optional).                          |                       |
|                           |                                                        |                                                          |                       |
|                           | For compound fields, also specify if one or more       | For compound fields:                                     |                       |
|                           | subfields are required or conditionally required. At   |                                                          |                       |
|                           | least one instance of a required field must be         | \• To make one or more                                   |                       |
|                           | present. More than one instance of a field may be      | subfields optional, the parent                           |                       |
|                           | allowed, depending on the value of allowmultiples.     | field and subfield(s) must be                            |                       |
|                           |                                                        | FALSE (optional).                                        |                       |
|                           |                                                        |                                                          |                       |
|                           |                                                        | \• To make one or more subfields                         |                       |
|                           |                                                        | required, the parent field and                           |                       |
|                           |                                                        | the required subfield(s) must be                         |                       |
|                           |                                                        | TRUE (required).                                         |                       |
|                           |                                                        |                                                          |                       |
|                           |                                                        | \• To make one or more subfields                         |                       |
|                           |                                                        | conditionally required, make the                         |                       |
|                           |                                                        | parent field FALSE (optional)                            |                       |
|                           |                                                        | and make TRUE (required) any                             |                       |
|                           |                                                        | subfield or subfields that are                           |                       |
|                           |                                                        | required if any other subfields                          |                       |
|                           |                                                        | are filled.                                              |                       |
+---------------------------+--------------------------------------------------------+----------------------------------------------------------+-----------------------+
| parent                    | For subfields, specify the name of the parent or       | \• Must not result in a cyclical reference.              |                       |
|                           | containing field.                                      |                                                          |                       |
|                           |                                                        | \• Must reference an existing field in the same          |                       |
|                           |                                                        | #metadataBlock.                                          |                       |
+---------------------------+--------------------------------------------------------+----------------------------------------------------------+-----------------------+
| metadatablock_id          | Specify the name of the #metadataBlock that contains   | \• Must reference an existing #metadataBlock.            |                       |
|                           | this field.                                            |                                                          |                       |
|                           |                                                        | \• As a best practice, the value should reference the    |                       |
|                           |                                                        | #metadataBlock in the current                            |                       |
|                           |                                                        | definition (it is technically                            |                       |
|                           |                                                        | possible to reference another                            |                       |
|                           |                                                        | existing metadata block.)                                |                       |
+---------------------------+--------------------------------------------------------+----------------------------------------------------------+-----------------------+
| termURI                   | Specify a global URI identifying this term in an       | For example, the existing citation                       |                       |
|                           | external community vocabulary.                         | #metadataBlock defines the property                      |                       |
|                           |                                                        | named 'title' as http://purl.org/dc/terms/title          |                       |
|                           | This value overrides the default (created by appending | - i.e. indicating that it can                            |                       |
|                           | the property name to the blockURI defined for the      | be interpreted as the Dublin Core term 'title'           |                       |
|                           | #metadataBlock)                                        |                                                          |                       |
+---------------------------+--------------------------------------------------------+----------------------------------------------------------+-----------------------+

.. _cvoc-props:

#controlledVocabulary (enumerated) properties
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

+--------------+--------------------------------------------+-----------------------------------------+
| **Property** | **Purpose**                                | **Allowed values and restrictions**     |
+--------------+--------------------------------------------+-----------------------------------------+
| DatasetField | Specifies the #datasetField to which       | Must reference an existing              |
|              | #datasetField to which this entry applies. | #datasetField.                          |
|              |                                            | As a best practice, the value should    |
|              |                                            | reference a #datasetField in the        |
|              |                                            | current metadata  block definition. (It |
|              |                                            | is technically possible to reference    |
|              |                                            | an existing #datasetField from          |
|              |                                            | another metadata block.)                |
+--------------+--------------------------------------------+-----------------------------------------+
| Value        | A short display string, representing       | Free text. When defining a boolean, the |
|              | an enumerated value for this field. If     | values "True" and "False" are           |
|              | the identifier property is empty,          | recommended and "Unknown" can be added  |
|              | this value is used as the identifier.      | if needed.                              |
+--------------+--------------------------------------------+-----------------------------------------+
| identifier   | A string used to encode the selected       | Free text                               |
|              | enumerated value of a field. If this       |                                         |
|              | property is empty, the value of the        |                                         |
|              | “Value” field is used as the identifier.   |                                         |
+--------------+--------------------------------------------+-----------------------------------------+
| displayOrder | Control the order in which the enumerated  | Non-negative integer.                   |
|              | values are displayed for selection. When   |                                         |
|              | adding new values, you don't have to add   |                                         |
|              | them at the end. You can renumber existing |                                         |
|              | values to update the order in which they   |                                         |
|              | appear.                                    |                                         |
+--------------+--------------------------------------------+-----------------------------------------+

FieldType definitions
~~~~~~~~~~~~~~~~~~~~~

+---------------+------------------------------------+
| **Fieldtype** | **Definition**                     |
+---------------+------------------------------------+
| none          | Used for compound fields, in which |
|               | case the parent field would have   |
|               | no value and display no data       |
|               | entry control.                     |
+---------------+------------------------------------+
| date          | A date, expressed in one of three  |
|               | resolutions of the form            |
|               | YYYY-MM-DD, YYYY-MM, or YYYY.      |
+---------------+------------------------------------+
| email         | A valid email address. Not         |
|               | indexed for privacy reasons.       |
+---------------+------------------------------------+
| text          | Any text other than newlines may   |
|               | be entered into this field.        |
|               | The text fieldtype may be used to  |
|               | define a boolean (see "Value"      |
|               | under :ref:`cvoc-props`).          |
+---------------+------------------------------------+
| textbox       | Any text may be entered. For       |
|               | input, the Dataverse Software      |
|               | presents a                         |
|               | multi-line area that accepts       |
|               | newlines. While any HTML is        |
|               | permitted, only a subset of HTML   |
|               | tags will be rendered in the UI.   |
|               | See the                            |
|               | :ref:`supported-html-fields`       |
|               | section of the Dataset + File      |
|               | Management page in the User Guide. |
+---------------+------------------------------------+
| url           | If not empty, field must contain   |
|               | a valid URL.                       |
+---------------+------------------------------------+
| int           | An integer value destined for a    |
|               | numeric field.                     |
+---------------+------------------------------------+
| float         | A floating point number destined   |
|               | for a numeric field.               |
+---------------+------------------------------------+

displayFormat variables
~~~~~~~~~~~~~~~~~~~~~~~

These are common ways to use the displayFormat to control how values are displayed in the UI. This list is not exhaustive.

+---------------------------------+--------------------------------------------------------+
| **Variable**                    | **Description**                                        |
+---------------------------------+--------------------------------------------------------+
| (blank)                         | The displayFormat is left blank                        |
|                                 | for primitive fields (e.g.                             |
|                                 | subtitle) and fields that do not                       |
|                                 | take values (e.g. author), since                       |
|                                 | displayFormats do not work for                         |
|                                 | these fields.                                          |
+---------------------------------+--------------------------------------------------------+
| #VALUE                          | The value of the field (instance level).               |
+---------------------------------+--------------------------------------------------------+
| #NAME                           | The name of the field (class level).                   |
+---------------------------------+--------------------------------------------------------+
| #EMAIL                          | For displaying emails.                                 |
+---------------------------------+--------------------------------------------------------+
| <a href="#VALUE">#VALUE</a>     | For displaying the value as a                          |
|                                 | link (if the value entered is a                        |
|                                 | link).                                                 |
+---------------------------------+--------------------------------------------------------+
| <a href='URL/#VALUE'>#VALUE</a> | For displaying the value as a                          |
|                                 | link, with the value included in                       |
|                                 | the URL (e.g. if URL is                                |
|                                 | \http://emsearch.rutgers.edu/atla\                     |
|                                 | \s/#VALUE_summary.html,                                |
|                                 | and the value entered is 1001,                         |
|                                 | the field is displayed as                              |
|                                 | `1001 <http://emsearch.rutgers.ed                      |
|                                 | u/atlas/1001_summary.html>`__                          |
|                                 | (hyperlinked to                                        |
|                                 | http://emsearch.rutgers.edu/atlas/1001_summary.html)). |
+---------------------------------+--------------------------------------------------------+
| <img src="#VALUE" alt="#NAME"   | For displaying the image of an                         |
| class="metadata-logo"/><br/>    | entered image URL (used to                             |
|                                 | display images in the producer                         |
|                                 | and distributor logos metadata                         |
|                                 | fields).                                               |
+---------------------------------+--------------------------------------------------------+
| #VALUE:                         | Appends and/or prepends                                |
|                                 | characters to the value of the                         |
| \- #VALUE:                      | field. e.g. if the displayFormat                       |
|                                 | for the distributorAffiliation is                      |
| (#VALUE)                        | (#VALUE) (wrapped with parens)                         |
|                                 | and the value entered                                  |
|                                 | is University of North                                 |
|                                 | Carolina, the field is displayed                       |
|                                 | in the UI as (University of                            |
|                                 | North Carolina).                                       |
+---------------------------------+--------------------------------------------------------+
| ;                               | Displays the character (e.g.                           |
|                                 | semicolon, comma) between the                          |
| :                               | values of fields within                                |
|                                 | compound fields. For example,                          |
| ,                               | if the displayFormat for the                           |
|                                 | compound field “series” is a                           |
|                                 | colon, and if the value                                |
|                                 | entered for seriesName is                              |
|                                 | IMPs and for                                           |
|                                 | seriesInformation is A                                 |
|                                 | collection of NMR data, the                            |
|                                 | compound field is displayed in                         |
|                                 | the UI as IMPs: A                                      |
|                                 | collection of NMR data.                                |
+---------------------------------+--------------------------------------------------------+

Metadata Block Setup
--------------------

Now that you understand the TSV format used for metadata blocks, the next step is to attempt to make improvements to existing metadata blocks or create entirely new metadata blocks. For either task, you should have a Dataverse Software development environment set up for testing where you can drop the database frequently while you make edits to TSV files. Once you have tested your TSV files, you should consider making a pull request to contribute your improvement back to the community.

.. _exploring-metadata-blocks:

Exploring Metadata Blocks
~~~~~~~~~~~~~~~~~~~~~~~~~

In addition to studying the TSV files themselves you will probably find the :ref:`metadata-blocks-api` API helpful in getting a structured dump of metadata blocks in JSON format.

There are also a few older, highly experimental, and subject-to-change API endpoints under the "admin" API documented below but the public API above is preferred.

You can get a dump of metadata fields like this:

``curl http://localhost:8080/api/admin/datasetfield``

To see details about an individual field such as "title" in the example below:

``curl http://localhost:8080/api/admin/datasetfield/title``

Setting Up a Dev Environment for Testing
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

You have several options for setting up a dev environment for testing metadata block changes:

- Docker: See :doc:`/container/running/metadata-blocks` in the Container Guide.
- AWS deployment: See the :doc:`/developers/deployment` section of the Developer Guide.
- Full dev environment: See the :doc:`/developers/dev-environment` section of the Developer Guide.

Editing TSV files
~~~~~~~~~~~~~~~~~

Early in Dataverse Software 4.0 development, metadata blocks were edited in the Google spreadsheet mentioned above and then exported in TSV format. This worked fine when there was only one person editing the Google spreadsheet but now that contributions are coming in from all over, the TSV files are edited directly. We are somewhat painfully aware that another format such as XML might make more sense these days. Please see https://github.com/IQSS/dataverse/issues/4451 for a discussion of non-TSV formats.

Please note that metadata fields share a common namespace so they must be unique. The following curl command will print the list of metadata fields already available in the system:

``curl http://localhost:8080/api/admin/index/solr/schema``

We'll use this command again below to update the Solr schema to accomodate metadata fields we've added.

Loading TSV files into a Dataverse Installation
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

A number of TSV files are loaded into a newly-installed Dataverse installation, becoming the metadata blocks you see in the UI. For the list of metadata blocks that are included with the Dataverse Software out of the box, see the :doc:`/user/appendix` section of the User Guide.

Along with TSV file, there are corresponding ResourceBundle property files with key=value pair `here <https://github.com/IQSS/dataverse/tree/develop/src/main/java/propertyFiles>`__.  To add other language files, see the :doc:`/installation/config` for dataverse.lang.directory JVM Options section, and add a file, for example: "citation_lang.properties" to the path you specified for the ``dataverse.lang.directory`` JVM option, and then restart the app server.

If you are improving an existing metadata block, the Dataverse Software installation process will load the TSV for you, assuming you edited the TSV file in place. The TSV file for the Citation metadata block, for example, can be found at ``scripts/api/data/metadatablocks/citation.tsv``.
If any of the below mentioned property values are changed, corresponding ResourceBundle property file has to be edited and stored under ``dataverse.lang.directory`` location

- name, displayName property under #metadataBlock
- name, title, description, watermark properties under #datasetfield
- DatasetField, Value property under #controlledVocabulary

If you are creating a new custom metadata block (hopefully with the idea of contributing it back to the community if you feel like it would provide value to others), the Dataverse Software installation process won't know about your new TSV file so you must load it manually. The script that loads the TSV files into the system is ``scripts/api/setup-datasetfields.sh`` and contains a series of curl commands. Here's an example of the necessary curl command with the new custom metadata block in the "/tmp" directory.

``curl http://localhost:8080/api/admin/datasetfield/load -H "Content-type: text/tab-separated-values" -X POST --upload-file /tmp/new-metadata-block.tsv``

To create a new ResourceBundle, here are the steps to generate key=value pair for the three main sections:

#metadataBlock properties
~~~~~~~~~~~~~~~~~~~~~~~~~
metadatablock.name=(the value of **name** property from #metadatablock)

metadatablock.displayName=(the value of **displayName** property from #metadatablock)

metadatablock.displayFacet=(the value of **displayFacet** property from #metadatablock)

example:

metadatablock.name=citation

metadatablock.displayName=Citation Metadata

metadatablock.displayFacet=Citation

#datasetField (field) properties
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
datasetfieldtype.(the value of **name** property from #datasetField).title=(the value of **title** property from #datasetField)

datasetfieldtype.(the value of **name** property from #datasetField).description=(the value of **description** property from #datasetField)

datasetfieldtype.(the value of **name** property from #datasetField).watermark=(the value of **watermark** property from #datasetField)

example:

datasetfieldtype.title.title=Title

datasetfieldtype.title.description=Full title by which the Dataset is known.

datasetfieldtype.title.watermark=Enter title...

#controlledVocabulary (enumerated) properties
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
controlledvocabulary.(the value of **DatasetField** property from #controlledVocabulary).(the value of **Value** property from #controlledVocabulary)=(the value of **Value** property from #controlledVocabulary)

Since the **Value** property from #controlledVocabulary is free text, while creating the key, it has to be converted to lowercase, replace space with underscore, and strip accents.

example:

controlledvocabulary.subject.agricultural_sciences=Agricultural Sciences

controlledvocabulary.language.marathi_(marathi)=Marathi (Mar\u0101\u1E6Dh\u012B)


Enabling a Metadata Block
~~~~~~~~~~~~~~~~~~~~~~~~~

Running a curl command like "load" example above should make the new custom metadata block available within the system but in order to start using the fields you must either enable it from the UI (see :ref:`general-information` section of Dataverse Collection Management in the User Guide) or by running a curl command like the one below using a superuser API token. In the example below we are enabling the "journal" and "geospatial" metadata blocks for the root Dataverse collection:

``curl -H "X-Dataverse-key:$API_TOKEN" -X POST -H "Content-type:application/json" -d "[\"journal\",\"geospatial\"]" http://localhost:8080/api/dataverses/:root/metadatablocks``

.. _update-solr-schema:

Updating the Solr Schema
~~~~~~~~~~~~~~~~~~~~~~~~

Once you have enabled a new metadata block you should be able to see the new fields in the GUI but before you can save
the dataset, you must add additional fields to your Solr schema.

An API endpoint of your Dataverse installation provides you with a generated set of all fields that need to be added to
the Solr schema configuration, including any enabled metadata schemas:

``curl "http://localhost:8080/api/admin/index/solr/schema"``

You can use :download:`update-fields.sh <../../../../conf/solr/update-fields.sh>` to easily add these to the
Solr schema you installed for your Dataverse installation.

The script needs a target XML file containing your Solr schema. (See the :doc:`/installation/prerequisites/` section of
the Installation Guide for a suggested location on disk for the Solr schema file.)

You can either pipe the downloaded schema to the script or provide the file as an argument. (We recommended you to take
a look at usage output of ``update-fields.sh -h``)

.. code-block::
    :caption: Example usage of ``update-fields.sh``

    curl "http://localhost:8080/api/admin/index/solr/schema" | update-fields.sh /usr/local/solr/server/solr/collection1/conf/schema.xml

You will need to reload your Solr schema via an HTTP-API call, targeting your Solr instance:

``curl "http://localhost:8983/solr/admin/cores?action=RELOAD&core=collection1"``

You can easily roll your own little script to automate the process (which might involve fetching the schema bits
from some place else than your Dataverse installation).

Please note that reconfigurations of your Solr index might require a re-index. Usually release notes indicate
a necessary re-index, but for your custom metadata you will need to keep track on your own.

Please note also that if you are going to make a pull request updating ``conf/solr/schema.xml`` with fields you have
added, you should first load all the custom metadata blocks in ``scripts/api/data/metadatablocks`` (including ones you
don't care about) to create a complete list of fields. (This might change in the future.) Please see :ref:`update-solr-schema-dev` in the Developer Guide.

Reloading a Metadata Block
--------------------------

As mentioned above, changes to metadata blocks that ship with the Dataverse Software will be made over time to improve them and release notes will sometimes instruct you to reload an existing metadata block. The syntax for reloading is the same as loading. Here's an example with the "citation" metadata block:

``curl http://localhost:8080/api/admin/datasetfield/load -H "Content-type: text/tab-separated-values" -X POST --upload-file citation.tsv``

Great care must be taken when reloading a metadata block. Matching is done on field names (or identifiers and then names in the case of controlled vocabulary values) so it's easy to accidentally create duplicate fields.

The ability to reload metadata blocks means that SQL update scripts don't need to be written for these changes. See also the :doc:`/developers/sql-upgrade-scripts` section of the Developer Guide.

.. _using-external-vocabulary-services:

Using External Vocabulary Services
----------------------------------

The Dataverse software has a mechanism to associate specific fields defined in metadata blocks with a vocabulary(ies) managed by external services. The mechanism relies on trusted third-party Javascripts. The mapping from field type to external vocabulary(ies) is managed via the :ref:`:CVocConf <:CVocConf>` setting.

*This functionality may require significant effort to configure and is likely to evolve in subsequent Dataverse software releases.*

The effect of configuring this mechanism is similar to that of defining a field in a metadata block with 'allowControlledVocabulary=true':

- Users are able to select from a controlled list of values.
- Values can be shown in any language the term has been defined in.
  
In general, the external vocabulary support mechanism may be a better choice for large vocabularies, hierarchical/structured vocabularies, and/or vocabularies managed by third-parties. In addition, the external vocabulary mechanism differs from the internal controlled vocabulary mechanism in several ways that may make it a preferred option:

- the machine-readable URI form of a vocabulary is stored in the Dataverse database and can be included in exported metadata files.
- vocabulary mappings can be changed without changing the metadata block, making it possible for different Dataverse installations to use different vocabularies in the same field.
- mappings can associate a field with more than one vocabulary.
- mappings can be configured to also allow custom/free-text entries as well as vocabulary values.
- mappings can be configured for compound fields and a user's selection of a given vocabulary value can be used to fill in related child fields (e.g. selection of a keyword could fill in a vocabulary name field as well).
- removing a mapping does not affect stored values (the field would revert to allowing free text).
 
The specifics of the user interface for entering/selecting a vocabulary term and how that term is then displayed are managed by third-party Javascripts. The initial Javascripts that have been created provide auto-completion, displaying a list of choices that match what the user has typed so far, but other interfaces, such as displaying a tree of options for a hierarchical vocabulary, are possible. 
Similarly, existing scripts do relatively simple things for displaying a term - showing the term's name in the appropriate language and providing a link to an external URL with more information, but more sophisticated displays are possible.

Scripts supporting use of vocabularies from services supporting the SKOSMOS protocol (see https://skosmos.org), retrieving ORCIDs (from https://orcid.org), services based on Ontoportal product (see https://ontoportal.org/), and using ROR (https://ror.org/) are available https://github.com/gdcc/dataverse-external-vocab-support. (Custom scripts can also be used and community members are encouraged to share new scripts through the dataverse-external-vocab-support repository.)

Configuration involves specifying which fields are to be mapped, to which Solr field they should be indexed, whether free-text entries are allowed, which vocabulary(ies) should be used, what languages those vocabulary(ies) are available in, and several service protocol and service instance specific parameters, including the ability to send HTTP headers on calls to the service.
These are all defined in the :ref:`:CVocConf <:CVocConf>` setting as a JSON array. Details about the required elements as well as example JSON arrays are available at https://github.com/gdcc/dataverse-external-vocab-support, along with an example metadata block that can be used for testing.
The scripts required can be hosted locally or retrieved dynamically from https://gdcc.github.io/ (similar to how dataverse-previewers work).

Since external vocabulary scripts can change how fields are indexed (storing an identifier and name and/or values in different languages),
updating the Solr schema as described in :ref:`update-solr-schema` should be done after adding new scripts to your configuration.

Please note that in addition to the :ref:`:CVocConf` described above, an alternative is the :ref:`:ControlledVocabularyCustomJavaScript` setting.

Protecting MetadataBlocks
-------------------------

Dataverse can be configured to only allow entries for a metadata block to be changed (created, edited, deleted) by entities that know a defined secret key. 
Metadata blocks protected by such a key are referred to as "System" metadata blocks. 
A primary use case for system metadata blocks is to handle metadata created by third-party tools interacting with Dataverse where unintended changes to the metadata could cause a failure. Examples might include archiving systems or workflow engines.
To protect an existing metadatablock, one must set a key (recommended to be long and un-guessable) for that block:

dataverse.metadata.block-system-metadata-keys.<block name>=<key value>

This can be done using system properties (see :ref:`jvm-options`), environment variables or other MicroProfile Config mechanisms supported by the app server.
   `See Payara docs for supported sources <https://docs.payara.fish/community/docs/documentation/microprofile/config/README.html#config-sources>`_. Note that a Payara restart may be required to enable the new option.

For these secret keys, Payara password aliases are recommended.

   Alias creation example using the codemeta metadata block (actual name: codeMeta20):

   .. code-block:: shell

      echo "AS_ADMIN_ALIASPASSWORD=1234ChangeMeToSomethingLong" > /tmp/key.txt
      asadmin create-password-alias --passwordfile /tmp/key.txt dataverse.metadata.block-system-metadata-keys.codeMeta20
      rm /tmp/key.txt
      
   Alias deletion example for the codemeta metadata block (removes protected status)
   
   .. code-block:: shell

      asadmin delete-password-alias dataverse.metadata.block-system-metadata-keys.codeMeta20

A Payara restart is required after these example commands.

When protected via a key, a metadata block will not be shown in the user interface when a dataset is being created or when metadata is being edited. Entries in such a system metadata block will be shown to users, consistent with Dataverse's design in which all metadata in published datasets is publicly visible.

Note that protecting a block with required fields, or using a template with an entry in a protected block, will make it impossible to create a new dataset via the user interface. Also note that for this reason protecting the citation metadatablock is not recommended. (Creating a dataset also automatically sets the date of deposit field in the citation block, which would be prohibited if the citation block is protected.) 

To remove proted status and return a block to working normally, remove the associated key.

To add metadata to a system metadata block via API, one must include an additional key of the form 

mdkey.<blockName>=<key value>

as an HTTP Header or query parameter (case sensitive) for each system metadata block to any API call in which metadata values are changed in that block. Multiple keys are allowed if more than one system metadatablock is being changed in a given API call.

For example, following the :ref:`Add Dataset Metadata <add-semantic-metadata>` example from the :doc:`/developers/dataset-semantic-metadata-api`:

.. code-block:: bash

  curl -X PUT -H X-Dataverse-key:$API_TOKEN -H 'Content-Type: application/ld+json' -H 'mdkey.codeMeta20:1234ChangeMeToSomethingLong' -d '{"codeVersion": "1.0.0", "@context":{"codeVersion": "https://schema.org/softwareVersion"}}' "$SERVER_URL/api/datasets/$DATASET_ID/metadata"
  
  curl -X PUT -H X-Dataverse-key:$API_TOKEN -H 'Content-Type: application/ld+json' -d '{"codeVersion": "1.0.1", "@context":{"codeVersion": "https://schema.org/softwareVersion"}}' "$SERVER_URL/api/datasets/$DATASET_ID/metadata?mdkey.codeMeta20=1234ChangeMeToSomethingLong&replace=true"
    

Tips from the Dataverse Community
---------------------------------

When creating new metadata blocks, please review the :doc:`/style/text` section of the Style Guide, which includes guidance about naming metadata fields and writing text for metadata tooltips and watermarks.

If there are tips that you feel are omitted from this document, please open an issue at https://github.com/IQSS/dataverse/issues and consider making a pull request to make improvements. You can find this document at https://github.com/IQSS/dataverse/blob/develop/doc/sphinx-guides/source/admin/metadatacustomization.rst

Alternatively, you are welcome to request "edit" access to this "Tips for Dataverse Software metadata blocks from the community" Google doc: https://docs.google.com/document/d/1XpblRw0v0SvV-Bq6njlN96WyHJ7tqG0WWejqBdl7hE0/edit?usp=sharing

The thinking is that the tips can become issues and the issues can eventually be worked on as features to improve the Dataverse Software metadata system.

Development Tasks Specific to Changing Fields in Core Metadata Blocks
---------------------------------------------------------------------

When it comes to the fields from the core blocks that are distributed with Dataverse (such as Citation, Social Science and Geospatial blocks), code dependencies may exist in Dataverse, primarily in the Import and Export subsystems, on these fields being configured a certain way. So, if it becomes necessary to modify one of such core fields, code changes may be necessary to accompany the change in the block tsv, plus some sample and test files maintained in the Dataverse source tree will need to be adjusted accordingly. 

Making a Field Multi-Valued
~~~~~~~~~~~~~~~~~~~~~~~~~~~

As a recent real life example, a few fields from the Citation and Social Science block were changed to support multiple values, in order to accommodate specific needs of some community member institutions. A PR for one of these fields, ``alternativeTitle`` from the Citation block is linked below. Each time a number of code changes, plus some changes in the sample metadata files in the Dataverse code tree had to be made. The checklist below is to help another developer in the event that a similar change becomes necessary in the future. Note that some of the steps below may not apply 1:1 to a different metadata field, depending on how it is exported and imported in various formats by Dataverse. It may help to consult the PR `#9440 <https://github.com/IQSS/dataverse/pull/9440/files>`_ as a specific example of the changes that had to be made for the ``alternativeTitle`` field. 

- Change the value from ``FALSE`` to ``TRUE`` in the ``allowmultiples`` column of the .tsv file for the block.
- Change the value of the ``multiValued`` attribute for the search field in the Solr schema (``conf/solr/x.x.x/schema.xml``).
- Modify the DDI import code (``ImportDDIServiceBean.java``) to support multiple values. (You may be able to use the change in the PR above as a model.)
- Modify the DDI export utility (``DdiExportUtil.java``).
- Modify the OpenAire export utility (``OpenAireExportUtil.java``).
- Modify the following JSON source files in the Dataverse code tree to actually include multiple values for the field (two should be quite enough!): ``scripts/api/data/dataset-create-new-all-default-fields.json``, ``src/test/java/edu/harvard/iq/dataverse/export/dataset-all-defaults.txt``, ``src/test/java/edu/harvard/iq/dataverse/export/ddi/dataset-finch1.json`` and ``src/test/java/edu/harvard/iq/dataverse/export/ddi/dataset-create-new-all-ddi-fields.json``. (These are used as examples for populating datasets via the import API and by the automated import and export code tests).
- Similarly modify the following XML files that are used by the DDI export code tests: ``src/test/java/edu/harvard/iq/dataverse/export/ddi/dataset-finch1.xml`` and ``src/test/java/edu/harvard/iq/dataverse/export/ddi/exportfull.xml``.
- Make sure all the automated unit and integration tests are passing. See :doc:`/developers/testing` in the Developer Guide.
- Write a short release note to announce the change in the upcoming release. See :ref:`writing-release-note-snippets` in the Developer Guide.
- Make a pull request. 


Footnotes
---------

.. [1]
   https://www.iana.org/assignments/media-types/text/tab-separated-values

.. [2]
   Although the structure of the data, as you’ll see below, violates the
   “Each record must have the same number of fields” tenet of TSV

.. [3]
   https://en.wikipedia.org/wiki/CamelCase

.. [4]
   These field names are added to the Solr schema.xml and cannot be
   duplicated. See "Editing TSV files" for how to check for duplication.

.. [5]
   "displayoncreate" was "showabovefold" in Dataverse Software ``<=4.3.1`` (see
   `#3073 <https://github.com/IQSS/dataverse/issues/3073>`__) but parsing is
   done based on column order rather than name so this only matters to the
   person reading the TSV file.
