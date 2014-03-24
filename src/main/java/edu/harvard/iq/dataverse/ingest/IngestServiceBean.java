/*
   Copyright (C) 2005-2012, by the President and Fellows of Harvard College.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

   Dataverse Network - A web application to share, preserve and analyze research data.
   Developed at the Institute for Quantitative Social Science, Harvard University.
   Version 3.0.
*/

package edu.harvard.iq.dataverse.ingest;

import edu.harvard.iq.dataverse.datavariable.VariableServiceBean;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetPage;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.FileMetadataField;
import edu.harvard.iq.dataverse.FileMetadataFieldValue;
import edu.harvard.iq.dataverse.dataaccess.ImageThumbConverter;
import edu.harvard.iq.dataverse.dataaccess.TabularSubsetGenerator;
import edu.harvard.iq.dataverse.datavariable.SummaryStatistic;
import edu.harvard.iq.dataverse.datavariable.DataVariable;
import edu.harvard.iq.dataverse.ingest.metadataextraction.FileMetadataExtractor;
import edu.harvard.iq.dataverse.ingest.metadataextraction.impl.plugins.fits.FITSFileMetadataExtractor;
import edu.harvard.iq.dataverse.ingest.tabulardata.TabularDataFileReader;
import edu.harvard.iq.dataverse.ingest.tabulardata.TabularDataIngest;
import edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.dta.DTAFileReader;
import edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.dta.DTAFileReaderSpi;
import edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.rdata.RDATAFileReader;
import edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.rdata.RDATAFileReaderSpi;
import edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.csv.CSVFileReader;
import edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.csv.CSVFileReaderSpi;
import edu.harvard.iq.dataverse.util.SumStatCalculator;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.primefaces.model.UploadedFile;

/**
 *
 * @author Leonid Andreev
 * dataverse 4.0
 * New service for handling ingest tasks
 * 
 */
@Stateless
@Named
public class IngestServiceBean {
    private static final Logger logger = Logger.getLogger(IngestServiceBean.class.getCanonicalName());
    @EJB
    VariableServiceBean variableService;
    @EJB 
    DatasetServiceBean datasetService;
    @EJB
    DatasetFieldServiceBean fieldService;
    
    // TODO: this constant should be provided by the Ingest Service Provder Registry;
    private static final String METADATA_SUMMARY = "FILE_METADATA_SUMMARY_INFO";
    
    public void produceSummaryStatistics(DataFile dataFile) throws IOException {
        //produceDiscreteNumericSummaryStatistics(dataFile); 
        produceContinuousSummaryStatistics(dataFile);
        //produceCharacterSummaryStatistics(dataFile);
    }
    
    public void produceContinuousSummaryStatistics(DataFile dataFile) throws IOException {

        Double[][] variableVectors = subsetContinuousVectors(dataFile);

        calculateContinuousSummaryStatistics(dataFile, variableVectors);

    }
    
    public boolean ingestAsTabular(String tempFileLocation, DataFile dataFile) throws IOException {
        boolean ingestSuccessful = false;

        // Locate ingest plugin for the file format by looking
        // it up with the Ingest Service Provider Registry:
        //TabularDataFileReader ingestPlugin = IngestSP.getTabDataReaderByMIMEType(dFile.getContentType());
        //TabularDataFileReader ingestPlugin = new DTAFileReader(new DTAFileReaderSpi());
        TabularDataFileReader ingestPlugin = getTabDataReaderByFileNameExtension(dataFile.getName());

        if (ingestPlugin == null) {
            throw new IOException("Could not find ingest plugin for the file " + dataFile.getName());
        }

        FileInputStream tempFileInputStream = null; 
        
        try {
            tempFileInputStream = new FileInputStream(new File(tempFileLocation));
        } catch (FileNotFoundException notfoundEx) {
            throw new IOException("Could not open temp file "+tempFileLocation);
        }
        
        TabularDataIngest tabDataIngest = ingestPlugin.read(new BufferedInputStream(tempFileInputStream), null);

        if (tabDataIngest != null) {
            File tabFile = tabDataIngest.getTabDelimitedFile();

            if (tabDataIngest.getDataTable() != null
                    && tabFile != null
                    && tabFile.exists()) {

                Logger.getLogger(DatasetPage.class.getName()).log(Level.INFO, "Tabular data successfully ingested; DataTable with "
                        + tabDataIngest.getDataTable().getVarQuantity() + " variables produced.");

                Logger.getLogger(DatasetPage.class.getName()).log(Level.INFO, "Tab-delimited file produced: " + tabFile.getAbsolutePath());

                tabDataIngest.getDataTable().setOriginalFileFormat(determineMimeType(dataFile));

                dataFile.setName(dataFile.getName().replaceAll("\\.dta$", ".tab"));
                dataFile.setName(dataFile.getName().replaceAll("\\.RData", ".tab"));
                dataFile.setName(dataFile.getName().replaceAll("\\.csv", ".tab"));
                // A safety check, if through some sorcery the file exists already: 
                while (Files.exists(dataFile.getFileSystemLocation())) {
                    datasetService.generateFileSystemName(dataFile);
                }
                Files.copy(Paths.get(tabFile.getAbsolutePath()), dataFile.getFileSystemLocation(), StandardCopyOption.REPLACE_EXISTING);

                // And we want to save the original of the ingested file: 
                try {
                    saveIngestedOriginal(dataFile, new FileInputStream(new File(tempFileLocation)));
                } catch (IOException iox) {
                    Logger.getLogger(DatasetPage.class.getName()).log(Level.INFO, "Failed to save the ingested original! " + iox.getMessage());
                }

                dataFile.setDataTable(tabDataIngest.getDataTable());
                tabDataIngest.getDataTable().setDataFile(dataFile);
                
                try {
                    produceSummaryStatistics(dataFile);
                } catch (IOException sumStatEx) {
                    throw new IOException ("Ingest: failed to calculate summary statistics. "+sumStatEx.getMessage());
                }
                
                ingestSuccessful = true;
            }
        }
        return ingestSuccessful;
    }

    private String determineMimeType(DataFile dataFile) {
        /*
         * Another placeholder method - we'll be using a new version 
         * of the file type recognition utility instead. 
         * -- L.A. 4.0 alpha 1
         */
        String fileName = dataFile.getName();

        if (fileName == null) {
            return null;
        }

        if (fileName.endsWith(".dta")) {
            return "application/x-stata";
        } else if (fileName.endsWith(".RData")) {
            return "application/x-rlang-transport";
        } else if (fileName.endsWith(".csv")) {
            return "text/csv";
        }

        return null;
    }
    
    public boolean fileMetadataExtractable(DataFile dataFile) {
        /* 
         * Eventually we'll be consulting the Ingest Service Provider Registry
         * to see if there is a plugin for this type of file;
         * for now - just a hardcoded list of filename extensions:
         *  -- L.A. 4.0alpha1
         */
        if (dataFile.getName() != null && dataFile.getName().endsWith(".fits")) {
            return true;
        }
        return false;
    }

    public boolean ingestableAsTabular(DataFile dataFile) {
        /* 
         * Eventually we'll be using some complex technology of identifying 
         * potentially ingestable file formats here, similar to what we had in 
         * v.3.*; for now - just a hardcoded list of filename extensions:
         *  -- L.A. 4.0alpha1
         */
        if (dataFile.getName() != null && dataFile.getName().endsWith(".dta")) {
            return true;
        } else if (dataFile.getName() != null && dataFile.getName().endsWith(".RData")) {
            return true;
        } else if (dataFile.getName() != null && dataFile.getName().endsWith(".csv")) {
            return true;
        }

        return false;
    }
    
    public boolean extractIndexableMetadata(String tempFileLocation, DataFile dataFile) throws IOException {
        boolean ingestSuccessful = false;

        FileInputStream tempFileInputStream = null; 
        
        try {
            tempFileInputStream = new FileInputStream(new File(tempFileLocation));
        } catch (FileNotFoundException notfoundEx) {
            throw new IOException("Could not open temp file "+tempFileLocation);
        }
        
        // Locate metadata extraction plugin for the file format by looking
        // it up with the Ingest Service Provider Registry:
        //FileMetadataExtractor extractorPlugin = IngestSP.getMetadataExtractorByMIMEType(dfile.getContentType());
        FileMetadataExtractor extractorPlugin = new FITSFileMetadataExtractor();

        Map<String, Set<String>> extractedMetadata = extractorPlugin.ingest(new BufferedInputStream(tempFileInputStream));

        // Store the fields and values we've gathered for safe-keeping:
        // from 3.6:
        // attempt to ingest the extracted metadata into the database; 
        // TODO: this should throw an exception if anything goes wrong.
        FileMetadata fileMetadata = dataFile.getFileMetadata();

        if (extractedMetadata != null) {
            //ingestFileLevelMetadata(fileLevelMetadata, file.getFileMetadata(), fileIngester.getFormatName());
            ingestFileLevelMetadata(extractedMetadata, dataFile, fileMetadata, extractorPlugin.getFormatName());

        }

        ingestSuccessful = true;

        return ingestSuccessful;
    }

    private void ingestFileLevelMetadata(Map<String, Set<String>> fileLevelMetadata, DataFile dataFile, FileMetadata fileMetadata, String fileFormatName) {
        // First, add the "metadata summary" generated by the file reader/ingester
        // to the fileMetadata object, as the "description":

        Set<String> metadataSummarySet = fileLevelMetadata.get(METADATA_SUMMARY);
        if (metadataSummarySet != null && metadataSummarySet.size() > 0) {
            String metadataSummary = "";
            for (String s : metadataSummarySet) {
                metadataSummary = metadataSummary.concat(s);
            }
            if (!metadataSummary.equals("")) {
                // The AddFiles page allows a user to enter file description 
                // on ingest. We don't want to overwrite whatever they may 
                // have entered. Rather, we'll append our metadata summary 
                // to the existing value. 
                String userEnteredFileDescription = fileMetadata.getDescription();
                if (userEnteredFileDescription != null
                        && !(userEnteredFileDescription.equals(""))) {

                    metadataSummary
                            = userEnteredFileDescription.concat("\n" + metadataSummary);
                }
                fileMetadata.setDescription(metadataSummary);
            }

            fileLevelMetadata.remove(METADATA_SUMMARY);
        }

        // And now we can go through the remaining key/value pairs in the 
        // metadata maps and process the metadata elements found in the 
        // file: 
        for (String mKey : fileLevelMetadata.keySet()) {

            Set<String> mValues = fileLevelMetadata.get(mKey);

            Logger.getLogger(DatasetPage.class.getName()).log(Level.FINE, "Looking up file meta field " + mKey + ", file format " + fileFormatName);
            FileMetadataField fileMetaField = fieldService.findFileMetadataFieldByNameAndFormat(mKey, fileFormatName);

            if (fileMetaField == null) {
                //fileMetaField = studyFieldService.createFileMetadataField(mKey, fileFormatName); 
                fileMetaField = new FileMetadataField();

                if (fileMetaField == null) {
                    Logger.getLogger(DatasetPage.class.getName()).log(Level.WARNING, "Failed to create a new File Metadata Field; skipping.");
                    continue;
                }

                fileMetaField.setName(mKey);
                fileMetaField.setFileFormatName(fileFormatName);
                // TODO: provide meaningful descriptions and labels:
                fileMetaField.setDescription(mKey);
                fileMetaField.setTitle(mKey);

                try {
                    fieldService.saveFileMetadataField(fileMetaField);
                } catch (Exception ex) {
                    Logger.getLogger(DatasetPage.class.getName()).log(Level.WARNING, "Failed to save new file metadata field (" + mKey + "); skipping values.");
                    continue;
                }

                Logger.getLogger(DatasetPage.class.getName()).log(Level.FINE, "Created file meta field " + mKey);
            }

            String fieldValueText = null;

            if (mValues != null) {
                for (String mValue : mValues) {
                    if (mValue != null) {
                        if (fieldValueText == null) {
                            fieldValueText = mValue;
                        } else {
                            fieldValueText = fieldValueText.concat(" ".concat(mValue));
                        }
                    }
                }
            }

            FileMetadataFieldValue fileMetaFieldValue = null;

            if (!"".equals(fieldValueText)) {
                Logger.getLogger(DatasetPage.class.getName()).log(Level.FINE, "Attempting to create a file meta value for study file " + dataFile.getId() + ", value " + fieldValueText);
                if (dataFile != null) {
                    fileMetaFieldValue
                            = new FileMetadataFieldValue(fileMetaField, dataFile, fieldValueText);
                }
            }
            if (fileMetaFieldValue == null) {
                Logger.getLogger(DatasetPage.class.getName()).log(Level.WARNING, "Failed to create a new File Metadata Field value; skipping");
                continue;
            } else {
                if (dataFile.getFileMetadataFieldValues() == null) {
                    dataFile.setFileMetadataFieldValues(new ArrayList<FileMetadataFieldValue>());
                }
                dataFile.getFileMetadataFieldValues().add(fileMetaFieldValue);
            }
        }
    }
    
    public void performPostProcessingTasks(DataFile dataFile) {
        /*
         * At this point (4.0 alpha 1) the only ingest "post-processing task" performed 
         * is pre-generation of thumbnails in some popular sizes for image files. 
         * -- L.A. 
         */
        if (dataFile != null && dataFile.isImage()) {
            ImageThumbConverter.generateImageThumb(dataFile.getFileSystemLocation().toString(), ImageThumbConverter.DEFAULT_THUMBNAIL_SIZE);
            ImageThumbConverter.generateImageThumb(dataFile.getFileSystemLocation().toString(), ImageThumbConverter.DEFAULT_PREVIEW_SIZE);
        }
    }
    
    private void saveIngestedOriginal(DataFile dataFile, InputStream originalFileStream) throws IOException {
        String ingestedFileName = dataFile.getFileSystemName();

        if (ingestedFileName != null && !ingestedFileName.equals("")) {
            Path savedOriginalPath = Paths.get(dataFile.getOwner().getFileSystemDirectory().toString(), "_" + ingestedFileName);
            Files.copy(originalFileStream, savedOriginalPath);
        } else {
            throw new IOException("Ingested tabular data file: no filesystem name.");
        }
    }
    
    private TabularDataFileReader getTabDataReaderByFileNameExtension(String fileName) {
        /* 
         * Temporary local implementation; 
         * eventually, the Ingest Service Provider Registry will be providing
         * the ingest plugin lookup functionality. -- L.A. 4.0 alpha 1.
         */

        if (fileName == null) {
            return null;
        }

        TabularDataFileReader ingestPlugin = null;

        if (fileName.endsWith(".dta")) {
            ingestPlugin = new DTAFileReader(new DTAFileReaderSpi());
        } else if (fileName.endsWith(".RData")) {
            ingestPlugin = new RDATAFileReader(new RDATAFileReaderSpi());
        } else if (fileName.endsWith(".csv")) {
            ingestPlugin = new CSVFileReader(new CSVFileReaderSpi());
        }

        return ingestPlugin;
    }
    

    private Set<Integer> selectContinuousVariableColumns(DataFile dataFile) {
        Set<Integer> contVarFields = new LinkedHashSet<Integer>();

        for (int i = 0; i < dataFile.getDataTable().getVarQuantity(); i++) {
            if ("continuous".equals(dataFile.getDataTable().getDataVariables().get(i).getVariableIntervalType().getName())) {
                contVarFields.add(i);
            }
        }

        return contVarFields;
    }
    
    private Double[][] subsetContinuousVectors(DataFile dataFile) throws IOException {
        Set<Integer> contVarFields = selectContinuousVariableColumns(dataFile);
        
        FileInputStream tabFileStream = null; 
        
        try {
            tabFileStream = new FileInputStream(dataFile.getFileSystemLocation().toFile());
        } catch (FileNotFoundException notfoundEx) {
            throw new IOException ("Could not open file "+dataFile.getFileSystemLocation());
        }
        
        Double[][] variableVectors = null; 
        
        variableVectors = TabularSubsetGenerator.subsetDoubleVectors(tabFileStream, contVarFields, dataFile.getDataTable().getCaseQuantity().intValue());
        
        return variableVectors;
    }
    
    private void calculateContinuousSummaryStatistics(DataFile dataFile, Double[][] dataVectors) throws IOException {
        int k = 0;
        for (int i = 0; i < dataFile.getDataTable().getVarQuantity(); i++) {
            if ("continuous".equals(dataFile.getDataTable().getDataVariables().get(i).getVariableIntervalType().getName())) {
                double[] sumStats = SumStatCalculator.calculateSummaryStatistics(dataVectors[k++]);

                assignContinuousSummaryStatistics(dataFile.getDataTable().getDataVariables().get(i), sumStats);
            }
        }
    }
    
    private void assignContinuousSummaryStatistics(DataVariable variable, double[] sumStats) throws IOException {
        if (sumStats == null || sumStats.length != variableService.summaryStatisticTypes.length) {
            throw new IOException ("Wrong number of summary statistics types calculated! ("+sumStats.length+")");
        }
        
        for (int j = 0; j < variableService.summaryStatisticTypes.length; j++) {
            SummaryStatistic ss = new SummaryStatistic();
            ss.setType(variableService.findSummaryStatisticTypeByName(variableService.summaryStatisticTypes[j]));
            if (!"mode".equals(variableService.summaryStatisticTypes[j])) {
                ss.setValue((new Double(sumStats[j])).toString());
            } else {
                ss.setValue(".");
            }
            ss.setDataVariable(variable);
            variable.getSummaryStatistics().add(ss);
        }

    }
}
