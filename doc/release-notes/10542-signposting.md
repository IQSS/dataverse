# Signposting Output Now Contains Links to All Dataset Metadata Export Formats

When Signposting was added in Dataverse 5.14 (#8981), it only provided links for the `schema.org` metadata export format.

The output of HEAD, GET, and the Signposting "linkset" API have all been updated to include links to all available dataset metadata export formats (including any external exporters, such as Croissant, that have been enabled).

This provides a lightweight machine-readable way to first retrieve a list of links (via a HTTP HEAD request, for example) to each available metadata export format and then follow up with a request for the export format of interest.

In addition, the content type for the `schema.org` dataset metadata export format has been corrected. It was `application/json` and now it is `application/ld+json`.

See also [the docs](https://preview.guides.gdcc.io/en/develop/api/native-api.html#retrieve-signposting-information) and #10542.
