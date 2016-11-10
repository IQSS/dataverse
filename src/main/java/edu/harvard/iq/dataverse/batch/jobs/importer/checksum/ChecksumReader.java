package edu.harvard.iq.dataverse.batch.jobs.importer.checksum;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import org.beanio.BeanReader;
import org.beanio.StreamFactory;

import javax.annotation.PostConstruct;
import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.AbstractItemReader;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.ejb.EJB;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

@Named
@Dependent
public class ChecksumReader extends AbstractItemReader {

    private static final Logger logger = Logger.getLogger(ChecksumReader.class.getName());

    public static final String FILE_SEPARATOR = System.getProperty("file.separator");

    @Inject
    JobContext jobContext;

    @Inject
    StepContext stepContext;

    @Inject
    @BatchProperty
    String dataDir;

    @Inject
    @BatchProperty
    String checksumManifest;

    @Inject
    @BatchProperty
    String checksumType;

    @EJB
    DatasetServiceBean datasetServiceBean;

    @EJB
    DataFileServiceBean fileService;

    private boolean preflight = true;
    private String persistentUserData = "";

    File directory;

    ArrayList<ChecksumRecord> records = new ArrayList<>();

    Iterator<ChecksumRecord> iterator;

    long currentRecordNumber = 0;

    long totalRecordNumber = 0;

    List<DataFile> dataFileList;

    List<String> missingDataFiles = new ArrayList<>();
    List<String> missingChecksums = new ArrayList<>();

    Dataset dataset;

    @PostConstruct
    public void init() {
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        Properties jobParams = jobOperator.getParameters(jobContext.getInstanceId());
        dataset = datasetServiceBean.findByGlobalId(jobParams.getProperty("datasetId"));
        dataFileList = dataset.getFiles();
    }

    @Override
    public void open(Serializable checkpoint) throws Exception {

        directory = new File(dataDir + dataset.getAuthority() + "/" + dataset.getIdentifier());

        if (preflight()) {
            StreamFactory factory = StreamFactory.newInstance();
            factory.loadResource("mapping.xml");
            BeanReader in = factory.createReader("checksumRecord",
                    new File(this.directory.getAbsolutePath() + FILE_SEPARATOR + checksumManifest));
            ChecksumRecord record;
            while ((record = (ChecksumRecord) in.read()) != null) {
                record.setType(checksumType); // set algorithm, in anticipation of multi-algorithm support in dataverse
                record.setPath(record.getPath().replaceAll("^\\./", "")); // clean up path
                records.add(record);
            }
            in.close();
            iterator = records.iterator();
            currentRecordNumber = 0;
            totalRecordNumber = (long) records.size();
        }

        // report missing checksums or datafile via persistentUserData
        boolean checksumsComplete = checksumManifestComplete();
        if (!checksumsComplete) {
            logger.log(Level.SEVERE, "Checksum and Datafile totals don't match.");
            stepContext.setExitStatus("FAILED");
            if (missingChecksums.size() > 0) {
                logger.log(Level.SEVERE, "FAILED: missing checksums " + missingChecksums.toString());
                persistentUserData += "FAILED: missing checksums " + missingChecksums.toString() + " ";
            }
            if (missingDataFiles.size() > 0) {
                logger.log(Level.SEVERE, "FAILED: missing data files " + missingDataFiles.toString());
                persistentUserData += "FAILED: missing data files " + missingDataFiles.toString() + " ";
            }
        }
    }

    @Override
    public ChecksumRecord readItem() {
        if (iterator != null && iterator.hasNext()) {
            currentRecordNumber++;
            ChecksumRecord record = iterator.next();
            return record;
        } else {
            return null;
        }
    }

    @Override
    public void close() {
        if (!persistentUserData.isEmpty()) {
            stepContext.setPersistentUserData(persistentUserData);
        }
    }

    public boolean preflight() {
        String preflightMessage;
        // make sure the checksum manifest exists
        File manifest = new File(this.directory.getAbsolutePath() + FILE_SEPARATOR + checksumManifest);
        if (!manifest.exists()) {
            this.preflight = false;
            preflightMessage = "The checksum manifest cannot be found: " + manifest.getAbsolutePath();
            logger.log(Level.SEVERE, preflightMessage);
        }
        return preflight;
    }

    private boolean checksumManifestComplete() {

        int dataFiles = dataFileList.size();
        if (totalRecordNumber == dataFiles) {
            return true;
        } else {
            logger.log(Level.SEVERE, "There are " + Integer.toString(dataFiles) + " data files and " +
                    Long.toString(totalRecordNumber) + " checksums.");

            // missing checksums
            for (DataFile datafile : dataFileList) {
                boolean found = false;
                for (ChecksumRecord record : records) {
                    if (datafile.getStorageIdentifier().equals(record.getPath())) {
                        found = true;
                        break;
                    }
                }
                if (!found) { missingChecksums.add(datafile.getStorageIdentifier()); }
            }

            // missing data files
            for (ChecksumRecord record : records) {
                boolean found = false;
                for (DataFile datafile : dataFileList) {
                    if (datafile.getStorageIdentifier().equals(record.getPath())) {
                        found = true;
                        break;
                    }
                }
                if (!found) { missingDataFiles.add(record.getPath()); }
            }
            return false;
        }
    }

}
