package edu.harvard.iq.dataverse;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

@ViewScoped
@Named("SearchPage")
public class SearchPage {

    private static final Logger logger = Logger.getLogger(SearchPage.class.getCanonicalName());

    private String query;
    private List<Dataverse> dataverses = new ArrayList();
    private List<Dataset> datasets = new ArrayList<>();
    private List<DataverseUser> dataverseUsers = new ArrayList<>();
    private List<DataFile> dataFiles = new ArrayList<>();

    public SearchPage() {
        for (int i = 0; i < 5; i++) {
            Long id = new Long(String.valueOf(i));

            Dataverse dataverse = new Dataverse();
            dataverse.setId(id);
            dataverse.setAlias("dataverse" + i);
            dataverses.add(dataverse);

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
            dataFile.setName("datafile" + i);
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

    public void search() {
        logger.info("search...");
    }
}
