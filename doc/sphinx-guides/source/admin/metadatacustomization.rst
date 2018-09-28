Metadata Customization
=================================================

.. contents:: |toctitle|
	:local:

Purpose
-------

Dataverse installers can customize the dataset-level metadata that Dataverse collects, including:

-  Editing and adding metadata fields

-  Editing and adding instructional text (field label tooltips and text
   box watermarks)

-  Editing and adding controlled vocabularies

-  Changing which fields depositors must use in order to save datasets

-  Changing how saved metadata values are displayed in the UI

Background
----------

In Dataverse 4, custom metadata are no longer defined as individual
fields, as they were in Dataverse Network 3.x, but in metadata blocks.
Dataverse 4 ships with a citation metadata block, which includes
mandatory fields needed for assigning persistent IDs to datasets, and
domain specific metadata blocks.

Definitions of these blocks are transmitted to a Dataverse instance in
tab-separated value (TSV). [1]_\ :sup:`,`\  [2]_ While it is technically
possible to define more than one metadata block in a TSV file, it is
good organizational practice to define only one in each file.

The metadata block TSVs shipped with Dataverse are in `this folder in
the Dataverse github
repo <https://github.com/IQSS/dataverse/tree/68bce75a2cd2b52e47e00a2cf880497481bea59e/scripts/api/data/metadatablocks>`__.
Human-readable copies are maintained in `this Google Sheets
document <https://docs.google.com/spreadsheets/d/13HP-jI_cwLDHBetn9UKTREPJ_F4iHdAvhjmlvmYdSSw/edit#gid=0>`__.

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
| dataverseAlias        | If specified, this    | Free text             |
|                       | metadata block will   |                       |
|                       | be available only to  |                       |
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
| fieldType             | Defines the type of   | | \• None              |
|                       | content that the      | | \• Date              |
|                       | field, if not empty,  | | \• Email             |
|                       | is meant to contain.  | | \• Text              |
|                       |                       | | \• Textbox           |
|                       |                       | | \• URL               |
|                       |                       | | \• Int               |
|                       |                       | | \• Float             |
|                       |                       | | \• See Appendix_ for |
|                       |                       | | fieldtype definitions|
+-----------------------+-----------------------+------------------------+
| displayOrder          | Controls the sequence | Non-negative integer.  |
|                       | in which the fields   |                        |
|                       | are displayed, both   |                        |
|                       | for input and         |                        |
|                       | presentation.         |                        |
+-----------------------+-----------------------+------------------------+
| displayFormat         | Controls how the      | See Appendix_ for      |
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
| displayOnCreate/showA\| Designate fields that | TRUE (display during   |
| \boveFold [5]_        | should display during | creation) or FALSE     |
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
| Identifier            | A string used to      | Free text             |
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

.. _Appendix:

Appendix
--------

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
| email                             | A valid email address.            |
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

.. [1]
   https://www.iana.org/assignments/media-types/text/tab-separated-values

.. [2]
   Although the structure of the data, as you’ll see below, violates the
   “Each record must have the same number of fields” tenet of TSV

.. [3]
   https://en.wikipedia.org/wiki/CamelCase

.. [4]
   These field names are added to the Solr schema.xml and cannot be
   duplicated.

.. [5]
   Labeled “showabovefold” in Dataverse versions before 4.3.1 (see
   `#3073 <https://github.com/IQSS/dataverse/issues/3073>`__).
