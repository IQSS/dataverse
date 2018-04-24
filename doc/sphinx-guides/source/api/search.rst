Search API
==========

.. contents:: |toctitle|
    :local:

The Search API supports the same searching, sorting, and faceting operations as the Dataverse web interface.

Unlike the web interface, this new API is limited to *published* data until `issue 1299 <https://github.com/IQSS/dataverse/issues/1299>`_ is resolved.

The parameters and JSON response are partly inspired by the `GitHub Search API <https://developer.github.com/v3/search/>`_.

.. note:: |CORS| The search API can be used from scripts running in web browsers, as it allows cross-origin resource sharing (CORS).

.. _CORS: https://www.w3.org/TR/cors/


Please note that in Dataverse 4.3 and older the "citation" field wrapped the persistent ID URL in an ``<a>`` tag but this has been changed to plaintext. If you want the old value with HTML in it, a new field called "citationHtml" can be used.


Parameters
----------

===============  =======  ===========
Name             Type     Description
===============  =======  ===========
q                string   The search term or terms. Using "title:data" will search only the "title" field. "*" can be used as a wildcard either alone or adjacent to a term (i.e. "bird*"). For example, https://demo.dataverse.org/api/search?q=title:data
type             string   Can be either "dataverse", "dataset", or "file". Multiple "type" parameters can be used to include multiple types (i.e. ``type=dataset&type=file``). If omitted, all types will be returned.  For example, https://demo.dataverse.org/api/search?q=*&type=dataset
subtree          string   The identifier of the dataverse to which the search should be narrowed. The subtree of this dataverse and all its children will be searched.  For example, https://demo.dataverse.org/api/search?q=data&subtree=birds
sort             string   The sort field. Supported values include "name" and "date". See example under "order".
order            string   The order in which to sort. Can either be "asc" or "desc".  For example, https://demo.dataverse.org/api/search?q=data&sort=name&order=asc
per_page         int      The number of results to return per request. The default is 10. The max is 1000. See :ref:`iteration example <iteration-example>`.
start            int      A cursor for paging through search results. See :ref:`iteration example <iteration-example>`.
show_relevance   boolean  Whether or not to show details of which fields were matched by the query. False by default. See :ref:`advanced search example <advancedsearch-example>`.
show_facets      boolean  Whether or not to show facets that can be operated on by the "fq" parameter. False by default. See :ref:`advanced search example <advancedsearch-example>`.
fq               string   A filter query on the search term. Multiple "fq" parameters can be used. See :ref:`advanced search example <advancedsearch-example>`.
show_entity_ids  boolean  Whether or not to show the database IDs of the search results (for developer use).
===============  =======  ===========

Basic Search Example
--------------------

https://demo.dataverse.org/api/search?q=trees

.. code-block:: json

    {
        "status":"OK",
        "data":{
            "q":"trees",
            "total_count":4,
            "start":0,
            "spelling_alternatives":{
                "trees":"[tree]"
            },
            "items":[
                {
                    "name":"Trees",
                    "type":"dataverse",
                    "url":"https://demo.dataverse.org/dataverse/trees",
                    "image_url":"https://demo.dataverse.org/api/access/dvCardImage/7",
                    "identifier":"trees",
                    "description":"A tree dataverse with some birds",
                    "published_at":"2016-05-10T12:53:38Z"
                },
                {
                    "name":"Chestnut Trees",
                    "type":"dataverse",
                    "url":"https://demo.dataverse.org/dataverse/chestnuttrees",
                    "image_url":"https://demo.dataverse.org/api/access/dvCardImage/9",
                    "identifier":"chestnuttrees",
                    "description":"A dataverse with chestnut trees and an oriole",
                    "published_at":"2016-05-10T12:52:38Z"
                },
                {
                    "name":"trees.png",
                    "type":"file",
                    "url":"https://demo.dataverse.org/api/access/datafile/12",
                    "image_url":"https://demo.dataverse.org/api/access/fileCardImage/12",
                    "file_id":"12",
                    "description":"",
                    "published_at":"2016-05-10T12:53:39Z",
                    "file_type":"PNG Image",
                    "file_content_type":"image/png",
                    "size_in_bytes":8361,
                    "md5":"0386269a5acb2c57b4eade587ff4db64",
                    "dataset_citation":"Spruce, Sabrina, 2016, \"Spruce Goose\", http://dx.doi.org/10.5072/FK2/NFSEHG, Root Dataverse, V1"
                },
                {
                    "name":"Birds",
                    "type":"dataverse",
                    "url":"https://demo.dataverse.org/dataverse/birds",
                    "image_url":"https://demo.dataverse.org/api/access/dvCardImage/2",
                    "identifier":"birds",
                    "description":"A bird dataverse with some trees",
                    "published_at":"2016-05-10T12:57:27Z"
                }
            ],
            "count_in_response":4
        }
    }

.. _advancedsearch-example:

Advanced Search Example
-----------------------

https://demo.dataverse.org/api/search?q=finch&show_relevance=true&show_facets=true&fq=publicationDate:2016&subtree=birds

In this example, ``show_relevance=true`` matches per field are shown. Available facets are shown with ``show_facets=true`` and of the facets is being used with ``fq=publication_date_s:2015``. The search is being narrowed to the dataverse with the identifier "birds" with the parameter ``subtree=birds``.

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
                    "image_url":"https://demo.dataverse.org/api/access/dvCardImage/3",
                    "identifier":"finches",
                    "description":"A dataverse with finches",
                    "published_at":"2016-05-10T12:57:38Z",
                    "matches":[
                        {
                            "description":{
                                "snippets":[
                                    "A dataverse with <span class=\"search-term-match\">finches</span>"
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
