### Embedded DVWebloader V2 (Experimental)

A new feature flag `embed-webloader-v2` has been added that enables embedding DVWebloader V2 directly within the Dataverse edit files page using an iframe, instead of opening it in a popup window.

This provides a more seamless file upload experience while maintaining full isolation of styles and security (the API token is passed via the iframe URL, not exposed in the main page source).

#### Requirements

1. The `:WebloaderUrl` setting must be configured and contain "v2" in the URL path
2. The feature flag must be enabled: `-Ddataverse.feature.embed-webloader-v2=true`

#### Configuration

To enable embedded DVWebloader V2:

```bash
# Set the WebloaderUrl (must contain "v2")
curl -X PUT -d 'https://your-host/dvwebloader/dvwebloaderV2.html' \
  http://localhost:8080/api/admin/settings/:WebloaderUrl

# Enable the feature flag (via JVM option)
# Add to your domain.xml or Payara configuration:
# -Ddataverse.feature.embed-webloader-v2=true
```

#### S3 Tagging Support

DVWebloader V2 also respects the `dataverse.files.<driverId>.disable-tagging` JVM setting. If S3 tagging is disabled for a storage driver (e.g., for MinIO or S3-compatible storage that doesn't support tagging), the embedded uploader will automatically disable tagging for uploads to that storage.

See also :ref:`feature-flags` and :ref:`:WebloaderUrl` in the Installation Guide.
