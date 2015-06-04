SWORD API
=========

SWORD_ stands for "Simple Web-service Offering Repository Deposit" and is a "profile" of AtomPub (`RFC 5023`_) which is a RESTful API that allows non-Dataverse software to deposit files and metadata into a Dataverse installation. :ref:`client-libraries` are available in Python, Java, R, Ruby, and PHP.

Introduced in Dataverse Network (DVN) `3.6 <http://guides.dataverse.org/en/3.6.2/dataverse-api-main.html#data-deposit-api>`_, the SWORD API was formerly known as the "Data Deposit API" and ``data-deposit/v1`` appeared in the URLs. For backwards compatibility these URLs will continue to work (with deprecation warnings). Due to architectural changes and security improvements (especially the introduction of API tokens) in Dataverse 4.0, a few backward incompatible changes were necessarily introduced and for this reason the version has been increased to ``v1.1``. For details, see :ref:`incompatible`.

Dataverse implements most of SWORDv2_, which is specified at http://swordapp.github.io/SWORDv2-Profile/SWORDProfile.html . Please reference the `SWORDv2 specification`_ for expected HTTP status codes (i.e. 201, 204, 404, etc.), headers (i.e. "Location"), etc. For a quick introduction to SWORD, the two minute video at http://cottagelabs.com/news/intro-to-sword-2 is recommended.

As a profile of AtomPub, XML is used throughout SWORD. As of Dataverse 4.0 datasets can also be created via JSON using the "native" API.

.. _SWORD: http://en.wikipedia.org/wiki/SWORD_%28protocol%29

.. _SWORDv2: http://swordapp.org/sword-v2/sword-v2-specifications/

.. _RFC 5023: https://tools.ietf.org/html/rfc5023

.. _SWORDv2 specification: http://swordapp.github.io/SWORDv2-Profile/SWORDProfile.html

.. contents::

.. _incompatible:

Backward incompatible changes
-----------------------------

For better security, usernames and passwords are no longer accepted. The use of an API token is required.

In addition, differences in Dataverse 4.0 have lead to a few minor backward incompatible changes in the Dataverse implementation of SWORD, which are listed below. Old ``v1`` URLs should continue to work but the ``Service Document`` will contain a deprecation warning and responses will contain ``v1.1`` URLs. See also :ref:`known-issues`.

- Newly required fields when creating/editing datasets for compliance with the `Joint Declaration for Data Citation principles <http://thedata.org/blog/joint-declaration-data-citation-principles-and-dataverse>`_.

  - dcterms:creator (maps to authorName)

  - dcterms:description

- Deaccessioning is no longer supported. An alternative will be developed at https://github.com/IQSS/dataverse/issues/778

- The Service Document will show a single API Terms of Use rather than root level and dataverse level Deposit Terms of Use.

New features as of v1.1
-----------------------

- Dataverse 4.0 supports API tokens and they must be used rather that a username and password. In the ``curl`` examples below, you will see ``curl -u $API_TOKEN:`` showing that you should send your API token as the username and nothing as the password. For example, ``curl -u 54b143b5-d001-4254-afc0-a1c0f6a5b5a7:``.

- Dataverses can be published via SWORD

- Datasets versions will only be increased to the next minor version (i.e. 1.1) rather than a major version (2.0) if possible. This depends on the nature of the change.

- "Author Affiliation" can now be populated with an XML attribute. For example: <dcterms:creator affiliation="Coffee Bean State University">Stumptown, Jane</dcterms:creator>

- "Contributor" can now be populated and the "Type" (Editor, Funder, Researcher, etc.) can be specified with an XML attribute. For example: <dcterms:contributor type="Funder">CaffeineForAll</dcterms:contributor>

- "License" can now be set with dcterms:license and the possible values are "CC0" and "NONE". "License" interacts with "Terms of Use" (dcterms:rights) in that if you include dcterms:rights in the XML, the license will be set to "NONE". If you don't include dcterms:rights, the license will default to "CC0". It is invalid to specify "CC0" as a license and also include dcterms:rights; an error will be returned. For backwards compatibility, dcterms:rights is allowed to be blank (i.e. <dcterms:rights></dcterms:rights>) but blank values will not be persisted to the database and the license will be set to "NONE".

- "Contact E-mail" is automatically populated from dataset owners email.

- "Subject" is automatically populated with "N/A".

- Zero-length files are now allowed (but not necessarily encouraged).

- "Depositor" and "Deposit Date" are auto-populated.

curl examples
-------------

Retrieve SWORD service document
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The service document enumerates the dataverses ("collections" from a SWORD perspective) the user can deposit data into. The "collectionPolicy" element for each dataverse contains the Terms of Use.

``curl -u $API_TOKEN: https://$HOSTNAME/dvn/api/data-deposit/v1.1/swordv2/service-document``

Create a dataset with an Atom entry
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

``curl -u $API_TOKEN: --data-binary "@path/to/atom-entry-study.xml" -H "Content-Type: application/atom+xml" https://$HOSTNAME/dvn/api/data-deposit/v1.1/swordv2/collection/dataverse/$DATAVERSE_ALIAS``

Example Atom entry (XML)

.. literalinclude:: sword-atom-entry.xml

Dublin Core Terms (DC Terms) Qualified Mapping - Dataverse DB Element Crosswalk
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

+-----------------------------+----------------------------------------------+--------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------+
|DC (terms: namespace)        |                Dataverse DB Element          |   Required   |                                                                     Note                                                                                    |
+=============================+==============================================+==============+=============================================================================================================================================================+
|dcterms:title                |                    title                     |       Y      |  Title of the Dataset.                                                                                                                                      |
+-----------------------------+----------------------------------------------+--------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------+
|dcterms:creator              |         authorName (LastName, FirstName)     |       Y      |  Author(s) for the Dataset.                                                                                                                                 |
+-----------------------------+----------------------------------------------+--------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------+
|dcterms:subject              |   subject (Controlled Vocabulary) OR keyword |       Y      |  Controlled Vocabulary list is in our User Guide > `Metadata References <../user/appendix.html#metadata-references>`_.   |                                                                                                                
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

``curl -u $API_TOKEN: https://$HOSTNAME/dvn/api/data-deposit/v1.1/swordv2/collection/dataverse/$DATAVERSE_ALIAS``

Add files to a dataset with a zip file
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

``curl -u $API_TOKEN: --data-binary @path/to/example.zip -H "Content-Disposition: filename=example.zip" -H "Content-Type: application/zip" -H "Packaging: http://purl.org/net/sword/package/SimpleZip" https://$HOSTNAME/dvn/api/data-deposit/v1.1/swordv2/edit-media/study/doi:TEST/12345``

Display a dataset atom entry
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Contains data citation (bibliographicCitation), alternate URI (persistent URI of study), edit URI, edit media URI, statement URI.

``curl -u $API_TOKEN: https://$HOSTNAME/dvn/api/data-deposit/v1.1/swordv2/edit/study/doi:TEST/12345``

Display a dataset statement
~~~~~~~~~~~~~~~~~~~~~~~~~~~

Contains title, author, feed of file entries, latestVersionState, locked boolean, updated timestamp.

``curl -u $API_TOKEN: https://$HOSTNAME/dvn/api/data-deposit/v1.1/swordv2/statement/study/doi:TEST/12345``

Delete a file by database id
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

``curl -u $API_TOKEN: -X DELETE https://$HOSTNAME/dvn/api/data-deposit/v1.1/swordv2/edit-media/file/123``

Replacing metadata for a dataset
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Please note that **ALL** metadata (title, author, etc.) will be replaced, including fields that can not be expressed with "dcterms" fields.

``curl -u $API_TOKEN: --upload-file "path/to/atom-entry-study2.xml" -H "Content-Type: application/atom+xml" https://$HOSTNAME/dvn/api/data-deposit/v1.1/swordv2/edit/study/doi:TEST/12345``

Delete a dataset
~~~~~~~~~~~~~~~~

``curl -u $API_TOKEN: -i -X DELETE https://$HOSTNAME/dvn/api/data-deposit/v1.1/swordv2/edit/study/doi:TEST/12345``

Determine if a dataverse has been published
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Look for a `dataverseHasBeenReleased` boolean.

``curl -u $API_TOKEN: https://$HOSTNAME/dvn/api/data-deposit/v1.1/swordv2/collection/dataverse/$DATAVERSE_ALIAS``

Publish a dataverse
~~~~~~~~~~~~~~~~~~~

The ``cat /dev/null`` and ``--data-binary @-`` arguments are used to send zero-length content to the API, which is required by the upstream library to process the ``In-Progress: false`` header.

``cat /dev/null | curl -u $API_TOKEN: -X POST -H "In-Progress: false" --data-binary @- https://$HOSTNAME/dvn/api/data-deposit/v1.1/swordv2/edit/dataverse/$DATAVERSE_ALIAS``

Publish a dataset
~~~~~~~~~~~~~~~~~

The ``cat /dev/null`` and ``--data-binary @-`` arguments are used to send zero-length content to the API, which is required by the upstream library to process the ``In-Progress: false`` header.

``cat /dev/null | curl -u $API_TOKEN: -X POST -H "In-Progress: false" --data-binary @- https://$HOSTNAME/dvn/api/data-deposit/v1.1/swordv2/edit/study/doi:TEST/12345``

.. _known-issues:

Known issues
------------

- Potential mismatch between the dataverses ("collections" from a SWORD perspective) the user can deposit data into in returned by the Service Document and which dataverses the user can actually deposit data into. This is due to an incomplete transition from the old DVN 3.x "admin-only" style permission checking to the new permissions system in Dataverse 4.0 ( https://github.com/IQSS/dataverse/issues/1070 ). The mismatch was reported at https://github.com/IQSS/dataverse/issues/1443

- Should see all the fields filled in for a dataset regardless of what the parent dataverse specifies: https://github.com/IQSS/dataverse/issues/756

- Inefficiency in constructing the ``Service Document``: https://github.com/IQSS/dataverse/issues/784

- Inefficiency in constructing the list of datasets: https://github.com/IQSS/dataverse/issues/784

Roadmap
-------

These are features we'd like to add in the future:

- Implement SWORD 2.0 Profile 6.4: https://github.com/IQSS/dataverse/issues/183

- Support deaccessioning via API: https://github.com/IQSS/dataverse/issues/778

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
- R: https://github.com/ropensci/dvn
- Ruby: https://github.com/swordapp/sword2ruby
- PHP: https://github.com/swordapp/swordappv2-php-library

