===============
Geospatial Data
===============

.. contents:: |toctitle|
	:local:

Geoconnect
----------

Geoconnect works as a middle layer, allowing geospatial data files in Dataverse to be visualized with Harvard WorldMap. To set up a Geoconnect development environment, you can follow the steps outlined in the `local_setup.md <https://github.com/IQSS/geoconnect/blob/master/local_setup.md>`_ guide. You will need Python and a few other prerequisites.

As mentioned under "Architecture and Components" in the :doc:`/installation/prep` section of the Installation Guide, Geoconnect is an optional component of Dataverse, so this section is only necessary to follow it you are working on an issue related to this feature.

How Dataverse Ingests Shapefiles
--------------------------------

A shapefile is a set of files, often uploaded/transferred in ``.zip`` format. This set may contain up to fifteen files. A minimum of three specific files (``.shp``, ``.shx``, ``.dbf``) are needed to be a valid shapefile and a fourth file (``.prj``) is required for WorldMap -- or any type of meaningful visualization.

For ingest and connecting to WorldMap, four files are the minimum required:

- ``.shp`` - shape format; the feature geometry itself
- ``.shx`` - shape index format; a positional index of the feature geometry to allow seeking forwards and backwards quickly
- ``.dbf`` - attribute format; columnar attributes for each shape, in dBase IV format
- ``.prj`` - projection format; the coordinate system and projection information, a plain text file describing the projection using well-known text format

Ingest
~~~~~~

When uploaded to Dataverse, the ``.zip`` is unpacked (same as all ``.zip`` files). Shapefile sets are recognized by the same base name and specific extensions. These individual files constitute a shapefile set. The first four are the minimum required (``.shp``, ``.shx``, ``.dbf``, ``.prj``)

For example:

- bicycles.shp    (required extension)
- bicycles.shx    (required extension)
- bicycles.prj	(required extension)
- bicycles.dbf	(required extension)
- bicycles.sbx	(NOT required extension)
- bicycles.sbn	(NOT required extension)

Upon recognition of the four required files, Dataverse will group them as well as any other relevant files into a shapefile set. Files with these extensions will be included in the shapefile set:

- Required: ``.shp``, ``.shx``, ``.dbf``, ``.prj``
- Optional: ``.sbn``, ``.sbx``, ``.fbn``, ``.fbx``, ``.ain``, ``.aih``, ``.ixs``, ``.mxs``, ``.atx``, ``.cpg``, ``shp.xml``

Then Dataverse creates a new ``.zip`` with mimetype as a shapefile. The shapefile set will persist as this new ``.zip``.

Example
~~~~~~~

**1a.** Original ``.zip`` contents:

A file named ``bikes_and_subways.zip`` is uploaded to the Dataverse. This ``.zip`` contains the following files.

- ``bicycles.shp``  (shapefile set #1)
- ``bicycles.shx``  (shapefile set #1)
- ``bicycles.prj``  (shapefile set #1)
- ``bicycles.dbf``  (shapefile set #1)
- ``bicycles.sbx``  (shapefile set #1)
- ``bicycles.sbn``  (shapefile set #1)
- ``bicycles.txt``
- ``the_bikes.md``
- ``readme.txt``
- ``subway_line.shp``  (shapefile set #2)
- ``subway_line.shx``  (shapefile set #2)
- ``subway_line.prj``  (shapefile set #2)
- ``subway_line.dbf``  (shapefile set #2)

**1b.** Dataverse unzips and re-zips files:

Upon ingest, Dataverse unpacks the file ``bikes_and_subways.zip``. Upon recognizing the shapefile sets, it groups those files together into new ``.zip`` files:

- files making up the "bicycles" shapefile become a new ``.zip``
- files making up the "subway_line" shapefile become a new ``.zip``
- remaining files will stay as they are

To ensure that a shapefile set remains intact, individual files such as ``bicycles.sbn`` are kept in the set -- even though they are not used for mapping.

**1c.** Dataverse final file listing:

- ``bicycles.zip`` (contains shapefile set #1: ``bicycles.shp``, ``bicycles.shx``, ``bicycles.prj``, ``bicycles.dbf``, ``bicycles.sbx``, ``bicycles.sbn``)
- ``bicycles.txt``  (separate, not part of a shapefile set)
- ``the_bikes.md``  (separate, not part of a shapefile set)
- ``readme.txt``  (separate, not part of a shapefile set)
- ``subway_line.zip``  (contains shapefile set #2: ``subway_line.shp``, ``subway_line.shx``, ``subway_line.prj``, ``subway_line.dbf``)

For two "final" shapefile sets, ``bicycles.zip`` and ``subway_line.zip``, a new mimetype is used:

- Mimetype: ``application/zipped-shapefile``
- Mimetype Label: "Shapefile as ZIP Archive"

WorldMap JoinTargets + API Endpoint
-----------------------------------

WorldMap supplies target layers -- or JoinTargets -- that a tabular file may be mapped against. A JSON description of these `CGA <http://gis.harvard.edu>`_-curated JoinTargets may be retrieved via API at ``http://worldmap.harvard.edu/datatables/api/jointargets/``. Please note: login is required. You may use any WorldMap account credentials via HTTP Basic Auth.

Example of JoinTarget information returned via the API:

.. code-block:: json

    {
	  "data":[
	    {
	      "layer":"geonode:census_tracts_2010_boston_6f6",
	      "name":"Census Tracts, Boston (GEOID10: State+County+Tract)",
	      "geocode_type_slug":"us-census-tract",
	      "geocode_type":"US Census Tract",
	      "attribute":{
	        "attribute":"CT_ID_10",
	        "type":"xsd:string"
	      },
	      "abstract":"As of the 2010 census, Boston, MA contains 7,288 city blocks [truncated for example]",
	      "title":"Census Tracts 2010, Boston (BARI)",
	      "expected_format":{
	        "expected_zero_padded_length":-1,
	        "is_zero_padded":false,
	        "description":"Concatenation of state, county and tract for 2010 Census Tracts.  Reference: https://www.census.gov/geo/maps-data/data/tract_rel_layout.html\r\n\r\nNote:  Across the US, this can be a zero-padded \"string\" but the original Boston layer has this column as \"numeric\" ",
	        "name":"2010 Census Boston GEOID10 (State+County+Tract)"
	      },
	      "year":2010,
	      "id":28
	    },
	    {
	      "layer":"geonode:addresses_2014_boston_1wr",
	      "name":"Addresses, Boston",
	      "geocode_type_slug":"boston-administrative-geography",
	      "geocode_type":"Boston, Administrative Geography",
	      "attribute":{
	        "attribute":"LocationID",
	        "type":"xsd:int"
	      },
	      "abstract":"Unique addresses present in the parcels data set, which itself is derived from [truncated for example]",
	      "title":"Addresses 2015, Boston (BARI)",
	      "expected_format":{
	        "expected_zero_padded_length":-1,
	        "is_zero_padded":false,
	        "description":"Boston, Administrative Geography, Boston Address Location ID.  Example: 1, 2, 3...nearly 120000",
	        "name":"Boston Address Location ID (integer)"
	      },
	      "year":2015,
	      "id":18
	    },
	    {
	      "layer":"geonode:bra_neighborhood_statistical_areas_2012__ug9",
	      "name":"BRA Neighborhood Statistical Areas, Boston",
	      "geocode_type_slug":"boston-administrative-geography",
	      "geocode_type":"Boston, Administrative Geography",
	      "attribute":{
	        "attribute":"BOSNA_R_ID",
	        "type":"xsd:double"
	      },
	      "abstract":"BRA Neighborhood Statistical Areas 2015, Boston. Provided by [truncated for example]",
	      "title":"BRA Neighborhood Statistical Areas 2015, Boston (BARI)",
	      "expected_format":{
	        "expected_zero_padded_length":-1,
	        "is_zero_padded":false,
	        "description":"Boston, Administrative Geography, Boston BRA Neighborhood Statistical Area ID (integer).  Examples: 1, 2, 3, ... 68, 69",
	        "name":"Boston BRA Neighborhood Statistical Area ID (integer)"
	      },
	      "year":2015,
	      "id":17
	    }
	  ],
	  "success":true
    }

How Geoconnect Uses Join Target Information
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

When a user attempts to map a tabular file, the application looks in the Geoconnect database for ``JoinTargetInformation``. If this information is more than 10 minutes* old, the application will retrieve fresh information and save it to the db.

(* Change the timing via the Django settings variable ``JOIN_TARGET_UPDATE_TIME``.)

This JoinTarget info is used to populate HTML forms used to match a tabular file column to a JoinTarget column. Once a JoinTarget is chosen, the JoinTarget ID is an essential piece of information used to make an API call to the WorldMap and attempt to map the file.

Retrieving Join Target Information from WorldMap API
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The ``get_join_targets()`` function in ``dataverse_layer_services.py`` uses the WorldMap API, retrieves a list of available tabular file JointTargets. (See the `dataverse_layer_services code in GitHub <https://github.com/IQSS/geoconnect/blob/master/gc_apps/worldmap_connect/dataverse_layer_services.py#L275>`_.)

Saving Join Target Information to Geoconnect Database
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The ``get_latest_jointarget_information()`` in ``utils.py`` retrieves recent JoinTarget Information from the database. (See the `utils code in GitHub <https://github.com/IQSS/geoconnect/blob/master/gc_apps/worldmap_connect/utils.py#L16>`_.)

Setting Up WorldMap Test Data
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

For the dataset page, this script gives a query to add test WorldMap map data. After the query is run, the "Explore Map" button should appear for a tabular file or shapefile. In the example SQL queries below, substitute ``$DATASET_ID`` and ``$DATAFILE_ID`` with the appropriate ID's.

To add sample map data for a tabular file:

.. code::

    INSERT INTO maplayermetadata (id, isjoinlayer, joindescription, embedmaplink, layerlink, layername, mapimagelink, worldmapusername, dataset_id, datafile_id) 
    VALUES (DEFAULT, true, 'This file was joined with WorldMap layer x, y, z',
    'https://worldmap.harvard.edu/maps/embed/?layer=geonode:zip_codes_2015_zip_s9i','https://worldmap.harvard.edu/data/geonode:zip_codes_2015_zip_s9i',
    'geonode:zip_codes_2015_zip_s9i',
    'http://worldmap.harvard.edu/download/wms/27289/png?layers=geonode%3Azip_codes_2015_zip_s9i&#38;width=865&#38;bbox=-71.1911091251%2C42.2270382738%2C-70.9228275369%2C42.3976144794&#38;service=WMS&#38;format=image%2Fpng&#38;srs=EPSG%3A4326&#38;request=GetMap&#38;height=550',
    'admin',$DATASET_ID,$DATAFILE_ID});

To add sample map data for a tabular shapefile:

.. code::

    INSERT INTO maplayermetadata (id, isjoinlayer, embedmaplink, layerlink, layername, mapimagelink, worldmapusername, dataset_id, datafile_id) 
    VALUES (DEFAULT, false,
    'https://worldmap.harvard.edu/maps/embed/?layer=geonode:zip_codes_2015_zip_s9i','https://worldmap.harvard.edu/data/geonode:zip_codes_2015_zip_s9i',
    'geonode:zip_codes_2015_zip_s9i',
    'http://worldmap.harvard.edu/download/wms/27289/png?layers=geonode%3Azip_codes_2015_zip_s9i&#38;width=865&#38;bbox=-71.1911091251%2C42.2270382738%2C-70.9228275369%2C42.3976144794&#38;service=WMS&#38;format=image%2Fpng&#38;srs=EPSG%3A4326&#38;request=GetMap&#38;height=550',
    'admin',$DATASET_ID,$DATAFILE_ID);

----

Previous: :doc:`unf/index` | Next: :doc:`remote-users`
