-- For #2957, additional columns for mapping of tabular data
--   > Distinguishes a mapped Tabular file from a shapefile
ALTER TABLE maplayermetadata ADD COLUMN isjoinlayer BOOLEAN default false;
--   > Description of the tabular join.  e.g. joined to layer XYZ on column TRACT, etc
ALTER TABLE maplayermetadata ADD COLUMN joindescription TEXT default NULL;
--   > For all maps, store the WorldMap links to generate alternative versions,
--      e.g. PNG, zipped shapefile, GeoJSON, Excel, etc
ALTER TABLE maplayermetadata ADD COLUMN maplayerlinks TEXT default NULL;

