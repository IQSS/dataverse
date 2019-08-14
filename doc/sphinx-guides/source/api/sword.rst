SWORD API
=========

SWORD_ stands for "Simple Web-service Offering Repository Deposit" and is a "profile" of AtomPub (`RFC 5023`_) which is a RESTful API that allows non-Dataverse software to deposit files and metadata into a Dataverse installation. :ref:`client-libraries` are available in Python, Java, R, Ruby, and PHP.

.. contents:: |toctitle|
    :local:

About
-----

Introduced in Dataverse Network (DVN) `3.6 <http://guides.dataverse.org/en/3.6.2/dataverse-api-main.html#data-deposit-api>`_, the SWORD API was formerly known as the "Data Deposit API" and ``data-deposit/v1`` appeared in the URLs. For backwards compatibility these URLs continue to work (with deprecation warnings). Due to architectural changes and security improvements (especially the introduction of API tokens) in Dataverse 4.0, a few backward incompatible changes were necessarily introduced and for this reason the version has been increased to ``v1.1``. For details, see :ref:`incompatible`.

Dataverse implements most of SWORDv2_, which is specified at http://swordapp.github.io/SWORDv2-Profile/SWORDProfile.html . Please reference the `SWORDv2 specification`_ for expected HTTP status codes (i.e. 201, 204, 404, etc.), headers (i.e. "Location"), etc.

As a profile of AtomPub, XML is used throughout SWORD. As of Dataverse 4.0 datasets can also be created via JSON using the "native" API. SWORD is limited to the dozen or so fields listed below in the crosswalk, but the native API allows you to populate all metadata fields available in Dataverse.

.. _SWORD: http://en.wikipedia.org/wiki/SWORD_%28protocol%29

.. _SWORDv2: http://swordapp.org/sword-v2/sword-v2-specifications/

.. _RFC 5023: https://tools.ietf.org/html/rfc5023

.. _SWORDv2 specification: http://swordapp.github.io/SWORDv2-Profile/SWORDProfile.html

.. _incompatible:

Backward incompatible changes
-----------------------------

For better security than in DVN 3.x, usernames and passwords are no longer accepted. The use of an API token is required.

Differences in Dataverse 4 from DVN 3.x lead to a few minor backward incompatible changes in the Dataverse implementation of SWORD, which are listed below. Old ``v1`` URLs should continue to work but the ``Service Document`` will contain a deprecation warning and responses will contain ``v1.1`` URLs. See also :ref:`known-issues`.

- Newly required fields when creating/editing datasets for compliance with the `Joint Declaration for Data Citation principles <http://thedata.org/blog/joint-declaration-data-citation-principles-and-dataverse>`_.

  - dcterms:creator (maps to authorName)

  - dcterms:description

- Deaccessioning is no longer supported. An alternative will be developed at https://github.com/IQSS/dataverse/issues/778

- The Service Document will show a single API Terms of Use rather than root level and dataverse level Deposit Terms of Use.

New features as of v1.1
-----------------------

- Dataverse 4 supports API tokens and requires them to be used for APIs instead of a username and password. In the ``curl`` examples below, you will see ``curl -u $API_TOKEN:`` showing that you should send your API token as the username and nothing as the password. For example, ``curl -u 54b143b5-d001-4254-afc0-a1c0f6a5b5a7:``.

- SWORD operations no longer require "admin" permission. In order to use any SWORD operation in DVN 3.x, you had to be an "admin" on a dataverse (the container for your dataset) and similar rules were applied in Dataverse 4.4 and earlier (the ``EditDataverse`` permission was required). The SWORD API has now been fully integrated with the Dataverse 4 permission model such that any action you have permission to perform in the GUI or "native" API you are able to perform via SWORD. This means that even a user with a "Contributor" role can operate on datasets via SWORD. Note that users with the "Contributor" role do not have the ``PublishDataset`` permission and will not be able publish their datasets via any mechanism, GUI or API.

- Dataverses can be published via SWORD.

- Datasets versions will only be increased to the next minor version (i.e. 1.1) rather than a major version (2.0) if possible. This depends on the nature of the change. Adding or removing a file, for example, requires a major version bump.

- "Author Affiliation" can now be populated with an XML attribute. For example: <dcterms:creator affiliation="Coffee Bean State University">Stumptown, Jane</dcterms:creator>

- "Contributor" can now be populated and the "Type" (Editor, Funder, Researcher, etc.) can be specified with an XML attribute. For example: <dcterms:contributor type="Funder">CaffeineForAll</dcterms:contributor>

- "License" can now be set with dcterms:license and the possible values are "CC0" and "NONE". "License" interacts with "Terms of Use" (dcterms:rights) in that if you include dcterms:rights in the XML, the license will be set to "NONE". If you don't include dcterms:rights, the license will default to "CC0". It is invalid to specify "CC0" as a license and also include dcterms:rights; an error will be returned. For backwards compatibility, dcterms:rights is allowed to be blank (i.e. <dcterms:rights></dcterms:rights>) but blank values will not be persisted to the database and the license will be set to "NONE".

- "Contact E-mail" is automatically populated from dataset owner's email.

- "Subject" uses our controlled vocabulary list of subjects. This list is in the Citation Metadata of our User Guide > `Metadata References <http://guides.dataverse.org/en/latest/user/appendix.html#metadata-references>`_. Otherwise, if a term does not match our controlled vocabulary list, it will put any subject terms in "Keyword". If Subject is empty it is automatically populated with "N/A".

- Zero-length files are now allowed (but not necessarily encouraged).

- "Depositor" and "Deposit Date" are auto-populated.

curl examples
-------------

Retrieve SWORD service document
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The service document enumerates the dataverses ("collections" from a SWORD perspective) the user can deposit data into. The "collectionPolicy" element for each dataverse contains the Terms of Use. Any user with an API token can use this API endpoint. Institution-wide Shibboleth groups are not respected because membership in such a group can only be set via a browser.

``curl -u $API_TOKEN: https://$HOSTNAME/dvn/api/data-deposit/v1.1/swordv2/service-document``

Create a dataset with an Atom entry
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

To create a dataset, you must have the "Dataset Creator" role (the ``AddDataset`` permission) on a dataverse. Practically speaking, you should first retrieve the service document to list the dataverses into which you are authorized to deposit data.

``curl -u $API_TOKEN: --data-binary "@path/to/atom-entry-study.xml" -H "Content-Type: application/atom+xml" https://$HOSTNAME/dvn/api/data-deposit/v1.1/swordv2/collection/dataverse/$DATAVERSE_ALIAS``

Example Atom entry (XML)

.. literalinclude:: sword-atom-entry.xml

Dublin Core Terms (DC Terms) Qualified Mapping - Dataverse DB Element Crosswalk
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
+-----------------------------+----------------------------------------------+--------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------+
|DC (terms: namespace)        |                Dataverse DB Element          |   Required   |                                                                     Note                                                                                    |
+=============================+==============================================+==============+=============================================================================================================================================================+
|dcterms:title                |                    title                     |       Y      |  Title of the Dataset.                                                                                                                                      |
+-----------------------------+----------------------------------------------+--------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------+
|dcterms:creator              |         authorName (LastName, FirstName)     |       Y      |  Author(s) for the Dataset.                                                                                                                                 |
+-----------------------------+----------------------------------------------+--------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------+
|dcterms:subject              |   subject (Controlled Vocabulary) OR keyword |       Y      |  Controlled Vocabulary list is in our User Guide > `Metadata References <http://guides.dataverse.org/en/latest/user/appendix.html#metadata-references>`_.   |                                                                                                                
+-----------------------------+----------------------------------------------+--------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------+
|dcterms:description          |              dsDescriptionValue              |       Y      |  Describing the purpose, scope or nature of the Dataset. Can also use dcterms:abstract.                                                                     |
+-----------------------------+----------------------------------------------+--------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------+
|dcterms:publisher            |                 producerName                 |              |  Person or agency financially or administratively responsible for the Dataset                                                                               |
+-----------------------------+----------------------------------------------+--------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------+
|dcterms:contributor          |               datasetContactEmail            |       Y      |  Contact Email is required so will need to add an attribute type="Contact". Also used for Funder: add attribute type="Funder" which maps to contributorName.|                                                                                                                 
+-----------------------------+----------------------------------------------+--------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------+
|dcterms:date                 |productionDate (YYYY-MM-DD or YYYY-MM or YYYY)|              |  Production date of Dataset.                                                                                                                                |
+-----------------------------+----------------------------------------------+--------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------+
|dcterms:type                 |                  kindOfData                  |              |  Type of data included in the file: survey data, census/enumeration data, aggregate data, clinical.                                                         |
+-----------------------------+----------------------------------------------+--------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------+
|dcterms:source               |                 dataSources                  |              |  List of books, articles, data files if any that served as the sources for the Dataset.                                                                     |
+-----------------------------+----------------------------------------------+--------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------+
|dcterms:relation             |               relatedMaterial                |              |  Any related material (journal article citation is not included here - see: dcterms:isReferencedBy below).                                                  |
+-----------------------------+----------------------------------------------+--------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------+
|dcterms:coverage             |              otherGeographicCoverage         |              |  General information on the geographic coverage of the Dataset.                                                                                             |
+-----------------------------+----------------------------------------------+--------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------+
|dcterms:license              |                   license                    |              |  Set the license to CC0 (default in Dataverse for new Datasets), otherwise enter "NONE" and fill in the dcterms:rights field.                               |                
+-----------------------------+----------------------------------------------+--------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------+
|dcterms:rights               |                 termsofuse                   |              |  If not using CC0, enter any terms of use or restrictions for the Dataset.                                                                                  |
+-----------------------------+----------------------------------------------+--------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------+
|dcterms:isReferencedBy       |             publicationCitation              |              |  The publication (journal article, book, other work) that uses this dataset (include citation, permanent identifier (DOI), and permanent URL).              |
+-----------------------------+----------------------------------------------+--------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------+

List datasets in a dataverse
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

You must have permission to add datasets in a dataverse (the dataverse should appear in the service document) to list the datasets inside. Institution-wide Shibboleth groups are not respected because membership in such a group can only be set via a browser.

``curl -u $API_TOKEN: https://$HOSTNAME/dvn/api/data-deposit/v1.1/swordv2/collection/dataverse/$DATAVERSE_ALIAS``

Add files to a dataset with a zip file
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

You must have ``EditDataset`` permission (Contributor role or above such as Curator or Admin) on the dataset to add files.

``curl -u $API_TOKEN: --data-binary @path/to/example.zip -H "Content-Disposition: filename=example.zip" -H "Content-Type: application/zip" -H "Packaging: http://purl.org/net/sword/package/SimpleZip" https://$HOSTNAME/dvn/api/data-deposit/v1.1/swordv2/edit-media/study/doi:TEST/12345``

Display a dataset atom entry
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

You must have ``ViewUnpublishedDataset`` permission (Contributor role or above such as Curator or Admin) on the dataset to view its Atom entry.

Contains data citation (bibliographicCitation), alternate URI (persistent URI of study), edit URI, edit media URI, statement URI.

``curl -u $API_TOKEN: https://$HOSTNAME/dvn/api/data-deposit/v1.1/swordv2/edit/study/doi:TEST/12345``

Display a dataset statement
~~~~~~~~~~~~~~~~~~~~~~~~~~~

Contains title, author, feed of file entries, latestVersionState, locked boolean, updated timestamp. You must have ``ViewUnpublishedDataset`` permission (Contributor role or above such as Curator or Admin) on the dataset to display the statement.

``curl -u $API_TOKEN: https://$HOSTNAME/dvn/api/data-deposit/v1.1/swordv2/statement/study/doi:TEST/12345``

Delete a file by database id
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

You must have ``EditDataset`` permission (Contributor role or above such as Curator or Admin) on the dataset to delete files.

``curl -u $API_TOKEN: -X DELETE https://$HOSTNAME/dvn/api/data-deposit/v1.1/swordv2/edit-media/file/123``

Replacing metadata for a dataset
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Please note that **ALL** metadata (title, author, etc.) will be replaced, including fields that can not be expressed with "dcterms" fields. You must have ``EditDataset`` permission (Contributor role or above such as Curator or Admin) on the dataset to replace metadata.

``curl -u $API_TOKEN: --upload-file "path/to/atom-entry-study2.xml" -H "Content-Type: application/atom+xml" https://$HOSTNAME/dvn/api/data-deposit/v1.1/swordv2/edit/study/doi:TEST/12345``

Delete a dataset
~~~~~~~~~~~~~~~~

You must have the ``DeleteDatasetDraft`` permission (Contributor role or above such as Curator or Admin) on the dataset to delete it. Please note that if the dataset has never been published you will be able to delete it completely but if the dataset has already been published you will only be able to delete post-publication drafts, never a published version.

``curl -u $API_TOKEN: -i -X DELETE https://$HOSTNAME/dvn/api/data-deposit/v1.1/swordv2/edit/study/doi:TEST/12345``

Determine if a dataverse has been published
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This API endpoint is the same as the "list datasets in a dataverse" endpoint documented above and the same permissions apply but it is documented here separately to point out that you can look for a boolean called ``dataverseHasBeenReleased`` to know if a dataverse has been released, which is required for publishing a dataset.

``curl -u $API_TOKEN: https://$HOSTNAME/dvn/api/data-deposit/v1.1/swordv2/collection/dataverse/$DATAVERSE_ALIAS``

Publish a dataverse
~~~~~~~~~~~~~~~~~~~

The ``cat /dev/null`` and ``--data-binary @-`` arguments are used to send zero-length content to the API, which is required by the upstream library to process the ``In-Progress: false`` header. You must have the ``PublishDataverse`` permission (Admin role) on the dataverse to publish it.

``cat /dev/null | curl -u $API_TOKEN: -X POST -H "In-Progress: false" --data-binary @- https://$HOSTNAME/dvn/api/data-deposit/v1.1/swordv2/edit/dataverse/$DATAVERSE_ALIAS``

Publish a dataset
~~~~~~~~~~~~~~~~~

The ``cat /dev/null`` and ``--data-binary @-`` arguments are used to send zero-length content to the API, which is required by the upstream library to process the ``In-Progress: false`` header. You must have the ``PublishDataset`` permission (Curator or Admin role) on the dataset to publish it.

``cat /dev/null | curl -u $API_TOKEN: -X POST -H "In-Progress: false" --data-binary @- https://$HOSTNAME/dvn/api/data-deposit/v1.1/swordv2/edit/study/doi:TEST/12345``

.. _known-issues:

Known issues
------------

- Deleting a file from a published version (not a draft) creates a draft but doesn't delete the file: https://github.com/IQSS/dataverse/issues/2464

- The Service Document does not honor groups within groups: https://github.com/IQSS/dataverse/issues/3056

- Should see all the fields filled in for a dataset regardless of what the parent dataverse specifies: https://github.com/IQSS/dataverse/issues/756

- SWORD 2.0 Profile 6.4 "Retrieving the content" has not been implemented: https://github.com/IQSS/dataverse/issues/183

- Deaccessioning via API is not supported (it was in DVN 3.x): https://github.com/IQSS/dataverse/issues/778

- Let file metadata (i.e. description) be specified during zip upload: https://github.com/IQSS/dataverse/issues/723

- SWORD: Display of actual dcterms xml element for equivalent of required field not found: https://github.com/IQSS/dataverse/issues/1019

Bug fixes in v1.1
-----------------

- Fix Abdera ArrayIndexOutOfBoundsException with non-existent atom-entry-study.xml in SWORD jar (upstream ideally) https://github.com/IQSS/dataverse/issues/893 

- Sword API: Can't create study when hidden characters are introduced in atom.xml https://github.com/IQSS/dataverse/issues/894

.. _client-libraries:

Client libraries
----------------

- Python: https://github.com/swordapp/python-client-sword2
- Java: https://github.com/swordapp/JavaClient2.0
- R: https://github.com/IQSS/dataverse-client-r
- Ruby: https://github.com/swordapp/sword2ruby
- PHP: https://github.com/swordapp/swordappv2-php-library

