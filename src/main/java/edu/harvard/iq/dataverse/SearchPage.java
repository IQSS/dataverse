package edu.harvard.iq.dataverse;

import java.util.ArrayList;
import java.util.List;
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

    @EJB
    SearchServiceBean searchService;
    @EJB
    DataverseServiceBean dataverseService;

    public SearchPage() {
        logger.info("SearchPage initialized. Query: " + query);
    }

    public void search() {
        logger.info("Search button clicked. Query: " + query);
        /**
         * @todo remove this? What about pagination for many, many results?
         */
        dataverses = new ArrayList();

        query = query == null ? "*" : query;
        List<SolrSearchResult> searchResults = searchService.search(query);
        for (SolrSearchResult searchResult : searchResults) {
            Dataverse dataverse = dataverseService.find(searchResult.getId());
            if (searchResult.getHighlightSnippets() != null) {
                dataverse.setDescription(searchResult.getHighlightSnippets().get(0));
            } else {
                dataverse.setDescription(dataverse.getDescription());
            }
            dataverse.setAlias(searchResult.getName());
            dataverses.add(dataverse);

        }

        /**
         * @todo fill this in with real search results
         */
        datasets = new ArrayList();
        dataverseUsers = new ArrayList();
        dataFiles = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Long id = new Long(String.valueOf(i));

            Dataset dataset = new Dataset();
            dataset.setId(id);
            dataset.setTitle("dataset" + i);
            datasets.add(dataset);

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

}
