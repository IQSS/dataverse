 ## Release Highlights

### A new file access API

A new api offers *crawlable* access view of the folders and files within a datset:

```
  /api/datasets/<dataset id>/dirindex/
```

will output a simple html listing, based on the standard Apache
directory index, with Access API download links for individual files,
and recursive calls to the API above for sub-folders. (See the
documentation entry in the guides for more information).

Using this API, ``wget --recursive`` (or similar crawling client) can
be used to download all the files in a dataset, preserving the file
names and folder structure; without having to use the download-as-zip
API. In addition to being faster (zipping is a relatively
resource-intensive operation on the server side), this process can be
restarted if interrupted (with ``wget --continue`` or equivalent) -
unlike zipped multi-file downloads that always have to start from the
beginning.

On a system that uses S3 with download redirects, the individual file
downloads will be handled by S3 directly, without having to be proxied
through the Dataverse application.


