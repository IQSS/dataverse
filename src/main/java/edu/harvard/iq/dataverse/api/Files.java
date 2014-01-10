package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

@Path("files")
public class Files {

    private static final Logger logger = Logger.getLogger(Files.class.getCanonicalName());

    @EJB
    DatasetServiceBean datasetService;

    @POST
    public String add(DataFile dataFile) {
        Dataset dataset = datasetService.find(dataFile.getDataset().getId());
        List<DataFile> newListOfFiles = dataset.getFiles();
        newListOfFiles.add(dataFile);
        dataset.setFiles(newListOfFiles);
        datasetService.save(dataset);
        return "dataset " + dataFile.getName() + " files updated (and hopefully indexed)\n";
    }

}
