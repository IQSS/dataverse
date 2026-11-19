## Report Metadata Setup Failures

Metadata setup now checks HTTP responses as well as curl's exit status. Failed requests report the metadata block name, HTTP status, and response body. All standard blocks are attempted, and setup returns a non-zero exit status if any request fails.

Bootstrap and the installer continue the remaining instance configuration and security lockdown after a metadata-loading failure, then return a non-zero exit status rather than reporting successful completion. Development bootstrap retains its intentionally insecure configuration. Installer diagnostics remain available in `setup-all.*.log`. Metadata-update jobs such as #12467 stop before the Solr update on failure. Successful block uploads are not rolled back. No manual upgrade steps are required.

For more information, see #12804.
