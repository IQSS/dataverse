Search API (/api/search) responses for Datafiles include image_url for the thumbnail if each of the following are true:
1. The DataFile is not Harvested
2. A Thumbnail is available for the Datafile
3. If the Datafile is Restricted then the caller must have Download File Permission for the Datafile
4. The Datafile is NOT actively embargoed
5. The Datafile's retention period has NOT expired

See also #10875 and #10886.
