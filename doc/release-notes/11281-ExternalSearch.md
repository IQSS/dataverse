### Configurable Search Services

Dataverse now has an experimental capability to dynamically add and configure new search engines.
The current Dataverse user interface can be configured to use a specified search engine instead of the built-in solr search.
The search API now supports an optional &searchService query parameter that allows using any configured search engine.
An additional /api/search/services endpoint allows discovery of the services installed.

In addition to two trivial example services designed for testing, Dataverse ships with two search engine classes that support calling an externally-hosted search service (via HTTP GET or POST).
These classes rely on the internal solr search to perform access-control and to format the final results, simplifying development of such an external engine.

Details about the new functionality are described in https://guides.dataverse.org/en/latest/developers/search-services.html

## Settings

### Database Settings:

***New:***

- :GetExternalSearchUrl
- :GetExternalSearchName
- :PostExternalSearchUrl
- :PostExternalSearchName

### New Configuration Options

- `dataverse.search.services.directory`
- `dataverse.search.default-service`