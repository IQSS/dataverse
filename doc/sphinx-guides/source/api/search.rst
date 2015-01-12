Search API
==========

.. contents:: :local:

About
-----

The Search API supports the same searching, sorting, and faceting operations as the Dataverse web interface.

The parameters and JSON response are partly inspired by the `GitHub Search API <https://developer.github.com/v3/search/>`_.

Parameters
----------

==============  =======  ===========
Name            Type     Description
==============  =======  ===========
q               string   The search term or terms. Using "title:data" will search only the "title" field. "*" can be used as a wildcard either alone or adjacent to a term (i.e. "bird*").
type            string   Can be either "dataverse", "dataset", or "file". Multiple "type" parameters can be used to include multiple types (i.e. ``type=dataset&type=file``). If omitted, all types will be returned.
subtree         string   The alias of the dataverse to which the search should be narrowed. The subtree of this dataverse and all its children will be searched.
sort            string   The sort field. Supported values include "name" and "date". 
order           string   The order in which to sort. Can either be "asc" or "desc".
per_page        int      The number of results to return per request. The default is 10. The max is 1000.
start           int      A cursor for paging through search results. See iteration example below.
show_relevance  boolean  Whether or not to show details of which fields were matched by the query. False by default.
show_facets     boolean  Whether or not to show facets that can be operated on by the "fq" parameter. False by default.
fq              string   A filter query on the search term. Multiple "fq" parameters can be used.
==============  =======  ===========

Example
-------

https://apitest.dataverse.org/api/search?q=*

.. code-block:: json

    {
      "data": {
        "count_in_response": 4,
        "items": [
          {
            "authors": [
              "Spruce, Sabrina"
            ],
            "citation": "Spruce, Sabrina, 2015, \"Spruce Goose\", http://dx.doi.org/10.5072/FK2/I4VPEZ,  Root Dataverse,  V0",
            "published_at": "2015-01-08T03:27Z",
            "global_id": "doi:10.5072/FK2/I4VPEZ",
            "persistent_url": "http://dx.doi.org/10.5072/FK2/I4VPEZ",
            "html_url": "https://apitest.dataverse.org/dataset.xhtml?globalId=doi:10.5072/FK2/I4VPEZ",
            "type": "dataset",
            "name": "Spruce Goose"
          },
          {
            "file_type": "PNG Image",
            "published_at": "2015-01-08T03:27Z",
            "description": "",
            "file_id": "12",
            "persistent_url": "http://dx.doi.org/10.5072/FK2/I4VPEZ",
            "html_url": "https://apitest.dataverse.org/dataset.xhtml?globalId=doi:10.5072/FK2/I4VPEZ",
            "type": "dataset",
            "name": "trees.png"
          },
          {
            "published_at": "2015-01-08T03:27Z",
            "description": "A spruce with some birds",
            "alias": "spruce",
            "html_url": "https://apitest.dataverse.org/dataverse/spruce",
            "type": "dataverse",
            "name": "Spruce"
          },
          {
            "published_at": "2015-01-08T03:27Z",
            "description": "A tree dataverse with some birds",
            "alias": "trees",
            "html_url": "https://apitest.dataverse.org/dataverse/trees",
            "type": "dataverse",
            "name": "Trees"
          }
        ],
        "spelling_alternatives": {},
        "start": 0,
        "total_count": 4,
        "q": "*"
      },
      "status": "OK"
    }

Relevance
---------

By setting ``show_relevance=true`` matches per field are shown:

.. code-block:: json

    "matches":[
        {
            "description":{
                "snippets":[
                    "A <span class=\"search-term-match\">tree</span> dataverse with some birds"
                ]
            }
        },
        {
            "name":{
                "snippets":[
                    "<span class=\"search-term-match\">Trees</span>"
                ]
            }
        }
    ]

Iteration
---------

Be default, up to 10 results are returned with every request (though this can be increased with the ``per_page`` parameter). To iterate through many results, increase the ``start`` parameter on each iteration until you reach the ``total_count`` in the response. An example in Python is below.

.. code-block:: python

    #!/usr/bin/env python
    import urllib2
    import json
    base = 'https://apitest.dataverse.org'
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
