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

INSERT INTO worldmapauth_tokentype
(   name,
    created,
    contactemail, hostname, ipaddress, 
    mapitlink, md5,
    modified, timelimitminutes)
    VALUES ( 'GEOCONNECT-DEFAULT', current_timestamp, 
        'support@dataverse.org',  'geoconnect.datascience.iq.harvard.edu', '140.247.115.127', 
	'http://geoconnect.datascience.iq.harvard.edu/shapefile/map-it',
	'38c0a931b2d582a5c43fc79405b30c22',
            current_timestamp, 30);


