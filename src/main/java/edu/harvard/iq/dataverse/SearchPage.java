package edu.harvard.iq.dataverse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

@ViewScoped
@Named("SearchPage")
public class SearchPage implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(SearchPage.class.getCanonicalName());

    private String query;
    private List<Dataverse> dataverses = new ArrayList();
    private List<Dataset> datasets = new ArrayList<>();
    private List<DataverseUser> dataverseUsers = new ArrayList<>();
    private List<DataFile> dataFiles = new ArrayList<>();
    private List<String> facets = new ArrayList<>();
    private List<String> spelling_alternatives = new ArrayList<>();

    @EJB
    SearchServiceBean searchService;
    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    DatasetServiceBean datasetService;

    public SearchPage() {
        logger.info("SearchPage initialized. Query: " + query);
    }

    public void search() {
        logger.info("Search button clicked. Query: " + query);
        /**
         * @todo remove this? What about pagination for many, many results?
         */
        dataverses = new ArrayList();
        datasets = new ArrayList();
        facets = new ArrayList<>();

        query = query == null ? "*" : query;
        SolrQueryResponse solrQueryResponse = searchService.search(query);
        List<SolrSearchResult> searchResults = solrQueryResponse.getSolrSearchResults();
        for (Map.Entry<String, List<String>> entry : solrQueryResponse.getSpellingSuggestionsByToken().entrySet()) {
            spelling_alternatives.add(entry.getValue().toString());
        }
        for (String facet : solrQueryResponse.getFacets()) {
            facets.add(facet);
        }
        for (SolrSearchResult searchResult : searchResults) {
            String type = searchResult.getType();
            switch (type) {
                case "dataverses":
                    Dataverse dataverse = dataverseService.find(searchResult.getEntityId());
                    if (searchResult.getHighlightSnippets() != null) {
                        /**
                         * @todo when does long description truncate?
                         */
                        dataverse.setDescription(searchResult.getHighlightSnippets().get(0));
                    }
                    dataverses.add(dataverse);
                    break;
                case "datasets":
                    /**
                     * @todo add highlighting?
                     */
                    Dataset dataset = datasetService.find(searchResult.getEntityId());
                    datasets.add(dataset);
                    break;
                default:
                    break;
            }
        }

        /**
         * @todo fill this in with real search results
         */
        dataverseUsers = new ArrayList();
        dataFiles = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Long id = new Long(String.valueOf(i));

            DataverseUser dvUser = new DataverseUser();
            dvUser.setId(id);
            dvUser.setUserName("user" + i);
            dataverseUsers.add(dvUser);

            DataFile dataFile = new DataFile();
            dataFile.setId(id);
            dataFile.setName("file" + i);
            dataFiles.add(dataFile);

        }
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public List<Dataverse> getDataverses() {
        return dataverses;
    }

    public void setDataverses(List<Dataverse> dataverses) {
        this.dataverses = dataverses;
    }

    public List<Dataset> getDatasets() {
        return datasets;
    }

    public void setDatasets(List<Dataset> datasets) {
        this.datasets = datasets;
    }

    public List<DataverseUser> getDataverseUsers() {
        return dataverseUsers;
    }

    public void setDataverseUsers(List<DataverseUser> dataverseUsers) {
        this.dataverseUsers = dataverseUsers;
    }

    public List<DataFile> getDataFiles() {
        return dataFiles;
    }

    public void setDataFiles(List<DataFile> dataFiles) {
        this.dataFiles = dataFiles;
    }

    public List<String> getFacets() {
        return facets;
    }

    public void setFacets(List<String> facets) {
        this.facets = facets;
    }

    public List<String> getSpelling_alternatives() {
        return spelling_alternatives;
    }

    public void setSpelling_alternatives(List<String> spelling_alternatives) {
        this.spelling_alternatives = spelling_alternatives;
    }

}
