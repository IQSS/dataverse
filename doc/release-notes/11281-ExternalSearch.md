### Configurable Search Services

Dataverse now has an experimental capability to dynamically add and configure new search engines.
The current Dataverse user interface can be configured to use a specified search engine instead of the built-in solr search.
The search API now supports an optional &searchEngine query parameter that allows using any configured search engine.
An additional /api/search/engines endpoint allows discovery of the engines installed.

In addition to two trivial example engines designed for testing, Dataverse ships with two search engine classes that support calling an enternally-hosted search service (via HTTP GET or POST).
These classes rely on the internal solr search to perform access-control and to format the final results, simplifying development of such an external engine.

Details about the new functionality are described in 