### Search by License

A browse/search facet called "License" has been added and will be displayed as long as there is more than one license in datasets and datafiles in browse/search results. This facet allow you to filter by license such as CC0, etc.
Also, the Search API now handles license filtering using the `fq` parameter, for example : `/api/search?q=*&fq=license%3A%22CC0+1.0%22` for CC0 1.0. See PR #10204


