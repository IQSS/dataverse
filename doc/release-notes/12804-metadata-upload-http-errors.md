## Report Metadata Setup Failures

Metadata setup now checks HTTP responses as well as curl's exit status. Failed requests report the metadata block name, HTTP status, and response body. All standard blocks are attempted, and setup returns a non-zero exit status if any request fails.

Bootstrap and the installer's post-deployment setup now stop when metadata loading fails instead of reporting successful completion. Installer diagnostics remain available in `setup-all.*.log`. No manual upgrade steps are required.

For more information, see #12804.
