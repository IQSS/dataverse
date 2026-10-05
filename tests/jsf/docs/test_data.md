Title: "Test Data"
Author: "Snehashish Reddy Manda"
Email: "msreddy@unc.edu"
Date: "June 2026"
```

# What are these data files?

There are three test data files in [`tests/suite/test-data/`](../tests/suite/test-data/) used by the `@21cfr` dataset action tests.

The content inside these files is dummy data and does not matter for the purposes of testing as long as the files themselves are not empty.

---

## Why do these three files matter?

They are used in the combined **Dataset Actions** test (`13-dataset-actions.spec.ts`) which covers tests #7 through #11 of the 21 CFR Part 11 compliance suite.

### Test #7 — Create Dataset

| File | Purpose |
|---|---|
| `sample-dataset-file.txt` | First file uploaded during dataset creation |
| `sample-dataset-file-2.txt` | Second file uploaded during dataset creation |

Two files are used rather than one for a specific reason: **zip downloads**. Downloading a zip file containing multiple `.txt` files is easier to automate than downloading a single file because Dataverse renders a distinct **Download** button when two or more files are selected. That button is straightforward to target as a Playwright selector and lets the download verification test (test #15) work reliably with minimal flakiness. The button is only enabled at a minimum of two files, so we use the bare minimum to verify the user flow.

### Test #10 — Replace File

| File | Purpose |
|---|---|
| `replaced-sample-dataset-file.txt` | Replacement for `sample-dataset-file.txt` in the existing dataset |

After the replace operation the dataset should still contain two files: `replaced-sample-dataset-file.txt` and `sample-dataset-file-2.txt`. All other dataset behavior should remain unaffected.
