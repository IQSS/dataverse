Search Services
===============
Dataverse now supports configurable search services, allowing developers to integrate new search engines dynamically. This guide outlines the design and provides details on how to use the interfaces and classes involved.
Design Overview
---------------
The configurable search services feature is designed to allow:
1. Dynamic addition of new search engines
2. Configuration of the Dataverse UI to use a specified search engine
3. Use of different search engines via the API
4. Discovery of installed search engines
Key Components
--------------
1. SearchService Interface
^^^^^^^^^^^^^^^^^^^^^^^^^^
The ``SearchService`` interface is the core of the configurable search services. It defines the methods that any search engine implementation must provide.

.. code-block:: java

   public interface SearchService {
       String getServiceName();
       String getDisplayName();
       
       SolrQueryResponse search(DataverseRequest dataverseRequest, List<Dataverse> dataverses, String query,
               List<String> filterQueries, String sortField, String sortOrder, int paginationStart,
               boolean onlyDatatRelatedToMe, int numResultsPerPage, boolean retrieveEntities, String geoPoint,
               String geoRadius, boolean addFacets, boolean addHighlights) throws SearchException;

       default void setSolrSearchService(SearchService solrSearchService);
   }

The interface allows you to provide a service name and dsiplay name, and to respond to the same search parameters that are normally sent to the solr search engine.

The setSolrSearchService method is used by Dataverse to give your class a reference to the SolrSearchService, allowing your class to perform solr queries as needed. (See the ExternalSearchService engine for an example.)

2. ConfigurableSearchService Interface
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The ``ConfigurableSearchService`` interface extends the ``SearchService`` interface and adds a method for Dataverse to set the ``SettingsServiceBean``. This allows search services to be configurable through Dataverse settings.

.. code-block:: java

   public interface ConfigurableSearchService extends SearchService {
       void setSettingsService(SettingsServiceBean settingsService);
   }

The ExternalSearchService class provides a use case for this.

3. JVM Options for Search Configuration
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
Dataverse uses two JVM options to configure the search functionality:

- ``dataverse.search.services.directory``: Specifies the local directory where jar files with search engines (classes implementing the SearchService interface) can be found. Dataverse will dynamically load engines from this directory.

- ``dataverse.search.default-service``: The serviceName of the service that should be used in the Dataverse UI.

Example configuration:

.. code-block:: bash

   ./asadmin create-jvm-options "-Ddataverse.search.services.directory=/var/lib/dataverse/searchServices"
   ./asadmin create-jvm-options "-Ddataverse.search.default-service=solr"

Remember to restart your Payara server after modifying these JVM options for the changes to take effect.

4. Using Different Search Engines via API
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The loaded search services can be discovered using the /api/search/services endpoint.

Queries can be made to different engines by including the optional search_service=<serviceName> query parameter.

Available Search Services
-------------------------

The class definitions for four example search services are included in the Dataverse repository.
They are not included in the Dataverse .war file but can be built as two separate .jar files using

.. code-block:: bash 

    mvn clean package -DskipTests=true -Pexternal-search

or

.. code-block:: bash 

    mvn clean package -DskipTests=true -Ptrivial-search-examples

1. ExternalSearchServiceBean
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

2. PostExternalSearchServiceBean
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

These classes implement the ConfigurableSearchService interface.
They make a GET or POST call (respectively) to an external search engine that must return a JSON array of objects with "DOI" and "Distance" keys.
The query sent to the external engine use the same query parameters as the Dataverse search API (GET) or have a JSON payload with those keys (POST).
The results they return are then searched for using the solr search engine which enforces access control and provides the standard formatting expected by the Dataverse UI and API.
The Distance values are used to order the results, smallest distances first. 

They can be cofigured via 2 settings:
        :ExternalSearchUrl - the URL to send search queries to
        :ExternalSearchName - the display name to use for this configuration

As these classes use DOIs as identifiers, they cannot reference collections or, unless file DOIs are enabled, files.
Similar classes, or extensions of these classes could search by database ids instead, etc. to support the additional types.

Best Practices
--------------
1. Always implement proper access control in your search engine
2. Ensure your search results are consistent with Dataverse's data model
3. Thoroughly test your implementation with various query types and datasets
Conclusion
----------
The configurable search services feature provides a flexible way to integrate new search capabilities into Dataverse. By following this guide, developers can create and integrate custom search engines to enhance Dataverse's functionality.