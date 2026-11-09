Title: "Test Data"
Date: "October 2026"
```

# What are these data files?

Everything a test uploads lives in [`src/test/resources/jsf/`](../src/test/resources/jsf/).
Tests never reference these by a relative path — use the named paths in
[`TestData`](../src/test/java/edu/harvard/iq/dataverse/jsf/support/TestData.java) (`TestData.SAMPLE_TEXT`,
`TestData.LOGO_IMAGE`, …), and add a new constant there when you add a file here.

The content of the files is dummy data; it only matters that they are
non-empty and of the right format.

| File | Used by | Why |
|---|---|---|
| `sample-dataset-file.txt`, `sample-dataset-file-2.txt` | dataset lifecycle, download, preview URL, permissions, Locally FAIR | Two files rather than one because Dataverse only offers its single zip **Download** button once two or more files are selected. |
| `replaced-sample-dataset-file.txt` | dataset lifecycle | Replaces `sample-dataset-file.txt`; afterwards the dataset lists it alongside `sample-dataset-file-2.txt`. |
| `sample-data.csv`, `demo-archive.zip`, `demo-document.pdf`, `demo-code.R`, `ro-crate-metadata.json` | file upload (non-ingest) | Common non-tabular formats. `demo-archive.zip` is unpacked on upload into `readme.txt` and `data.csv`. |
| `demo-data.dta`, `demo-data.RData`, `demo-data.sav`, `demo-data.xlsx` | file upload (tabular) | Formats that trigger Dataverse's tabular ingest. |
| `demo-double-archive.zip`, `demo-geo.zip` | — | Not used yet; kept for future zip-in-zip and geospatial upload tests. |
| `theme/logo.png`, `theme/thumbnail.png`, `theme/footer.png` | collection theme | Images for the Theme + Widgets tests. |
