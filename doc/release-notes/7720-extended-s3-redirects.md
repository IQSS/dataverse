### Extended support for S3 Download Redirects ("Direct Downloads").

If your installation uses S3 for storage, and have "direct downloads" enabled, please note that, as of this release, it will cover the following download types that were not handled by redirects in the earlier versions: saved originals of tabular data files, cached RData frames, resized thumbnails for image files and other auxiliary files. In other words, all the forms of the file download API that take extra arguments, such as "format" or "imageThumb" - for example:
`/api/access/datafile/12345?format=original`
`/api/access/datafile/:persistentId?persistentId=doi:1234/ABCDE/FGHIJ?imageThumb=true`
etc., that were previously excluded.

This change should not in any way affect the web GUI users. Since browsers follow redirects automatically. Some API users may experience problems, if they use it in a way that does not expect to receive a redirect response. For example, if a user has a script where they expect to download a saved original of an ingested tabular file with the following command:

`curl https://yourhost.edu/api/access/datafile/12345?format=original > orig.dta`

it will fail to save the file when it receives a 303 (redirect) response instead of 200. So they will need to add "-L" to the command line above, to instruct curl to follow redirects:

`curl -L https://yourhost.edu/api/access/datafile/12345?format=original > orig.dta`

Most of your API users have likely figured it out already, since you enabled S3 redirects for "straightforward" downloads in your installation. But we feel it was worth a heads up, just in case. 
