Across the application, the Geospatial metadata block fields for north and south were labeled incorrectly as ‘Longitudes,’ as reported on #5645. After updating to this version of Dataverse, users will need to update all the endpoints that used ‘northLongitude’ and ‘southLongitude’ to ‘northLatitude’ and ‘southLatitude,’ respectively.


TODO: Whoever puts the release notes together should make sure there is the standard note about updating the schema after upgrading.