## CORS Filter Fix

Fixed an inconsistency where the `CorsFilter` was not always being invoked when accessing `/api/...` endpoints, preventing these endpoints from being used from webapps even when CORS was properly configured.

## Updates for Documentation Writers

A new dependency called "sphinx-reredirects" has been added. Please re-run the `pip install -r requirements.txt` setup [step](https://dataverse-guide--12151.org.readthedocs.build/en/12151/contributor/documentation.html#building-the-guides-with-sphinx) to update your environment. Otherwise you might see an error like `Could not import extension sphinx_reredirects`.
