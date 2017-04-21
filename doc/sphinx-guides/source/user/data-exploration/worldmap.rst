.. _world-map:

WorldMap: Geospatial Data Exploration
+++++++++++++++++++++++++++++++++++++

Dataverse and Worldmap
======================

`WorldMap <http://worldmap.harvard.edu/>`_ is developed by the Center for Geographic Analysis (CGA) at Harvard and is an open source software that helps researchers visualize and explore their data in maps. The WorldMap and Dataverse collaboration allows researchers to upload shapefiles or tabular files to Dataverse for long term storage and receive a persistent identifier (through DOI), then easily move into WorldMap to interact with the data and save to WorldMap as well.

Geoconnect
==========

GeoConnect is a platform that integrates Dataverse and WorldMap together, allowing researchers to visualize their geospatial data. GeoConnect can be used to create maps of shapefiles or of tabular files containing geospatial information.

Mapping shapefiles with GeoConnect
----------------------------------

GeoConnect is capable of mapping shapefiles which are uploaded to Dataverse in .zip format. Specifically, Dataverse recognizes a zipped shapefile by:

1.Examining the contents of the .zip file
2.Checking for the existence of four similarly named files with the following extensions: .dbf, .prj, .shp, .shx

Once you have uploaded your .zip shapefile, a Map Data button will appear next to the file in the dataset. In order to use this button, you'll need to publish your dataset. Once your dataset has been published, you can click on the Map Data button to be brought to GeoConnect, the portal between Dataverse and WorldMap that will allow you to create your map. 

To get started with visualizing your shapefile, click on the blue "Visualize on WorldMap" button in GeoConnect. It may take up to 45 seconds for the data to be sent to WorldMap and then back to GeoConnect.

Once this process has finished, you will be taken to a new page where you can style your map through Attribute, Classification Method, Number of Intervals, and Colors. Clicking "View on WorldMap" will open WorldMap in a new tab, allowing you to see how your map will be displayed there.

After styling your map, you can either save it by clicking "Return to Dataverse" or delete it with the "Delete" button. If you decide to delete the map, it will no longer appear on WorldMap. Returning to Dataverse will send the styled map layer to both Dataverse and Worldmap. A preview of your map will now be visible on your file page and your dataset page.

To replace your shapefile's map with a new one, simply click the Map Data button again. 

Mapping tabular files with GeoConnect
---------------------------------

Tabular files need a bit more preparation before they can be mapped in GeoConnect.