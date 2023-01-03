# Improved Security for External Tools

This release adds support for configuring external tools to use signed URLs to access the Dataverse API. This eliminates the need for tools to have access to the user's apiToken in order to access draft or restricted datasets and datafiles. Signed URLS can be transferred via POST or via a callback when triggering a tool via GET.