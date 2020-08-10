In previous versions of Dataverse, downloading all files from a dataset via API was a two step process:

- Find all the database id of the files.
- Download all the files, using those ids (comma-separated).

Now you can download all files from a dataset (assuming you have access to them) via API by passing the dataset persistent ID (PID such as DOI or Handle) or the dataset's database id. Versions are also supported like with the "download metadata" API you can pass :draft, :latest, :latest-published, or numbers (1.1, 2.0).
