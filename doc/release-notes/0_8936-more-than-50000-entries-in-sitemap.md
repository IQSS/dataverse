Dataverse can now handle more than 50,000 items when generating sitemap files, splitting the content across multiple files to comply with the Sitemap protocol.

For details see https://dataverse-guide--10321.org.readthedocs.build/en/10321/installation/config.html#creating-a-sitemap-and-submitting-it-to-search-engines #8936 and #10321.

## Upgrade instructions

If your installation has more than 50,000 entries, you should re-submit your sitemap URL to Google or other search engines. The file in the URL will change from ``sitemap.xml`` to ``sitemap_index.xml``.

As explained at https://dataverse-guide--10321.org.readthedocs.build/en/10321/installation/config.html#creating-a-sitemap-and-submitting-it-to-search-engines this is the command for regenerating your sitemap:

`curl -X POST http://localhost:8080/api/admin/sitemap`
