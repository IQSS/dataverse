Incremental improvements have been made to the process of registering dataset metadata with DataCite. If your instance is using DataCite, please make sure you have a valid DataCite REST API url configured, since it is now required.

The JVM option(s) in question are `dataverse.pid.*.datacite.rest-api-url` if the recommended, new-style pid configuration is used, or `doi.dataciterestapiurlstring` if the legacy settings are in place. In the latter case however, this may be a good occasion to switch to the new configuration setup.

For instances using registered DataCite authorities in production the url should be:

```<jvm-options>-Ddataverse.pid.<providername>.datacite.rest-api-url=https://api.datacite.org</jvm-options>```

Or, for test and development instances:

```<jvm-options>-Ddataverse.pid.<providername>.datacite.rest-api-url=https://api.test.datacite.org</jvm-options>```