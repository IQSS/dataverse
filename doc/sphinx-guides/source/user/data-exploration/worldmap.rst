.. _world-map:

WorldMap: Geospatial Data Exploration
+++++++++++++++++++++++++++++++++++++

WorldMap
========

`WorldMap <http://worldmap.harvard.edu/>`_ is developed by the Center for Geographic Analysis (CGA) at Harvard and is an open source software that helps researchers visualize and explore their data in maps. The WorldMap and Dataverse collaboration allows researchers to be able to upload shapefiles to Dataverse for long term storage and receive a persistent identifier (through DOI) as well as be able to easily move into WorldMap to interact with the data and save to WorldMap as well. GeoConnect is the platform integrating Dataverse and WorldMap together and what you will use to visualize your data.

Uploading Shapefiles to Dataverse
=================================

To get started, you will need to create a dataset in Dataverse. For more detailed instructions on creating a dataset, read the `Dataset + File Management <http://guides.dataverse.org/en/latest/user/dataset-management.html>`_ portion of this user guide.

Dataverse recognizes ZIP files that contain the components of a shapefile and will ingest them as a ZIP.

Once you have uploaded your ZIP files comprising a shapefile, a Map Data button will appear next to the file in the dataset.

Mapping your data with Geoconnect
=================================

In order to use the WorldMap and Dataverse integration, your dataset will need to be published. Once it has been published, you will be able to use the MapData button. Click on the Map Data button to be brought to GeoConnect, the portal between Dataverse and WorldMap that will process your shapefile and send it to WorldMap. 

To get started with visualizing your shapefile, click on the blue Visualize on WorldMap button in GeoConnect. It may take 30 seconds or longer for the data to be sent to WorldMap and then back to GeoConnect

Once the visualizing has finished, you will be able to style your map through Attribute, Classification Method, Number of Intervals, and Colors. At any time, you can view the map on WorldMap if you would like to see how it will be displayed there.

After styling your map, you can delete it or return to Dataverse. If you decide to delete the map, it will no longer appear on WorldMap. By returning to Dataverse, you will send the styled map layer to WorldMap as well as to Dataverse where a preview will be available of the map layer you styled using GeoConnect.

To map the shapefile again, all you will need to do is click the Map Data button again. 
