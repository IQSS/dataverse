/* 
For ticket #2597 add records to the setting table to control whether maps can be created 
or viewed - that is the rendering of those buttons, when all of the other requirements for rendering
have been fulfilled - SEK
 */

INSERT INTO setting(
            name, content)
    VALUES (':GeoconnectCreateEditMaps', true);

INSERT INTO setting(
            name, content)
    VALUES (':GeoconnectViewMaps', true);




