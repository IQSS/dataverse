Metadata Customization
======================

Dataverse has a flexible data-driven metadata system powered by "metadata blocks" that are listed in the :doc:`/user/appendix` section of the User Guide. In this section we explain the customization options.

.. contents:: |toctitle|
	:local:

Introduction
------------

Before you embark on customizing metadata in Dataverse you should make sure you are aware of the modest amount of customization that is available with the Dataverse web interface. It's possible to hide fields and make field required by clicking "Edit" at the dataverse level, clicking "General Information" and making adjustments under "Metadata Fields" as described in the context of dataset templates in the :doc:`/user/dataverse-management` section of the User Guide.

Much more customization of metadata is possible, but this is an advanced topic so feedback on what is written below is very welcome. The possibilities for customization include:

-  Editing and adding metadata fields

-  Editing and adding instructional text (field label tooltips and text
   box watermarks)

-  Editing and adding controlled vocabularies

-  Changing which fields depositors must use in order to save datasets (see also "dataset templates" in the :doc:`/user/dataverse-management` section of the User Guide.)

-  Changing how saved metadata values are displayed in the UI

Generally speaking it is safer to create your own custom metadata block rather than editing metadata blocks that ship with Dataverse, because changes to these blocks may be made in future releases of Dataverse. If you'd like to make improvements to any of the metadata blocks shipped with Dataverse, please open an issue at https://github.com/IQSS/dataverse/issues so it can be discussed before a pull request is made. Please note that the metadata blocks shipped with Dataverse are based on standards (e.g. DDI for social science) and you can learn more about these standards in the :doc:`/user/appendix` section of the User Guide. If you have developed your own custom metadata block that you think may be of interest to the Dataverse community, please create an issue and consider making a pull request as described in the :doc:`/developers/version-control` section of the Developer Guide.

In Dataverse 4, custom metadata are no longer defined as individual
fields, as they were in Dataverse Network (DVN) 3.x, but in metadata blocks.
Dataverse 4 ships with a citation metadata block, which includes
mandatory fields needed for assigning persistent IDs to datasets, and
domain specific metadata blocks. For a complete list, see the
:doc:`/user/appendix` section of the User Guide.

Definitions of these blocks are loaded into a Dataverse installation in
tab-separated value (TSV). [1]_\ :sup:`,`\  [2]_ While it is technically
possible to define more than one metadata block in a TSV file, it is
good organizational practice to define only one in each file.

The metadata block TSVs shipped with Dataverse are in `this folder in
the Dataverse github
repo <https://github.com/IQSS/dataverse/tree/develop/scripts/api/data/metadatablocks>`__ and the corresponding ResourceBundle property files are `here <https://github.com/IQSS/dataverse/tree/develop/src/main/java>`__.
Human-readable copies are available in `this Google Sheets
document <https://docs.google.com/spreadsheets/d/13HP-jI_cwLDHBetn9UKTREPJ_F4iHdAvhjmlvmYdSSw/edit#gid=0>`__ but they tend to get out of sync with the TSV files, which should be considered authoritative. The Dataverse installation process operates on the TSVs, not the Google spreadsheet.

About the metadata block TSV
----------------------------

Here we list and describe the purposes of each section and property of
the metadata block TSV.

1. metadataBlock

   -  Purpose: Represents the metadata block being defined.

   -  Cardinality:

      -  0 or more per Dataverse

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

+-----------------------+-----------------------+-----------------------+
| **Property**          | **Purpose**           | **Allowed values and  |
|                       |                       | restrictions**        |
+-----------------------+-----------------------+-----------------------+
| name                  | A user-definable      | \• No spaces or       |
|                       | string used to        | punctuation,          |
|                       | identify a            | except underscore.    |
|                       | #metadataBlock        |                       |
|                       |                       | \• By convention,     |
|                       |                       | should start with     |
|                       |                       | a letter, and use     |
|                       |                       | lower camel           |
|                       |                       | case [3]_             |
|                       |                       |                       |
|                       |                       | \• Must not collide   |
|                       |                       | with a field of       |
|                       |                       | the same name in      |
|                       |                       | the same or any       |
|                       |                       | other                 |
|                       |                       | #datasetField         |
|                       |                       | definition,           |
|                       |                       | including metadata    |
|                       |                       | blocks defined        |
|                       |                       | elsewhere. [4]_       |
+-----------------------+-----------------------+-----------------------+
| dataverseAlias        | If specified, this    | Free text. For an     |
|                       | metadata block will   | example, see          |
|                       | be available only to  | custom_hbgdki.tsv.    |
|                       | the dataverse         |                       |
|                       | designated here by    |                       |
|                       | its alias and to      |                       |
|                       | children of that      |                       |
|                       | dataverse.            |                       |
+-----------------------+-----------------------+-----------------------+
| displayName           | Acts as a brief label | Should be relatively  |
|                       | for display related   | brief. The limit is   |
|                       | to this               | 256 character, but    |
|                       | #metadataBlock.       | very long names might |
|                       |                       | cause display         |
|                       |                       | problems.             |
+-----------------------+-----------------------+-----------------------+

#datasetField (field) properties
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

+-----------------------+-----------------------+------------------------+
| **Property**          | **Purpose**           | **Allowed values and   |
|                       |                       | restrictions**         |
+-----------------------+-----------------------+------------------------+
| name                  | A user-definable      | \• (from               |
|                       | string used to        | DatasetFieldType.java) |
|                       | identify a            | The internal           |
|                       | #datasetField. Maps   | DDI-like name, no      |
|                       | directly to field     | spaces, etc.           |
|                       | name used by Solr.    |                        |
|                       |                       | \• (from Solr) Field   |
|                       |                       | names should           |
|                       |                       | consist of             |
|                       |                       | alphanumeric or        |
|                       |                       | underscore             |
|                       |                       | characters only        |
|                       |                       | and not start with     |
|                       |                       | a digit. This is       |
|                       |                       | not currently          |
|                       |                       | strictly enforced,     |
|                       |                       | but other field        |
|                       |                       | names will not         |
|                       |                       | have first class       |
|                       |                       | support from all       |
|                       |                       | components and         |
|                       |                       | back compatibility     |
|                       |                       | is not guaranteed.     |
|                       |                       | Names with both        |
|                       |                       | leading and            |
|                       |                       | trailing               |
|                       |                       | underscores (e.g.      |
|                       |                       | \_version_) are        |
|                       |                       | reserved.              |
|                       |                       |                        |
|                       |                       | \• Must not collide    |
|                       |                       | with a field of        |
|                       |                       | the same same name     |
|                       |                       | in another             |
|                       |                       | #metadataBlock         |
|                       |                       | definition or any      |
|                       |                       | name already           |
|                       |                       | included as a          |
|                       |                       | field in the Solr      |
|                       |                       | index.                 |
+-----------------------+-----------------------+------------------------+
| title                 | Acts as a brief label | Should be relatively   |
|                       | for display related   | brief.                 |
|                       | to this               |                        |
|                       | #datasetField.        |                        |
+-----------------------+-----------------------+------------------------+
| description           | Used to provide a     | Free text              |
|                       | description of the    |                        |
|                       | field.                |                        |
+-----------------------+-----------------------+------------------------+
| watermark             | A string to initially | Free text              |
|                       | display in a field as |                        |
|                       | a prompt for what the |                        |
|                       | user should enter.    |                        |
+-----------------------+-----------------------+------------------------+
| fieldType             | Defines the type of   | | \• none              |
|                       | content that the      | | \• date              |
|                       | field, if not empty,  | | \• email             |
|                       | is meant to contain.  | | \• text              |
|                       |                       | | \• textbox           |
|                       |                       | | \• url               |
|                       |                       | | \• int               |
|                       |                       | | \• float             |
|                       |                       | | \• See below for     |
|                       |                       | | fieldtype definitions|
+-----------------------+-----------------------+------------------------+
| displayOrder          | Controls the sequence | Non-negative integer.  |
|                       | in which the fields   |                        |
|                       | are displayed, both   |                        |
|                       | for input and         |                        |
|                       | presentation.         |                        |
+-----------------------+-----------------------+------------------------+
| displayFormat         | Controls how the      | See below for          |
|                       | content is displayed  | displayFormat          |
|                       | for presentation (not | variables              |
|                       | entry). The value of  |                        |
|                       | this field may        |                        |
|                       | contain one or more   |                        |
|                       | special variables     |                        |
|                       | (enumerated below).   |                        |
|                       | HTML tags, likely in  |                        |
|                       | conjunction with one  |                        |
|                       | or more of these      |                        |
|                       | values, may be used   |                        |
|                       | to control the        |                        |
|                       | display of content in |                        |
|                       | the web UI.           |                        |
+-----------------------+-----------------------+------------------------+
| advancedSearchField   | Specify whether this  | TRUE (available) or    |
|                       | field is available in | FALSE (not available)  |
|                       | advanced search.      |                        |
+-----------------------+-----------------------+------------------------+
| allowControlledVocabu\| Specify whether the   | TRUE (controlled) or   |
| \lary                 | possible values of    | FALSE (not             |
|                       | this field are        | controlled)            |
|                       | determined by values  |                        |
|                       | in the                |                        |
|                       | #controlledVocabulary |                        |
|                       | section.              |                        |
+-----------------------+-----------------------+------------------------+
| allowmultiples        | Specify whether this  | TRUE (repeatable) or   |
|                       | field is repeatable.  | FALSE (not             |
|                       |                       | repeatable)            |
+-----------------------+-----------------------+------------------------+
| facetable             | Specify whether the   | TRUE (controlled) or   |
|                       | field is facetable    | FALSE (not             |
|                       | (i.e., if the         | controlled)            |
|                       | expected values for   |                        |
|                       | this field are        |                        |
|                       | themselves useful     |                        |
|                       | search terms for this |                        |
|                       | field). If a field is |                        |
|                       | "facetable" (able to  |                        |
|                       | be faceted on), it    |                        |
|                       | appears under         |                        |
|                       | "Browse/Search        |                        |
|                       | Facets" when you edit |                        |
|                       | "General Information" |                        |
|                       | for a dataverse.      |                        |
|                       | Setting this value to |                        |
|                       | TRUE generally makes  |                        |
|                       | sense for enumerated  |                        |
|                       | or controlled         |                        |
|                       | vocabulary fields,    |                        |
|                       | fields representing   |                        |
|                       | identifiers (IDs,     |                        |
|                       | names, email          |                        |
|                       | addresses), and other |                        |
|                       | fields that are       |                        |
|                       | likely to share       |                        |
|                       | values across         |                        |
|                       | entries. It is less   |                        |
|                       | likely to make sense  |                        |
|                       | for fields containing |                        |
|                       | descriptions,         |                        |
|                       | floating point        |                        |
|                       | numbers, and other    |                        |
|                       | values that are       |                        |
|                       | likely to be unique.  |                        |
+-----------------------+-----------------------+------------------------+
| displayoncreate [5]_  | Designate fields that | TRUE (display during   |
|                       | should display during | creation) or FALSE     |
|                       | the creation of a new | (don’t display during  |
|                       | dataset, even before  | creation)              |
|                       | the dataset is saved. |                        |
|                       | Fields not so         |                        |
|                       | designated will not   |                        |
|                       | be displayed until    |                        |
|                       | the dataset has been  |                        |
|                       | saved.                |                        |
+-----------------------+-----------------------+------------------------+
| required              | Specify whether or    | TRUE (required) or     |
|                       | not the field is      | FALSE (optional)       |
|                       | required. This means  |                        |
|                       | that at least one     |                        |
|                       | instance of the field |                        |
|                       | must be present. More |                        |
|                       | than one field may be |                        |
|                       | allowed, depending on |                        |
|                       | the value of          |                        |
|                       | allowmultiples.       |                        |
+-----------------------+-----------------------+------------------------+
| parent                | For subfields,        | \• Must not result in  |
|                       | specify the name of   | a cyclical             |
|                       | the parent or         | reference.             |
|                       | containing field.     |                        |
|                       |                       | \• Must reference an   |
|                       |                       | existing field in      |
|                       |                       | the same               |
|                       |                       | #metadataBlock.        |
+-----------------------+-----------------------+------------------------+
| metadatablock_id      | Specify the name of   | \• Must reference an   |
|                       | the #metadataBlock    | existing               |
|                       | that contains this    | #metadataBlock.        |
|                       | field.                |                        |
|                       |                       | \• As a best           |
|                       |                       | practice, the          |
|                       |                       | value should           |
|                       |                       | reference the          |
|                       |                       | #metadataBlock in      |
|                       |                       | the current            |
|                       |                       | definition             |
|                       |                       | (it is technically     |
|                       |                       | possible to            |
|                       |                       | reference another      |
|                       |                       | existing metadata      |
|                       |                       | block.)                |
+-----------------------+-----------------------+------------------------+

#controlledVocabulary (enumerated) properties
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

+-----------------------+-----------------------+-----------------------+
| **Property**          | **Purpose**           | **Allowed values and  |
|                       |                       | restrictions**        |
+-----------------------+-----------------------+-----------------------+
| DatasetField          | Specifies the         | Must reference an     |
|                       | #datasetField to which| existing              |
|                       | this entry applies.   | #datasetField.        |
|                       |                       | As a best practice,   |
|                       |                       | the value should      |
|                       |                       | reference a           |
|                       |                       | #datasetField in the  |
|                       |                       | current metadata      |
|                       |                       | block definition. (It |
|                       |                       | is technically        |
|                       |                       | possible to reference |
|                       |                       | an existing           |
|                       |                       | #datasetField from    |
|                       |                       | another metadata      |
|                       |                       | block.)               |
+-----------------------+-----------------------+-----------------------+
| Value                 | A short display       | Free text             |
|                       | string, representing  |                       |
|                       | an enumerated value   |                       |
|                       | for this field. If    |                       |
|                       | the identifier        |                       |
|                       | property is empty,    |                       |
|                       | this value is used as |                       |
|                       | the identifier.       |                       |
+-----------------------+-----------------------+-----------------------+
| identifier            | A string used to      | Free text             |
|                       | encode the selected   |                       |
|                       | enumerated value of a |                       |
|                       | field. If this        |                       |
|                       | property is empty,    |                       |
|                       | the value of the      |                       |
|                       | “Value” field is used |                       |
|                       | as the identifier.    |                       |
+-----------------------+-----------------------+-----------------------+
| displayOrder          | Control the order in  | Non-negative integer. |
|                       | which the enumerated  |                       |
|                       | values are displayed  |                       |
|                       | for selection.        |                       |
+-----------------------+-----------------------+-----------------------+

FieldType definitions
~~~~~~~~~~~~~~~~~~~~~

+-----------------------------------+-----------------------------------+
| **Fieldtype**                     | **Definition**                    |
+-----------------------------------+-----------------------------------+
| none                              | Used for compound fields, in which|
|                                   | case the parent field would have  |
|                                   | no value and display no data      |
|                                   | entry control.                    |
+-----------------------------------+-----------------------------------+
| date                              | A date, expressed in one of three |
|                                   | resolutions of the form           |
|                                   | YYYY-MM-DD, YYYY-MM, or YYYY.     |
+-----------------------------------+-----------------------------------+
| email                             | A valid email address. Not        |
|                                   | indexed for privacy reasons.      |
+-----------------------------------+-----------------------------------+
| text                              | Any text other than newlines may  |
|                                   | be entered into this field.       |
+-----------------------------------+-----------------------------------+
| textbox                           | Any text may be entered. For      |
|                                   | input, Dataverse presents a       |
|                                   | multi-line area that accepts      |
|                                   | newlines. While any HTML is       |
|                                   | permitted, only a subset of HTML  |
|                                   | tags will be rendered in the UI.  |
|                                   | A `list of supported tags is      |
|                                   | included in the Dataverse User    |
|                                   | Guide <http://guides.dataverse.or |
|                                   | g/en/latest/user/dataset-manageme |
|                                   | nt.html#supported-html-fields>`__ |
|                                   | .                                 |
+-----------------------------------+-----------------------------------+
| url                               | If not empty, field must contain  |
|                                   | a valid URL.                      |
+-----------------------------------+-----------------------------------+
| int                               | An integer value destined for a   |
|                                   | numeric field.                    |
+-----------------------------------+-----------------------------------+
| float                             | A floating point number destined  |
|                                   | for a numeric field.              |
+-----------------------------------+-----------------------------------+

displayFormat variables
~~~~~~~~~~~~~~~~~~~~~~~

These are common ways to use the displayFormat to control how values are displayed in the UI. This list is not exhaustive.

+-----------------------------------+-----------------------------------+
| **Variable**                      | **Description**                   |
+-----------------------------------+-----------------------------------+
| (blank)                           | The displayFormat is left blank   |
|                                   | for primitive fields (e.g.        |
|                                   | subtitle) and fields that do not  |
|                                   | take values (e.g. author), since  |
|                                   | displayFormats do not work for    |
|                                   | these fields.                     |
+-----------------------------------+-----------------------------------+
| #VALUE                            | The value of the field (instance  |
|                                   | level).                           |
+-----------------------------------+-----------------------------------+
| #NAME                             | The name of the field (class      |
|                                   | level).                           |
+-----------------------------------+-----------------------------------+
| #EMAIL                            | For displaying emails.            |
+-----------------------------------+-----------------------------------+
| <a href="#VALUE">#VALUE</a>       | For displaying the value as a     |
|                                   | link (if the value entered is a   |
|                                   | link).                            |         
+-----------------------------------+-----------------------------------+
| <a href='URL/#VALUE'>#VALUE</a>   | For displaying the value as a     |
|                                   | link, with the value included in  |
|                                   | the URL (e.g. if URL is           |
|                                   | \http://emsearch.rutgers.edu/atla\|
|                                   | \s/#VALUE_summary.html,           |
|                                   | and the value entered is 1001,    |
|                                   | the field is displayed as         |
|                                   | `1001 <http://emsearch.rutgers.ed |
|                                   | u/atlas/1001_summary.html>`__     |
|                                   | (hyperlinked to                   |
|                                   | \http://emsearch.rutgers.edu/atlas|
|                                   | /1001_summary.html)).             |
+-----------------------------------+-----------------------------------+
| <img src="#VALUE" alt="#NAME"     | For displaying the image of an    |
| class="metadata-logo"/><br/>      | entered image URL (used to        |
|                                   | display images in the producer    |
|                                   | and distributor logos metadata    |
|                                   | fields).                          |
+-----------------------------------+-----------------------------------+
| #VALUE:                           | Appends and/or prepends           |
|                                   | characters to the value of the    |
| \- #VALUE:                        | field. e.g. if the displayFormat  |
|                                   | for the distributorAffiliation is |
| (#VALUE)                          | (#VALUE) (wrapped with parens)    |
|                                   | and the value entered             |
|                                   | is University of North            |
|                                   | Carolina, the field is displayed  |
|                                   | in the UI as (University of       |
|                                   | North Carolina).                  |
+-----------------------------------+-----------------------------------+
|    ;                              | Displays the character (e.g.      |
|                                   | semicolon, comma) between the     |
|    :                              | values of fields within           |
|                                   | compound fields. For example,     |
|    ,                              | if the displayFormat for the      |
|                                   | compound field “series” is a      |
|                                   | colon, and if the value           |
|                                   | entered for seriesName is         |
|                                   | IMPs and for                      |
|                                   | seriesInformation is A            |
|                                   | collection of NMR data, the       |
|                                   | compound field is displayed in    |
|                                   | the UI as IMPs: A                 |
|                                   | collection of NMR data.           |
+-----------------------------------+-----------------------------------+

Metadata Block Setup
--------------------

Now that you understand the TSV format used for metadata blocks, the next step is to attempt to make improvements to existing metadata blocks or create entirely new metadata blocks. For either task, you should have a Dataverse environment set up for testing where you can drop the database frequently while you make edits to TSV files. Once you have tested your TSV files, you should consider making a pull request to contribute your improvement back to the community.

Exploring Metadata Blocks
~~~~~~~~~~~~~~~~~~~~~~~~~

In addition to studying the TSV files themselves you might find the following highly experimental and subject-to-change API endpoints useful to understand the metadata blocks that have already been loaded into your installation of Dataverse:

You can get a dump of metadata fields (yes, the output is odd, please open a issue) like this:

``curl http://localhost:8080/api/admin/datasetfield``

To see details about an individual field such as "title" in the example below:

``curl http://localhost:8080/api/admin/datasetfield/title``

Setting Up a Dev Environment for Testing
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

You have several options for setting up a dev environment for testing metadata block changes:

- Vagrant: See the :doc:`/developers/tools` section of the Dev Guide.
- docker-aio: See https://github.com/IQSS/dataverse/tree/develop/conf/docker-aio
- AWS deployment: See the :doc:`/developers/deployment` section of the Dev Guide.
- Full dev environment: See the :doc:`/developers/dev-environment` section of the Dev Guide.

To get a clean environment in Vagrant, you'll be running ``vagrant destroy``. In Docker, you'll use ``docker rm``. For a full dev environment or AWS installation, you might find ``rebuild`` and related scripts at ``scripts/deploy/phoenix.dataverse.org`` useful.

Editing TSV files
~~~~~~~~~~~~~~~~~

Early in Dataverse 4 development metadata blocks were edited in the Google spreadsheet mentioned above and then exported in TSV format. This worked fine when there was only one person editing the Google spreadsheet but now that contributions are coming in from all over, the TSV files are edited directly. We are somewhat painfully aware that another format such as XML might make more sense these days. Please see https://github.com/IQSS/dataverse/issues/4451 for a discussion of non-TSV formats.

Please note that metadata fields share a common namespace so they must be unique. The following curl command will print list of metadata fields already available in the system:

``curl http://localhost:8080/api/admin/index/solr/schema``

We'll use this command again below to update the Solr schema to accomodate metadata fields we've added.

Loading TSV files into Dataverse
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

A number of TSV files are loaded into Dataverse on every new installation, becoming the metadata blocks you see in the UI. For the list of metadata blocks that are included with Dataverse out of the box, see the :doc:`/user/appendix` section of the User Guide.

Along with TSV file, there are corresponding ResourceBundle property files with key=value pair `here <https://github.com/IQSS/dataverse/tree/develop/src/main/java>`__.  To add other language files, see the :doc:`/installation/config` for dataverse.lang.directory JVM Options section, and add a file, for example: "citation_lang.properties" to the path you specified for the ``dataverse.lang.directory`` JVM option, and then restart Glassfish.

If you are improving an existing metadata block, the Dataverse installation process will load the TSV for you, assuming you edited the TSV file in place. The TSV file for the Citation metadata block, for example, can be found at ``scripts/api/data/metadatablocks/citation.tsv``.
If any of the below mentioned property values are changed, corresponsing ResourceBundle property file has to be edited and stored under ``dataverse.lang.directory`` location

- name, displayName property under #metadataBlock
- name, title, description, watermark properties under #datasetfield
- DatasetField, Value property under #controlledVocabulary

If you are creating a new custom metadata block (hopefully with the idea of contributing it back to the community if you feel like it would provide value to others), the Dataverse installation process won't know about your new TSV file so you must load it manually. The script that loads the TSV files into the system is ``scripts/api/setup-datasetfields.sh`` and contains a series of curl commands. Here's an example of the necessary curl command with the new custom metadata block in the "/tmp" directory.

``curl http://localhost:8080/api/admin/datasetfield/load -H "Content-type: text/tab-separated-values" -X POST --upload-file /tmp/new-metadata-block.tsv``

To create a new ResourceBundle, here are the steps to generate key=value pair for the three main sections:

#metadataBlock properties
~~~~~~~~~~~~~~~~~~~~~~~~~
metadatablock.name=(the value of **name** property from #metadatablock)

metadatablock.displayName=(the value of **displayName** property from #metadatablock)

example:

metadatablock.name=citation

metadatablock.displayName=Citation Metadata

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

Running a curl command like "load" example above should make the new custom metadata block available within the system but in order to start using the fields you must either enable it from the GUI (see "General Information" in the :doc:`/user/dataverse-management` section of the User Guide) or by running a curl command like the one below using a superuser API token. In the example below we are enabling the "journal" and "geospatial" metadata blocks for the root dataverse:

``curl -H "X-Dataverse-key:$API_TOKEN" -X POST -H "Content-type:application/json" -d "[\"journal\",\"geospatial\"]" http://localhost:8080/api/dataverses/:root/metadatablocks``

Updating the Solr Schema
~~~~~~~~~~~~~~~~~~~~~~~~

Once you have enabled a new metadata block you should be able to see the new fields in the GUI but before you can save the dataset, you must add additional fields to your Solr schema. You should run the following curl command to have Dataverse output the "field name" and "copyField" elements for all the metadata fields that have been loaded into Dataverse:

``curl http://localhost:8080/api/admin/index/solr/schema``

See the :doc:`/installation/prerequisites/` section of the Installation Guide for a suggested location on disk for the Solr schema file.

Please note that if you are going to make a pull request updating ``conf/solr/7.3.0/schema.xml`` with fields you have added, you should first load all the custom metadata blocks in ``scripts/api/data/metadatablocks`` (including ones you don't care about) to create a complete list of fields.

Reloading a Metadata Block
--------------------------

As mentioned above, changes to metadata blocks that ship with Dataverse will be made over time to improve them and release notes will sometimes instruct you to reload an existing metadata block. The syntax for reloading is the same as reloading. Here's an example with the "citation" metadata block:

``curl http://localhost:8080/api/admin/datasetfield/load -H "Content-type: text/tab-separated-values" -X POST --upload-file --upload-file citation.tsv``

Great care must be taken when reloading a metadata block. Matching is done on field names (or identifiers and then names in the case of controlled vocabulary values) so it's easy to accidentally create duplicate fields.

The ability to reload metadata blocks means that SQL update scripts don't need to be written for these changes. See also the :doc:`/developers/sql-upgrade-scripts` section of the Dev Guide.

Tips from the Dataverse Community
---------------------------------

If there are tips that you feel are omitted from this document, please open an issue at https://github.com/IQSS/dataverse/issues and consider making a pull request to make improvements. You can find this document at https://github.com/IQSS/dataverse/blob/develop/doc/sphinx-guides/source/admin/metadatacustomization.rst

Alternatively, you are welcome to request "edit" access to this "Tips for Dataverse metadata blocks from the community" Google doc: https://docs.google.com/document/d/1XpblRw0v0SvV-Bq6njlN96WyHJ7tqG0WWejqBdl7hE0/edit?usp=sharing

The thinking is that the tips can become issues and the issues can eventually be worked on as features to improve the Dataverse metadata system.

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
   "displayoncreate" was "showabovefold" in Dataverse versions before 4.3.1 (see
   `#3073 <https://github.com/IQSS/dataverse/issues/3073>`__) but parsing is
   done based on column order rather than name so this only matters to the
   person reading the TSV file.
