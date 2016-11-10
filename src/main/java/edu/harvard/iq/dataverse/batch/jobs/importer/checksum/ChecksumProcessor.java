package edu.harvard.iq.dataverse.batch.jobs.importer.checksum;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;

import javax.annotation.PostConstruct;
import javax.batch.api.chunk.ItemProcessor;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.context.JobContext;
import javax.ejb.EJB;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

@Named
@Dependent
public class ChecksumProcessor implements ItemProcessor {

    private static final Logger logger = Logger.getLogger(ChecksumProcessor.class.getName());

    @Inject
    JobContext jobContext;

    @EJB
    DatasetServiceBean datasetServiceBean;

    @EJB
    DataFileServiceBean fileServiceBean;

    Dataset dataset;

    List<DataFile> dataFileList;
    
    @PostConstruct
    public void init() {
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        Properties jobParams = jobOperator.getParameters(jobContext.getInstanceId());
        dataset = datasetServiceBean.findByGlobalId(jobParams.getProperty("datasetId"));
        dataFileList = dataset.getFiles();
    }

    @Override
    public Object processItem(Object object) throws Exception {

        ChecksumRecord record = (ChecksumRecord) object;
        String value = record.getValue();
        String path = record.getPath();
        DataFile dataFile = fileServiceBean.findByStorageIdandDatasetVersion(path, dataset.getLatestVersion());

        // set the checksum
        if (dataFile != null) {
            if (dataFile.getChecksumValue().equalsIgnoreCase(value)) {
                return null; // skip it, it was already set
            } else {
                dataFile.setChecksumValue(value);
                return dataFile;
            }
        } else {
            logger.log(Level.SEVERE, "Can't find the DataFile for: " + path);
            return null;
        }

    }

}

