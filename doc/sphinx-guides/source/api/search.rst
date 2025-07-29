Search API
==========

.. contents:: |toctitle|
    :local:

The Search API supports the same searching, sorting, and faceting operations as the Dataverse Software's web interface.

To search unpublished content, you must pass in an API token as described in the :doc:`auth` section.

The parameters and JSON response are partly inspired by the `GitHub Search API <https://developer.github.com/v3/search/>`_.

.. note:: |CORS| The search API can be used from scripts running in web browsers, as it allows cross-origin resource sharing (CORS).

.. _CORS: https://www.w3.org/TR/cors/


Please note that in Dataverse Software 4.3 and older the "citation" field wrapped the persistent ID URL in an ``<a>`` tag but this has been changed to plaintext. If you want the old value with HTML in it, a new field called "citationHtml" can be used.


Parameters
----------

================ =======  ===========
Name             Type     Description
================ =======  ===========
q                string   The search term or terms. Using "title:data" will search only the "title" field. "*" can be used as a wildcard either alone or adjacent to a term (i.e. "bird*"). For example, https://demo.dataverse.org/api/search?q=title:data . For a list of fields to search, please see https://github.com/IQSS/dataverse/issues/2558 (for now).
type             string   Can be either "dataverse", "dataset", or "file". Multiple "type" parameters can be used to include multiple types (i.e. ``type=dataset&type=file``). If omitted, all types will be returned.  For example, https://demo.dataverse.org/api/search?q=*&type=dataset
subtree          string   The identifier of the Dataverse collection to which the search should be narrowed. The subtree of this Dataverse collection and all its children will be searched.  Multiple "subtree" parameters can be used to include multiple Dataverse collections. For example, https://demo.dataverse.org/api/search?q=data&subtree=birds&subtree=cats .
sort             string   The sort field. Supported values include "name" and "date". See example under "order".
order            string   The order in which to sort. Can either be "asc" or "desc".  For example, https://demo.dataverse.org/api/search?q=data&sort=name&order=asc
per_page         int      The number of results to return per request. The default is 10. The max is 1000. See :ref:`iteration example <iteration-example>`.
start            int      A cursor for paging through search results. See :ref:`iteration example <iteration-example>`.
show_relevance   boolean  Whether or not to show details of which fields were matched by the query. False by default. See :ref:`advanced search example <advancedsearch-example>`.
show_facets      boolean  Whether or not to show facets that can be operated on by the "fq" parameter. False by default. See :ref:`advanced search example <advancedsearch-example>`.
fq               string   A filter query on the search term. Multiple "fq" parameters can be used. See :ref:`advanced search example <advancedsearch-example>`.
show_entity_ids  boolean  Whether or not to show the database IDs of the search results (for developer use).
show_api_urls    boolean  Whether or not to show API URLs for the search results
query_entities   boolean  Whether to query entities for extra metadata (slower). Default is true.
metadata_fields  string   Includes the requested fields for each dataset in the response. Multiple "metadata_fields" parameters can be used to include several fields. The value must be in the form "{metadata_block_name}:{field_name}" to include a specific field from a metadata block (see :ref:`example <dynamic-citation-some>`) or "{metadata_field_set_name}:\*" to include all the fields for a metadata block (see :ref:`example <dynamic-citation-all>`). "{field_name}" cannot be a subfield of a compound field. If "{field_name}" is a compound field, all subfields are included.
geo_point        string   Latitude and longitude in the form ``geo_point=42.3,-71.1``. You must supply ``geo_radius`` as well. See also :ref:`geospatial-search`.
geo_radius       string   Radial distance in kilometers from ``geo_point`` (which must be supplied as well) such as ``geo_radius=1.5``.
show_type_counts boolean  Whether or not to include total_count_per_object_type for types: Dataverse, Dataset, and Files.
show_collections boolean  Whether or not to include a list of Dataverse collections for each dataset search result.
search_service   string   The name of the search service to use for this query. If omitted, the default search service will be used. For available search services, see :ref:`discovering-available-search-services`.
================ =======  ===========

Basic Search Example
--------------------

https://demo.dataverse.org/api/search?q=trees

.. code-block:: json

    {
        "status":"OK",
        "data":{
            "q":"trees",
            "total_count":5,
            "start":0,
            "spelling_alternatives":{
                "trees":"[tree]"
            },
            "items":[
                {
                    "name":"Trees",
                    "type":"dataverse",
                    "url":"https://demo.dataverse.org/dataverse/trees",
                    "image_url":"https://demo.dataverse.org/api/access/dvCardImage/1",
                    "identifier":"trees",
                    "description":"A tree dataverse with some birds",
                    "published_at":"2016-05-10T12:53:38Z",
                    "publicationStatuses": [
                        "Published"
                    ],
                    "affiliation": "Dataverse.org",
                    "parentDataverseName": "Root",
                    "parentDataverseIdentifier": "root"
                },
                {
                    "name":"Chestnut Trees",
                    "type":"dataverse",
                    "url":"https://demo.dataverse.org/dataverse/chestnuttrees",
                    "image_url":"https://demo.dataverse.org/api/access/dvCardImage/2",
                    "identifier":"chestnuttrees",
                    "description":"A dataverse with chestnut trees and an oriole",
                    "published_at":"2016-05-10T12:52:38Z",
                    "publicationStatuses": [
                        "Published"
                    ],
                    "affiliation": "Dataverse.org",
                    "parentDataverseName": "Root",
                    "parentDataverseIdentifier": "root"
                },
                {
                    "name":"trees.png",
                    "type":"file",
                    "url":"https://demo.dataverse.org/api/access/datafile/12",
                    "image_url":"https://demo.dataverse.org/api/access/datafile/12?imageThumb=true",
                    "file_id":"12",
                    "description":"",
                    "published_at":"2016-05-10T12:53:39Z",
                    "file_type":"PNG Image",
                    "file_content_type":"image/png",
                    "size_in_bytes":8361,
                    "md5":"0386269a5acb2c57b4eade587ff4db64",
                    "file_persistent_id": "doi:10.5072/FK2/XTT5BV/PCCHV7",
                    "dataset_name": "Dataset One",
                    "dataset_id": "32",
                    "dataset_persistent_id": "doi:10.5072/FK2/XTT5BV",
                    "dataset_citation":"Spruce, Sabrina, 2016, \"Spruce Goose\", http://dx.doi.org/10.5072/FK2/XTT5BV, Root Dataverse, V1",
                    "publicationStatuses": [
                        "Published"
                    ],
                    "releaseOrCreateDate": "2016-05-10T12:53:39Z"
                },
                {
                    "name":"Birds",
                    "type":"dataverse",
                    "url":"https://demo.dataverse.org/dataverse/birds",
                    "image_url":"https://demo.dataverse.org/api/access/dvCardImage/3",
                    "identifier":"birds",
                    "description":"A bird Dataverse collection with some trees",
                    "published_at":"2016-05-10T12:57:27Z",
                    "publicationStatuses": [
                        "Published"
                    ],
                    "affiliation": "Dataverse.org",
                    "parentDataverseName": "Root",
                    "parentDataverseIdentifier": "root"
                },
                {  
                    "name":"Darwin's Finches",
                    "type":"dataset",
                    "url":"https://doi.org/10.70122/FK2/MB5VGR",
                    "global_id":"doi:10.70122/FK2/MB5VGR",
                    "description":"Darwin's finches (also known as the GalÃ¡pagos finches) are a group of about fifteen species of passerine birds.",
                    "published_at":"2019-12-11T15:26:10Z",
                    "publisher":"dvbe69f5e1",
                    "citationHtml":"Finch, Fiona; Spruce, Sabrina; Poe, Edgar Allen; Mulligan, Hercules, 2019, \"Darwin's Finches\", <a href=\"https://doi.org/10.70122/FK2/MB5VGR\" target=\"_blank\">https://doi.org/10.70122/FK2/MB5VGR</a>, Root, V3",
                    "identifier_of_dataverse":"dvbe69f5e1",
                    "name_of_dataverse":"dvbe69f5e1",
                    "citation":"Finch, Fiona; Spruce, Sabrina; Poe, Edgar Allen; Mulligan, Hercules, 2019, \"Darwin's Finches\", https://doi.org/10.70122/FK2/MB5VGR, Root, V3",
                    "publicationStatuses": [
                        "Published"
                    ],
                    "storageIdentifier":"file://10.70122/FK2/MB5VGR",
                    "subjects":[  
                       "Astronomy and Astrophysics",
                       "Other"
                    ],
                    "fileCount":3,
                    "versionId":1260,
                    "versionState":"RELEASED",
                    "majorVersion":3,
                    "minorVersion":0,
                    "createdAt":"2019-09-20T18:08:29Z",
                    "updatedAt":"2019-12-11T15:26:10Z",
                    "contacts":[  
                       {  
                          "name":"Finch, Fiona",
                          "affiliation":""
                       }
                    ],
                    "producers":[  
                       "Allen, Irwin",
                       "Spielberg, Stephen"
                    ],
                    "authors":[  
                       "Finch, Fiona",
                       "Spruce, Sabrina",
                       "Poe, Edgar Allen",
                       "Mulligan, Hercules"
                    ]
                 }
            ],
            "count_in_response":5
        }
    }

.. _advancedsearch-example:

Advanced Search Examples
------------------------

Narrowed to Collection, Show Relevance and Facets
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

https://demo.dataverse.org/api/search?q=finch&show_relevance=true&show_facets=true&fq=publicationDate:2016&subtree=birds

In this example, ``show_relevance=true`` matches per field are shown. Available facets are shown with ``show_facets=true`` and of the facets is being used with ``fq=publicationDate:2016``. The search is being narrowed to the Dataverse collection with the identifier "birds" with the parameter ``subtree=birds``.

.. code-block:: json

    {
        "status":"OK",
        "data":{
            "q":"finch",
            "total_count":2,
            "start":0,
            "spelling_alternatives":{
            },
            "items":[
                {
                    "name":"Finches",
                    "type":"dataverse",
                    "url":"https://demo.dataverse.org/dataverse/finches",
                    "image_url":"https://demo.dataverse.org/api/access/dvCardImage/2",
                    "identifier":"finches",
                    "description":"A Dataverse collection with finches",
                    "published_at":"2016-05-10T12:57:38Z",
                    "matches":[
                        {
                            "description":{
                                "snippets":[
                                    "A Dataverse collection with <span class=\"search-term-match\">finches</span>"
                                ]
                            }
                        },
                        {
                            "name":{
                                "snippets":[
                                    "<span class=\"search-term-match\">Finches</span>"
                                ]
                            }
                        }
                    ],
                    "score": 3.8500118255615234
                },
                {
                    "name":"Darwin's Finches",
                    "type":"dataset",
                    "url":"http://dx.doi.org/10.5072/FK2/G2VPE7",
                    "image_url":"https://demo.dataverse.org/api/access/dsCardImage/2",
                    "global_id":"doi:10.5072/FK2/G2VPE7",
                    "description": "Darwin's finches (also known as the Galápagos finches) are a group of about fifteen species of passerine birds.",
                    "published_at":"2016-05-10T12:57:45Z",
                    "citationHtml":"Finch, Fiona, 2016, \"Darwin's Finches\", <a href=\"http://dx.doi.org/10.5072/FK2/G2VPE7\" target=\"_blank\">http://dx.doi.org/10.5072/FK2/G2VPE7</a>, Root Dataverse, V1",
                    "citation":"Finch, Fiona, 2016, \"Darwin's Finches\", http://dx.doi.org/10.5072/FK2/G2VPE7, Root Dataverse, V1",
                    "publicationStatuses": [
                        "Published"
                    ],
                    "matches":[
                        {
                            "authorName":{
                                "snippets":[
                                    "<span class=\"search-term-match\">Finch</span>, Fiona"
                                ]
                            }
                        },
                        {
                            "dsDescriptionValue":{
                                "snippets":[
                                    "Darwin's <span class=\"search-term-match\">finches</span> (also known as the Galápagos <span class=\"search-term-match\">finches</span>) are a group of about fifteen species"
                                ]
                            }
                        },
                        {
                            "title":{
                                "snippets":[
                                    "Darwin's <span class=\"search-term-match\">Finches</span>"
                                ]
                            }
                        }
                    ],
                    "score": 1.5033848285675049,
                    "authors":[
                        "Finch, Fiona"
                    ]
                }
            ],
            "facets":[
                {
                    "subject_ss":{
                        "friendly":"Subject",
                        "labels":[
                            {
                                "Medicine, Health and Life Sciences":2
                            }
                        ]
                    },
                    "authorName_ss": {
                        "friendly":"Author Name",
                        "labels": [
                            {
                                "Finch, Fiona":1
                            }
                        ]
                    },
                    "publicationDate":{
                        "friendly":"Publication Date",
                        "labels":[
                            {
                                "2016":2
                            }
                        ]
                    }
                }
            ],
            "count_in_response":2
        }
    }

Retrieve Released Versions Only
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

https://demo.dataverse.org/api/search?q=finch&fq=publicationStatus:Published&type=dataset

The above example ``fq=publicationStatus:Published`` retrieves only "RELEASED" versions of datasets. The same could be done to retrieve "DRAFT" versions, ``fq=publicationStatus:Draft``

.. code-block:: json

    {
        "status": "OK",
        "data": {
            "q": "finch",
            "total_count": 2,
            "start": 0,
            "spelling_alternatives": {},
            "items": [
                {
                    "name": "Darwin's Finches",
                    "type": "dataset",
                    "url": "https://doi.org/10.70122/FK2/GUAS41",
                    "global_id": "doi:10.70122/FK2/GUAS41",
                    "description": "Darwin's finches (also known as the Galápagos finches) are a group of about fifteen species of passerine birds.",
                    "published_at": "2019-12-24T08:05:02Z",
                    "publisher": "mdmizanur rahman Dataverse collection",
                    "citationHtml": "Finch, Fiona, 2019, \"Darwin's Finches\", <a href=\"https://doi.org/10.70122/FK2/GUAS41\" target=\"_blank\">https://doi.org/10.70122/FK2/GUAS41</a>, Demo Dataverse, V1",
                    "identifier_of_dataverse": "rahman",
                    "name_of_dataverse": "mdmizanur rahman Dataverse collection",
                    "citation": "Finch, Fiona, 2019, \"Darwin's Finches\", https://doi.org/10.70122/FK2/GUAS41, Demo Dataverse, V1",
                    "publicationStatuses": [
                        "Published"
                    ],
                    "storageIdentifier": "file://10.70122/FK2/GUAS41",
                    "subjects": [
                        "Medicine, Health and Life Sciences"
                    ],
                    "fileCount":6,
                    "versionId": 53001,
                    "versionState": "RELEASED",
                    "majorVersion": 1,
                    "minorVersion": 0,
                    "createdAt": "2019-12-05T09:18:30Z",
                    "updatedAt": "2019-12-24T08:38:00Z",
                    "contacts": [
                        {
                            "name": "Finch, Fiona",
                            "affiliation": ""
                        }
                    ],
                    "authors": [
                        "Finch, Fiona"
                    ]
                },
                {
                    "name": "Darwin's Finches",
                    "type": "dataset",
                    "url": "https://doi.org/10.70122/FK2/7ZXYRH",
                    "global_id": "doi:10.70122/FK2/7ZXYRH",
                    "description": "Darwin's finches (also known as the Galápagos finches) are a group of about fifteen species of passerine birds.",
                    "published_at": "2020-01-22T21:47:34Z",
                    "publisher": "Demo Dataverse",
                    "citationHtml": "Finch, Fiona, 2020, \"Darwin's Finches\", <a href=\"https://doi.org/10.70122/FK2/7ZXYRH\" target=\"_blank\">https://doi.org/10.70122/FK2/7ZXYRH</a>, Demo Dataverse, V1",
                    "identifier_of_dataverse": "demo",
                    "name_of_dataverse": "Demo Dataverse",
                    "citation": "Finch, Fiona, 2020, \"Darwin's Finches\", https://doi.org/10.70122/FK2/7ZXYRH, Demo Dataverse, V1",
                    "publicationStatuses": [
                        "Published"
                    ],
                    "storageIdentifier": "file://10.70122/FK2/7ZXYRH",
                    "subjects": [
                        "Medicine, Health and Life Sciences"
                    ],
                    "fileCount":9,
                    "versionId": 53444,
                    "versionState": "RELEASED",
                    "majorVersion": 1,
                    "minorVersion": 0,
                    "createdAt": "2020-01-22T21:23:43Z",
                    "updatedAt": "2020-01-22T21:47:34Z",
                    "contacts": [
                        {
                            "name": "Finch, Fiona",
                            "affiliation": ""
                        }
                    ],
                    "authors": [
                        "Finch, Fiona"
                    ]
                }
            ],
            "count_in_response": 2
        }
    }
    
.. _dynamic-citation-all:

Include Metadata Blocks and/or Metadata Fields
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

https://demo.dataverse.org/api/search?q=\*&type=dataset&metadata_fields=citation:\*

The above example ``metadata_fields=citation:*`` returns under "metadataBlocks" all fields from the "citation" metadata block.

..  code-block:: json

    {
        "status": "OK",
        "data": {
            "q": "*",
            "total_count": 4,
            "start": 0,
            "spelling_alternatives": {},
            "items": [
                {
                    "name": "JDD avec GeoJson 2021-07-13T10:23:46.409Z",
                    "type": "dataset",
                    "url": "https://doi.org/10.5072/FK2/GIWCKB",
                    "global_id": "doi:10.5072/FK2/GIWCKB",
                    "description": "Démo sprint 5. Cette couche représente l'emprise des cimetières sur le territoire des Métropole. Ces périmètres d'emprise des cimetières sont issus du recensement des informations des PLU/POS de chaque commune de la métropole, des données du cadastre DGFiP et d'un inventaire terrain du Service Planification et Études Urbaines de Métropole",
                    "publisher": "Sample Data",
                    "citationHtml": "Rennes M&eacute;tropole, 2021, \"JDD avec GeoJson 2021-07-13T10:23:46.409Z\", <a href=\"https://doi.org/10.5072/FK2/GIWCKB\" target=\"_blank\">https://doi.org/10.5072/FK2/GIWCKB</a>, Root, DRAFT VERSION",
                    "identifier_of_dataverse": "Sample_data",
                    "name_of_dataverse": "Sample Data",
                    "citation": "Métropole, 2021, \"JDD avec GeoJson 2021-07-13T10:23:46.409Z\", https://doi.org/10.5072/FK2/GIWCKB, Root, DRAFT VERSION",
                    "publicationStatuses": [
                        "Unpublished",
                        "Draft"
                    ],
                    "storageIdentifier": "file://10.5072/FK2/GIWCKB",
                    "subjects": [
                        "Other"
                    ],
                    "fileCount": 0,
                    "versionId": 9976,
                    "versionState": "DRAFT",
                    "createdAt": "2021-07-13T10:28:45Z",
                    "updatedAt": "2021-07-13T10:28:45Z",
                    "contacts": [
                        {
                            "name": "string",
                            "affiliation": "string"
                        }
                    ],
                    "metadataBlocks": {
                        "citation": {
                            "displayName": "Citation Metadata",
                            "fields": [
                                {
                                    "typeName": "dsDescription",
                                    "multiple": true,
                                    "typeClass": "compound",
                                    "value": [
                                        {
                                            "dsDescriptionValue": {
                                                "typeName": "dsDescriptionValue",
                                                "multiple": false,
                                                "typeClass": "primitive",
                                                "value": "Démo sprint 5. Cette couche représente l'emprise des cimetières sur le territoire des Métropole. Ces périmètres d'emprise des cimetières sont issus du recensement des informations des PLU/POS de chaque commune de la métropole, des données du cadastre DGFiP et d'un inventaire terrain du Service Planification et Études Urbaines de Métropole"
                                            },
                                            "dsDescriptionDate": {
                                                "typeName": "dsDescriptionDate",
                                                "multiple": false,
                                                "typeClass": "primitive",
                                                "value": "2021-07-13"
                                            }
                                        }
                                    ]
                                },
                                {
                                    "typeName": "author",
                                    "multiple": true,
                                    "typeClass": "compound",
                                    "value": [
                                        {
                                            "authorName": {
                                                "typeName": "authorName",
                                                "multiple": false,
                                                "typeClass": "primitive",
                                                "value": "Métropole"
                                            },
                                            "authorAffiliation": {
                                                "typeName": "authorAffiliation",
                                                "multiple": false,
                                                "typeClass": "primitive",
                                                "value": "string"
                                            }
                                        }
                                    ]
                                },
                                {
                                    "typeName": "datasetContact",
                                    "multiple": true,
                                    "typeClass": "compound",
                                    "value": [
                                        {
                                            "datasetContactName": {
                                                "typeName": "datasetContactName",
                                                "multiple": false,
                                                "typeClass": "primitive",
                                                "value": "string"
                                            },
                                            "datasetContactAffiliation": {
                                                "typeName": "datasetContactAffiliation",
                                                "multiple": false,
                                                "typeClass": "primitive",
                                                "value": "string"
                                            },
                                            "datasetContactEmail": {
                                                "typeName": "datasetContactEmail",
                                                "multiple": false,
                                                "typeClass": "primitive",
                                                "value": "contact@Sample.fr"
                                            }
                                        }
                                    ]
                                },
                                {
                                    "typeName": "subject",
                                    "multiple": true,
                                    "typeClass": "controlledVocabulary",
                                    "value": [
                                        "Other"
                                    ]
                                },
                                {
                                    "typeName": "title",
                                    "multiple": false,
                                    "typeClass": "primitive",
                                    "value": "JDD avec GeoJson 2021-07-13T10:23:46.409Z"
                                }
                            ]
                        }
                    },
                    "authors": [
                        "Métropole"
                    ]
                },
                {
                    "name": "Raja Ampat Islands",
                    "type": "dataset",
                    "url": "https://doi.org/10.5072/FK2/ITNXGR",
                    "global_id": "doi:10.5072/FK2/ITNXGR",
                    "description": "Raja Ampat is located off the northwest tip of Bird's Head Peninsula on the island of New Guinea, in Indonesia's West Papua province, Raja Ampat, or the Four Kings, is an archipelago comprising over 1,500 small islands, cays, and shoals surrounding the four main islands of Misool, Salawati, Batanta, and Waigeo, and the smaller island of Kofiau. The Raja Ampat archipelago straddles the Equator and forms part of Coral Triangle which contains the richest marine biodiversity on earth. Administratively, the archipelago is part of the province of West Papua (formerly known as Irian Jaya). Most of the islands constitute the Raja Ampat Regency, which was separated out from Sorong Regency in 2004. The regency encompasses around 70,000 square kilometres (27,000 sq mi) of land and sea, and has a population of about 50,000 (as of 2017). (Wikipedia: https://en.wikipedia.org/wiki/Raja_Ampat_Islands)",
                    "published_at": "2020-07-30T09:23:34Z",
                    "publisher": "Root",
                    "citationHtml": "Admin, Dataverse, 2020, \"Raja Ampat Islands\", <a href=\"https://doi.org/10.5072/FK2/ITNXGR\" target=\"_blank\">https://doi.org/10.5072/FK2/ITNXGR</a>, Root, V1",
                    "identifier_of_dataverse": "root",
                    "name_of_dataverse": "Root",
                    "citation": "Admin, Dataverse, 2020, \"Raja Ampat Islands\", https://doi.org/10.5072/FK2/ITNXGR, Root, V1",
                    "authors": [
                        "Admin, Dataverse"
                    ]
                },
                {
                    "name": "Sample Test",
                    "type": "dataverse",
                    "url": "https://68b2d8bb37c6/dataverse/Sample_test",
                    "identifier": "Sample_test",
                    "description": "Dataverse utilisé pour les tests unitaires de Sample",
                    "published_at": "2021-03-16T08:11:54Z"
                },
                {
                    "name": "Sample Media Test",
                    "type": "dataverse",
                    "url": "https://68b2d8bb37c6/dataverse/Sample_media_test",
                    "identifier": "Sample_media_test",
                    "description": "Dataverse de test contenant les médias de Sample, comme les images des fournisseurs et des producteurs",
                    "published_at": "2021-04-08T15:04:14Z"
                }
            ],
            "count_in_response": 4
        }
    }

.. _dynamic-citation-some:

Include Specific Fields Only
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

https://demo.dataverse.org/api/search?q=*&type=dataset&metadata_fields=citation:dsDescription&metadata_fields=citation:author

The above example ``metadata_fields=citation:dsDescription&metadata_fields=citation:author`` returns under "metadataBlocks" only the compound fields "dsDescription" and "author" metadata fields from the "citation" metadata block.

.. code-block:: json

    {
        "status": "OK",
        "data": {
            "q": "*",
            "total_count": 4,
            "start": 0,
            "spelling_alternatives": {},
            "items": [
                {
                    "name": "JDD avec GeoJson 2021-07-13T10:23:46.409Z",
                    "type": "dataset",
                    "url": "https://doi.org/10.5072/FK2/GIWCKB",
                    "global_id": "doi:10.5072/FK2/GIWCKB",
                    "description": "Démo sprint 5. Cette couche représente l'emprise des cimetières sur le territoire des Métropole. Ces périmètres d'emprise des cimetières sont issus du recensement des informations des PLU/POS de chaque commune de la métropole, des données du cadastre DGFiP et d'un inventaire terrain du Service Planification et Études Urbaines de Métropole",
                    "publisher": "Sample Data",
                    "citationHtml": "Rennes M&eacute;tropole, 2021, \"JDD avec GeoJson 2021-07-13T10:23:46.409Z\", <a href=\"https://doi.org/10.5072/FK2/GIWCKB\" target=\"_blank\">https://doi.org/10.5072/FK2/GIWCKB</a>, Root, DRAFT VERSION",
                    "identifier_of_dataverse": "Sample_data",
                    "name_of_dataverse": "Sample Data",
                    "citation": "Métropole, 2021, \"JDD avec GeoJson 2021-07-13T10:23:46.409Z\", https://doi.org/10.5072/FK2/GIWCKB, Root, DRAFT VERSION",
                    "storageIdentifier": "file://10.5072/FK2/GIWCKB",
                    "subjects": [
                        "Other"
                    ],
                    "fileCount": 0,
                    "versionId": 9976,
                    "versionState": "DRAFT",
                    "createdAt": "2021-07-13T10:28:45Z",
                    "updatedAt": "2021-07-13T10:28:45Z",
                    "contacts": [
                        {
                            "name": "string",
                            "affiliation": "string"
                        }
                    ],
                    "metadataBlocks": {
                        "citation": {
                            "displayName": "Citation Metadata",
                            "fields": [
                                {
                                    "typeName": "dsDescription",
                                    "multiple": true,
                                    "typeClass": "compound",
                                    "value": [
                                        {
                                            "dsDescriptionValue": {
                                                "typeName": "dsDescriptionValue",
                                                "multiple": false,
                                                "typeClass": "primitive",
                                                "value": "Démo sprint 5. Cette couche représente l'emprise des cimetières sur le territoire des Métropole. Ces périmètres d'emprise des cimetières sont issus du recensement des informations des PLU/POS de chaque commune de la métropole, des données du cadastre DGFiP et d'un inventaire terrain du Service Planification et Études Urbaines de Métropole"
                                            },
                                            "dsDescriptionDate": {
                                                "typeName": "dsDescriptionDate",
                                                "multiple": false,
                                                "typeClass": "primitive",
                                                "value": "2021-07-13"
                                            }
                                        }
                                    ]
                                },
                                {
                                    "typeName": "author",
                                    "multiple": true,
                                    "typeClass": "compound",
                                    "value": [
                                        {
                                            "authorName": {
                                                "typeName": "authorName",
                                                "multiple": false,
                                                "typeClass": "primitive",
                                                "value": "Métropole"
                                            },
                                            "authorAffiliation": {
                                                "typeName": "authorAffiliation",
                                                "multiple": false,
                                                "typeClass": "primitive",
                                                "value": "string"
                                            }
                                        }
                                    ]
                                }
                            ]
                        }
                    },
                    "authors": [
                        "Métropole"
                    ]
                },
                {
                    "name": "Raja Ampat Islands",
                    "type": "dataset",
                    "url": "https://doi.org/10.5072/FK2/ITNXGR",
                    "global_id": "doi:10.5072/FK2/ITNXGR",
                    "description": "Raja Ampat is located off the northwest tip of Bird's Head Peninsula on the island of New Guinea, in Indonesia's West Papua province, Raja Ampat, or the Four Kings, is an archipelago comprising over 1,500 small islands, cays, and shoals surrounding the four main islands of Misool, Salawati, Batanta, and Waigeo, and the smaller island of Kofiau. The Raja Ampat archipelago straddles the Equator and forms part of Coral Triangle which contains the richest marine biodiversity on earth. Administratively, the archipelago is part of the province of West Papua (formerly known as Irian Jaya). Most of the islands constitute the Raja Ampat Regency, which was separated out from Sorong Regency in 2004. The regency encompasses around 70,000 square kilometres (27,000 sq mi) of land and sea, and has a population of about 50,000 (as of 2017). (Wikipedia: https://en.wikipedia.org/wiki/Raja_Ampat_Islands)",
                    "published_at": "2020-07-30T09:23:34Z",
                    "publisher": "Root",
                    "citationHtml": "Admin, Dataverse, 2020, \"Raja Ampat Islands\", <a href=\"https://doi.org/10.5072/FK2/ITNXGR\" target=\"_blank\">https://doi.org/10.5072/FK2/ITNXGR</a>, Root, V1",
                    "identifier_of_dataverse": "root",
                    "name_of_dataverse": "Root",
                    "citation": "Admin, Dataverse, 2020, \"Raja Ampat Islands\", https://doi.org/10.5072/FK2/ITNXGR, Root, V1",
                    "authors": [
                        "Admin, Dataverse"
                    ]
                },
                {
                    "name": "Sample Media Test",
                    "type": "dataverse",
                    "url": "https://68b2d8bb37c6/dataverse/Sample_media_test",
                    "identifier": "Sample_media_test",
                    "description": "Dataverse de test contenant les médias de Sample, comme les images des fournisseurs et des producteurs",
                    "published_at": "2021-04-08T15:04:14Z"
                },
                {
                    "name": "Sample Test",
                    "type": "dataverse",
                    "url": "https://68b2d8bb37c6/dataverse/Sample_test",
                    "identifier": "Sample_test",
                    "description": "Dataverse utilisé pour les tests unitaires de Sample",
                    "published_at": "2021-03-16T08:11:54Z"
                }
            ],
            "count_in_response": 4,
            "total_count_per_object_type": {
                "Datasets": 2,
                "Dataverses": 2
            }
        }
    }

.. _search-date-range:

Date Range Search Example
-------------------------

Below is an example of searching across a date range of Dataverse collections, datasets, and files that were published in 2018.

`https://demo.dataverse.org/api/search?q=*&per_page=1000&sort=date&order=asc&q=*&fq=dateSort:[2018-01-01T00\:00\:00Z+TO+2019-01-01T00\:00\:00Z] <https://demo.dataverse.org/api/search?q=*&per_page=1000&sort=date&order=asc&q=*&fq=dateSort:[2018-01-01T00\:00\:00Z+TO+2019-01-01T00\:00\:00Z]>`_

.. _iteration-example:

Iteration
---------

Be default, up to 10 results are returned with every request (though this can be increased with the ``per_page`` parameter). To iterate through many results, increase the ``start`` parameter on each iteration until you reach the ``total_count`` in the response. An example in Python is below.

.. code-block:: python

    #!/usr/bin/env python
    import urllib2
    import json
    base = 'https://demo.dataverse.org'
    rows = 10
    start = 0
    page = 1
    condition = True # emulate do-while
    while (condition):
        url = base + '/api/search?q=*' + "&start=" + str(start)
        data = json.load(urllib2.urlopen(url))
        total = data['data']['total_count']
        print "=== Page", page, "==="
        print "start:", start, " total:", total
        for i in data['data']['items']:
            print "- ", i['name'], "(" + i['type'] + ")"
        start = start + rows
        page += 1
        condition = start < total


Output from iteration example

.. code-block:: none

    === Page 1 ===
    start: 0  total: 12
    -  Spruce Goose (dataset)
    -  trees.png (file)
    -  Spruce (dataverse)
    -  Trees (dataverse)
    -  Darwin's Finches (dataset)
    -  Finches (dataverse)
    -  Birds (dataverse)
    -  Rings of Conifers (dataset)
    -  Chestnut Trees (dataverse)
    -  Sparrows (dataverse)
    === Page 2 ===
    start: 10  total: 12
    -  Chestnut Sparrows (dataverse)
    -  Wrens (dataverse)

.. |CORS| raw:: html

      <span class="label label-success pull-right">
        CORS
      </span>

.. _search-services:

Search Services
---------------

.. _discovering-available-search-services:

Discovering Available Search Services
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

To discover available search services and their capabilities, you can use the Search Services API endpoint.
Note: Configurable Search Services are an optional, experimental feature than may evolve faster than other parts of Dataverse.

Example API endpoint: https://demo.dataverse.org/api/search/services

This endpoint returns a list of available search services, including their names, and display names. It also indicates the default search service.

Example response:

.. code-block:: json

    {
        "status": "OK",
        "data": {
         "services": [
            {
                "name": "solr",
                "displayName": "Solr Search",
            },
            {
                "name": "externalSearch",
                "displayName": "External Search for Datasets",
            }
        ],
        "defaultService": "solr"
    }

You can use the ``name`` values returned by this endpoint in the ``search_service`` parameter of the main search API to specify which search service to use for a particular query.

Using Different Search Services
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

To use a specific search service, include the ``search_service`` parameter in your search query and pass the ``name``. For example:

https://demo.dataverse.org/api/search?q=trees&search_service=externalSearch

This query will use the ``externalSearch`` service (assuming it exists) instead of the default search service (``solr``).

.. note:: Other search services may not be complete replacements for the included ``solr`` service. For example, they may not support searching for collections or files (just datasets).

Developing Search Services
~~~~~~~~~~~~~~~~~~~~~~~~~~

See :doc:`/developers/search-services` in the Developer Guide.
