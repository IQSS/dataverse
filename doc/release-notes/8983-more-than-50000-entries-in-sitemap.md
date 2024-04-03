The sitemap file generation can handle more than 50,000 entries if needed with the [sitemapgen4j](https://github.com/gdcc/sitemapgen4j) library, maintained by the Global Dataverse Community Consortium.

In this case, the Dataverse Admin API `api/admin/sitemap` create a sitemap index file, called `sitemap_index.xml`, in place of the single `sitemap.xml` file. This created file reference multiples simple sitemap file, named ``sitemap1.xml``, ``sitemap2.xml``, etc. This referenced files will be as many files as necessary to contain the URLs of dataverses and datasets presents your installation, while respecting the limit of 50,000 URLs per file. See the [config section of the Installation Guide](https://guides.dataverse.org/en/latest/installation/config.html#creating-a-sitemap-and-submitting-it-to-search-engines) for details.

A HTML preview can be found [here](https://dataverse-guide--10321.org.readthedocs.build/en/10321/installation/config.html#creating-a-sitemap-and-submitting-it-to-search-engines).

For more information, see [#8936](https://github.com/IQSS/dataverse/issues/8936).
