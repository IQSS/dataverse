Files downloaded from Binder are now in their original format.

For example, data.dta (a Stata file) will be downloaded instead of data.tab (the archival version Dataverse creates as part of a successful ingest).

This should make it easier to write code to reproduce results as the dataset authors and subsequent researchers are likely operating on the original file format rather that the format that Dataverse creates.

For details, see #9374, <https://github.com/jupyterhub/repo2docker/issues/1242>, and <https://github.com/jupyterhub/repo2docker/pull/1253>.
