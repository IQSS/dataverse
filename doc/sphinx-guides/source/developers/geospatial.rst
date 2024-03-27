===============
Geospatial Data
===============

.. contents:: |toctitle|
	:local:

How The Dataverse Software Ingests Shapefiles
---------------------------------------------

A shapefile is a set of files, often uploaded/transferred in ``.zip`` format. This set may contain up to fifteen files. A minimum of three specific files (``.shp``, ``.shx``, ``.dbf``) are needed to be a valid shapefile and a fourth file (``.prj``) is required for some applications -- or any type of meaningful visualization.

For ingest, four files are the minimum required:

- ``.shp`` - shape format; the feature geometry itself
- ``.shx`` - shape index format; a positional index of the feature geometry to allow seeking forwards and backwards quickly
- ``.dbf`` - attribute format; columnar attributes for each shape, in dBase IV format
- ``.prj`` - projection format; the coordinate system and projection information, a plain text file describing the projection using well-known text format

Ingest
~~~~~~

When uploaded to a Dataverse installation, the ``.zip`` is unpacked (same as all ``.zip`` files). Shapefile sets are recognized by the same base name and specific extensions. These individual files constitute a shapefile set. The first four are the minimum required (``.shp``, ``.shx``, ``.dbf``, ``.prj``)

For example:

- bicycles.shp    (required extension)
- bicycles.shx    (required extension)
- bicycles.prj	(required extension)
- bicycles.dbf	(required extension)
- bicycles.sbx	(NOT required extension)
- bicycles.sbn	(NOT required extension)

Upon recognition of the four required files, the Dataverse installation will group them as well as any other relevant files into a shapefile set. Files with these extensions will be included in the shapefile set:

- Required: ``.shp``, ``.shx``, ``.dbf``, ``.prj``
- Optional: ``.sbn``, ``.sbx``, ``.fbn``, ``.fbx``, ``.ain``, ``.aih``, ``.ixs``, ``.mxs``, ``.atx``, ``.cpg``, ``.qpj``, ``.qmd``, ``shp.xml``

Then the Dataverse installation creates a new ``.zip`` with mimetype as a shapefile. The shapefile set will persist as this new ``.zip``.

Example
~~~~~~~

**1a.** Original ``.zip`` contents:

A file named ``bikes_and_subways.zip`` is uploaded to the Dataverse installation. This ``.zip`` contains the following files.

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

**1b.** The Dataverse installation unzips and re-zips files:

Upon ingest, the Dataverse installation unpacks the file ``bikes_and_subways.zip``. Upon recognizing the shapefile sets, it groups those files together into new ``.zip`` files:

- files making up the "bicycles" shapefile become a new ``.zip``
- files making up the "subway_line" shapefile become a new ``.zip``
- remaining files will stay as they are

To ensure that a shapefile set remains intact, individual files such as ``bicycles.sbn`` are kept in the set -- even though they are not used for mapping.

**1c.** The Dataverse installation final file listing:

- ``bicycles.zip`` (contains shapefile set #1: ``bicycles.shp``, ``bicycles.shx``, ``bicycles.prj``, ``bicycles.dbf``, ``bicycles.sbx``, ``bicycles.sbn``)
- ``bicycles.txt``  (separate, not part of a shapefile set)
- ``the_bikes.md``  (separate, not part of a shapefile set)
- ``readme.txt``  (separate, not part of a shapefile set)
- ``subway_line.zip``  (contains shapefile set #2: ``subway_line.shp``, ``subway_line.shx``, ``subway_line.prj``, ``subway_line.dbf``)

For two "final" shapefile sets, ``bicycles.zip`` and ``subway_line.zip``, a new mimetype is used:

- Mimetype: ``application/zipped-shapefile``
- Mimetype Label: "Shapefile as ZIP Archive"
