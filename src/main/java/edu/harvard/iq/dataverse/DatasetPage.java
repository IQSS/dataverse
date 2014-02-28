/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.DatasetVersion.VersionState;
import edu.harvard.iq.dataverse.ingest.tabulardata.TabularDataFileReader;
import edu.harvard.iq.dataverse.ingest.tabulardata.TabularDataIngest;
import edu.harvard.iq.dataverse.ingest.tabulardata.TabularDataIngest;
import edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.dta.DTAFileReader;
import edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.dta.DTAFileReaderSpi;
import edu.harvard.iq.dataverse.ingest.metadataextraction.FileMetadataExtractor;
import edu.harvard.iq.dataverse.ingest.metadataextraction.impl.plugins.fits.FITSFileMetadataExtractor;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlDataTable;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;

/**
 *
 * @author gdurand
 */
@ViewScoped
@Named("DatasetPage")
public class DatasetPage implements java.io.Serializable {

    public enum EditMode {CREATE, INFO, FILE, METADATA};
    
    @EJB
    DatasetServiceBean datasetService;
    @EJB
    DataFileServiceBean datafileService;
    @EJB
    DataverseServiceBean dataverseService;    
    @EJB
    TemplateServiceBean templateService;
    @EJB
    DatasetFieldServiceBean fieldService;

    private Dataset dataset = new Dataset();
    private EditMode editMode;
    private Long ownerId;
    private int selectedTabIndex;
    private Map<UploadedFile,DataFile> newFiles = new HashMap();
    private Map<MetadataBlock, Object> metadataBlockValues = new HashMap();
    private DatasetVersion editVersion = new DatasetVersion();   
    private List<DatasetField> citationFields = new ArrayList();
    private List<DatasetField> otherMetadataFields = new ArrayList();
    private List<DatasetFieldValue> citationValues = new ArrayList();
    private DatasetVersionUI datasetVersionUI = new DatasetVersionUI();
    private boolean authorAsOrg = false;

    public boolean isAuthorAsOrg() {
        return authorAsOrg;
    }

    public void setAuthorAsOrg(boolean authorAsOrg) {
        this.authorAsOrg = authorAsOrg;
    }
    
    // TODO: this constant should be provided by the Ingest Service Provder Registry;
    private static final String METADATA_SUMMARY = "FILE_METADATA_SUMMARY_INFO";

    public List<DatasetFieldValue> getCitationValues() {
        return citationValues;
    }

    public void setCitationValues(List<DatasetFieldValue> citationValues) {
        this.citationValues = citationValues;
    }
    private List<DatasetFieldValue> otherMetadataValues = new ArrayList();    
    private List<DatasetFieldValue> editValues = new ArrayList();
    
    
    public List<DatasetFieldValue> getOtherMetadataValues() {
        return otherMetadataValues;
    }
    /*
    public Map<MetadataBlock, Object> getMetadataBlockValues() {
        return metadataBlockValues;
    }*/
    
    public List<Map.Entry<MetadataBlock, Object>> getMetadataBlockValues() {
    Set<Map.Entry<MetadataBlock, Object>> metadataSet = 
                     metadataBlockValues.entrySet();
    return new ArrayList<>(metadataSet);
}


    public void setOtherMetadataValues(List<DatasetFieldValue> otherMetadataValues) {
        this.otherMetadataValues = otherMetadataValues;
    }
    
    public Dataset getDataset() {
        return dataset;
    }
    
    public List<DatasetFieldValue> getEditValues() {
        return editValues;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    public EditMode getEditMode() {
        return editMode;
    }

    public void setEditMode(EditMode editMode) {
        this.editMode = editMode;
    }

    public DatasetVersion getEditVersion() {
        return editVersion;
    }

    public void setEditVersion(DatasetVersion editVersion) {
        this.editVersion = editVersion;
    }
    
    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public int getSelectedTabIndex() {
        return selectedTabIndex;
    }

    public void setSelectedTabIndex(int selectedTabIndex) {
        this.selectedTabIndex = selectedTabIndex;
    }
    
    private boolean initOrgAsAuthorFlag() {
        for (DatasetAuthor author : datasetVersionUI.getDatasetAuthors()) {
            if (author.getLastName() != null && author.getFirstName() != null) {
                if (!author.getLastName().isEmpty() && author.getFirstName().isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public void init() {
        if (dataset.getId() != null) { // view mode for a dataset           
            dataset = datasetService.find(dataset.getId());
            editVersion = dataset.getLatestVersion();
            editVersion.setDatasetFieldValues(editVersion.initDatasetFieldValues()); 
            editValues = editVersion.getDatasetFieldValues();
            citationValues = extractValues(editValues, true);
            otherMetadataValues = extractValues(editValues, false);
            getMetadataValueBlocks(editValues);
            ownerId = dataset.getOwner().getId();
            datasetVersionUI = new DatasetVersionUI(editVersion); 
            authorAsOrg = initOrgAsAuthorFlag();
        } else if (ownerId != null) { // create mode for a new child dataset
            editMode = EditMode.CREATE;
            dataset.setOwner(dataverseService.find(ownerId));
            dataset.setVersions(new ArrayList());
            editVersion.setDataset(dataset);           
            editVersion.setFileMetadatas(new ArrayList());
            editVersion.setDatasetFieldValues(null);            
            editVersion.setVersionState(VersionState.DRAFT);
            editVersion.setDatasetFieldValues(editVersion.initDatasetFieldValues()); 
            editVersion.setVersionNumber(new Long(1));  
            editValues = editVersion.getDatasetFieldValues();
            citationValues = extractValues(editValues, true);
            otherMetadataValues = extractValues(editValues, false);
            getMetadataValueBlocks(editValues);
            datasetVersionUI = new DatasetVersionUI(editVersion);  
            dataset.getVersions().add(editVersion);
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,"Add New Dataset", " - Add your dataset metadata and files."));
        } else {
            throw new RuntimeException("On Dataset page without id or ownerid."); // improve error handling
        }        
        setCitationFields(dataverseService.findCitationDatasetFieldsByDataverseId(ownerId));
        setOtherMetadataFields(dataverseService.findOtherMetadataDatasetFieldsByDataverseId(ownerId));       
    }
    
    public void edit(EditMode editMode) {

        this.editMode = editMode;
        if (editMode == EditMode.INFO) {
            editVersion = dataset.getEditVersion();
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,"Edit Dataset Info", " - Edit your dataset info."));
        } else if (editMode == EditMode.FILE) {
            editVersion = dataset.getEditVersion();
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,"Edit Dataset Files", " - Edit your dataset files."));
        } else if (editMode == EditMode.METADATA) {
            editVersion = dataset.getEditVersion();
            editVersion.setDatasetFieldValues(editVersion.initDatasetFieldValues()); 
            editVersion.setVersionNumber(new Long(1));  
            editValues = editVersion.getDatasetFieldValues();
            citationValues = extractValues(editValues, true);
            otherMetadataValues = extractValues(editValues, false);
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,"Edit Dataset Metadata", " - Edit your dataset metadata."));
        }
    }
    
    public void addRecord(String recordType){
        
    if (recordType.equals("AUTHOR")) {
            DatasetAuthor addAuthor = new DatasetAuthor();
            addAuthor.setDatasetVersion(editVersion);
            DatasetFieldValue author = new DatasetFieldValue();
            author.setDatasetVersion(editVersion);
            author.setDatasetField(fieldService.findByName(DatasetFieldConstant.author));
            DatasetFieldValue authorName = new DatasetFieldValue();
            authorName.setDatasetField(fieldService.findByName(DatasetFieldConstant.authorName));
            authorName.setDatasetVersion(editVersion);
            authorName.setStrValue("");
            author.setChildDatasetFieldValues(new ArrayList());
            author.getChildDatasetFieldValues().add(authorName);
            addAuthor.setName(authorName);
            DatasetFieldValue firstName = new DatasetFieldValue();
            firstName.setDatasetField(fieldService.findByName(DatasetFieldConstant.authorFirstName));
            firstName.setDatasetVersion(editVersion);
            firstName.setStrValue("");
            author.getChildDatasetFieldValues().add(firstName);
            addAuthor.setFirstName(firstName);
                        DatasetFieldValue lastName = new DatasetFieldValue();
            lastName.setDatasetField(fieldService.findByName(DatasetFieldConstant.authorLastName));
            lastName.setDatasetVersion(editVersion);
            lastName.setStrValue("");
            author.getChildDatasetFieldValues().add(lastName);
            addAuthor.setLastName(lastName);
            DatasetFieldValue authorAffiliation = new DatasetFieldValue();
            authorAffiliation.setDatasetField(fieldService.findByName(DatasetFieldConstant.authorAffiliation));
            authorAffiliation.setDatasetVersion(editVersion);
            authorAffiliation.setStrValue("");
            author.getChildDatasetFieldValues().add(authorAffiliation);
            addAuthor.setAffiliation(authorAffiliation);
            editVersion.getDatasetFieldValues().add(author);
            editVersion.getDatasetFieldValues().add(authorName);
            editVersion.getDatasetFieldValues().add(firstName);
            editVersion.getDatasetFieldValues().add(lastName);
            editVersion.getDatasetFieldValues().add(authorAffiliation);
            datasetVersionUI.getDatasetAuthors().add(addAuthor);           
        } else if (recordType.equals("KEYWORD")){
            DatasetKeyword addKeyword = new DatasetKeyword();
            addKeyword.setDatasetVersion(editVersion);
            DatasetFieldValue keyword = new DatasetFieldValue();
            keyword.setDatasetVersion(editVersion);
            keyword.setDatasetField(fieldService.findByName(DatasetFieldConstant.keyword));
            DatasetFieldValue keywordValue = new DatasetFieldValue();
            keywordValue.setDatasetField(fieldService.findByName(DatasetFieldConstant.keywordValue));
            keywordValue.setDatasetVersion(editVersion);
            keywordValue.setStrValue("");
            keyword.setChildDatasetFieldValues(new ArrayList());
            keyword.getChildDatasetFieldValues().add(keywordValue);
            addKeyword.setValue(keywordValue);
            editVersion.getDatasetFieldValues().add(keyword);
            editVersion.getDatasetFieldValues().add(keywordValue);
            datasetVersionUI.getDatasetKeywords().add(addKeyword);             
        }          else if (recordType.equals("TOPIC")){
            DatasetTopicClass addTopic = new DatasetTopicClass();
            addTopic.setDatasetVersion(editVersion);
            DatasetFieldValue topic = new DatasetFieldValue();
            topic.setDatasetVersion(editVersion);
            topic.setDatasetField(fieldService.findByName(DatasetFieldConstant.topicClassification));
            DatasetFieldValue topicValue = new DatasetFieldValue();
            topicValue.setDatasetField(fieldService.findByName(DatasetFieldConstant.topicClassValue));
            topicValue.setDatasetVersion(editVersion);
            topicValue.setStrValue("");
            DatasetFieldValue topicCV = new DatasetFieldValue();
            topicCV.setDatasetField(fieldService.findByName(DatasetFieldConstant.topicClassVocab));
            topicCV.setDatasetVersion(editVersion);
            topicCV.setStrValue("");
            topic.setChildDatasetFieldValues(new ArrayList());
            topic.getChildDatasetFieldValues().add(topicValue);
            topic.getChildDatasetFieldValues().add(topicCV);
            DatasetFieldValue topicURI = new DatasetFieldValue();
            topicURI.setDatasetField(fieldService.findByName(DatasetFieldConstant.topicClassVocabURI));
            topicURI.setDatasetVersion(editVersion);
            topicURI.setStrValue("");
            topic.setChildDatasetFieldValues(new ArrayList());
            topic.getChildDatasetFieldValues().add(topicValue);
            topic.getChildDatasetFieldValues().add(topicCV);
            topic.getChildDatasetFieldValues().add(topicURI);
            addTopic.setValue(topicValue);
            addTopic.setVocab(topicCV);
            addTopic.setVocabURI(topicURI);
            editVersion.getDatasetFieldValues().add(topic);
            editVersion.getDatasetFieldValues().add(topicValue);
            editVersion.getDatasetFieldValues().add(topicCV);
            editVersion.getDatasetFieldValues().add(topicURI);
            datasetVersionUI.getDatasetTopicClasses().add(addTopic);             
        }       
    }
    public void deleteRecord(String recordType){
        
    }
       
    public void save() {
        
        dataset.setOwner(dataverseService.find(ownerId));
        //TODO get real application-wide protocol/authority
        dataset.setProtocol("doi");
        dataset.setAuthority("10.5072/FK2");
        dataset.setIdentifier("5555");

        if (!(dataset.getVersions().get(0).getFileMetadatas() == null) && !dataset.getVersions().get(0).getFileMetadatas().isEmpty()) {
            int fmdIndex = 0;
            for (FileMetadata fmd : dataset.getVersions().get(0).getFileMetadatas()) {
                for (FileMetadata fmdTest : editVersion.getFileMetadatas()) {
                    if (fmd.equals(fmdTest)) {
                        dataset.getVersions().get(0).getFileMetadatas().get(fmdIndex).setDataFile(fmdTest.getDataFile());
                    }
                }
                fmdIndex++;
            }
        }        

        // save any new files
        for (UploadedFile uFile : newFiles.keySet()) {
            DataFile dFile = newFiles.get(uFile);
            try {
                boolean ingestedAsTabular = false;
                boolean metadataExtracted = false; 
                
                /* Make sure the dataset directory exists: */
                if (!Files.exists(dataset.getFileSystemDirectory())) {
                    /* Note that "createDirectories()" must be used - not 
                     * "createDirectory()", to make sure all the parent 
                     * directories that may not yet exist are created as well. 
                     */
                    Files.createDirectories(dataset.getFileSystemDirectory());
                }

                if (ingestableAsTabular(dFile)) {
                    
                    try {          
                        ingestedAsTabular = ingestAsTabular(uFile, dFile);
                        dFile.setContentType("text/tab-separated-values");
                    } catch (IOException iex) {
                        Logger.getLogger(DatasetPage.class.getName()).log(Level.SEVERE, null, iex);
                        ingestedAsTabular = false; 
                    }
                } else if (fileMetadataExtractable(dFile)) {
                    
                    try {
                        metadataExtracted = extractIndexableMetadata(uFile, dFile);
                        dFile.setContentType("application/fits");
                    } catch(IOException mex) {
                        Logger.getLogger(DatasetPage.class.getName()).log(Level.SEVERE, "Caught exception trying to extract indexable metadata from file "+dFile.getName(), mex);
                    }
                    if (metadataExtracted) {
                        Logger.getLogger(DatasetPage.class.getName()).log(Level.INFO, "Successfully extracted indexable metadata from file " + dFile.getName());
                    } else {
                        Logger.getLogger(DatasetPage.class.getName()).log(Level.INFO, "Failed to extract indexable metadata from file " + dFile.getName());
                    }
                }

                if (!ingestedAsTabular) {
                    Logger.getLogger(DatasetPage.class.getName()).log(Level.INFO, "Will attempt to save the file as: " + dFile.getFileSystemLocation().toString());
                    Files.copy(uFile.getInputstream(), dFile.getFileSystemLocation(), StandardCopyOption.REPLACE_EXISTING);
                }
                
            } catch (IOException ex) {
                Logger.getLogger(DatasetPage.class.getName()).log(Level.SEVERE, null, ex);
                // TODO: 
                // discard the DataFile and disconnect it from the dataset object!
            }

        }
        
        dataset = datasetService.save(dataset);

        newFiles.clear();
        editMode = null;
    }

    public void cancel() {
        // reset values
        dataset = datasetService.find(dataset.getId());
        ownerId = dataset.getOwner().getId();
        newFiles.clear();
        editMode = null;
    }

    public void handleFileUpload(FileUploadEvent event) {
        UploadedFile uFile = event.getFile();
        DataFile dFile = new DataFile(uFile.getFileName(), uFile.getContentType());
        FileMetadata fmd = new FileMetadata();
        dFile.setOwner(dataset);
        fmd.setDataFile(dFile);
        dFile.getFileMetadatas().add(fmd);
        fmd.setLabel(dFile.getName());
        fmd.setCategory(dFile.getContentType());
        fmd.setDescription("add description");
        if (editVersion.getFileMetadatas() == null) {
            editVersion.setFileMetadatas(new ArrayList());
        }
        editVersion.getFileMetadatas().add(fmd);
        fmd.setDatasetVersion(editVersion);
        dataset.getFiles().add( dFile );
        newFiles.put(uFile, dFile);
    }
    
    public void addRow(ActionEvent ae) {       
        //      UIComponent dataTable = ae.getComponent().getParent().getParent().getParent();
        HtmlDataTable dataTable = (HtmlDataTable)ae.getComponent().getParent().getParent();
            DatasetAuthor newElem = new DatasetAuthor();
           // newElem.setMetadata(editVersion.getMetadata());
           // editVersion.getMetadata().getDatasetAuthors().add(dataTable.getRowIndex()+1,newElem);
            //JavascriptContext.addJavascriptCall(getFacesContext(),"initAddAuthorSync();");
                         
    }

                
    public DataModel getDatasetFieldsDataModel() {
        List values = new ArrayList();  
        int i = 0;
        for (DatasetFieldValue dsfv : editVersion.getDatasetFieldValues()){
            DatasetField datasetField = dsfv.getDatasetField();                      
                Object[] row = new Object[4];
                row[0] = datasetField;
                row[1] = getValuesDataModel(dsfv);
                row[2] = new Integer(i);
                row[3] = datasetField;                
                values.add(row);
                i++;
        }         
        return new ListDataModel(values);
    }
    
    private DataModel getValuesDataModel(DatasetFieldValue datasetFieldValue) {
        List values = new ArrayList();
                Object[] row = new Object[2];
                row[0] = datasetFieldValue;
                row[1] = datasetFieldValue.getDatasetField().getDatasetFieldValues(); // used by the remove method
                values.add(row);
        return new ListDataModel(values);
    }
    
    public DatasetVersionUI getDatasetVersionUI(){
        return datasetVersionUI;
    }
    

    public List<DatasetField> getCitationFields() {
        return citationFields;
    }

    public void setCitationFields(List<DatasetField> citationFields) {
        this.citationFields = citationFields;
    }

    public List<DatasetField> getOtherMetadataFields() {
        return otherMetadataFields;
    }

    public void setOtherMetadataFields(List<DatasetField> otherMetadataFields) {
        this.otherMetadataFields = otherMetadataFields;
    }
    
    private List<DatasetFieldValue> extractValues(List<DatasetFieldValue> inList, boolean citation){
        
        List retList = new ArrayList();
        for (DatasetFieldValue dsfv : inList){
            if (citation && dsfv.getDatasetField().getMetadataBlock().isShowOnCreate()) { 
                  retList.add(dsfv);                
            } else if (!citation && !dsfv.getDatasetField().getMetadataBlock().isShowOnCreate()) {
                  retList.add(dsfv);
            }
        }
        return retList;
    }
    
    private void getMetadataValueBlocks(List<DatasetFieldValue> inList){
        metadataBlockValues.clear();
        for(MetadataBlock mdb: dataset.getOwner().getMetadataBlocks()){
            List addList = new ArrayList();
            if(!mdb.isShowOnCreate()){
                for (DatasetFieldValue dsfv: inList){
                     if (dsfv.getDatasetField().getMetadataBlock().equals(mdb)){
                         addList.add(dsfv);
                     }
                }
                metadataBlockValues.put(mdb, addList);
            }
        }
            
     int a = 1;    
    }

    private boolean ingestableAsTabular(DataFile dataFile) {
        /* 
         * Eventually we'll be using some complex technology of identifying 
         * potentially ingestable file formats here, similar to what we had in 
         * v.3.*; for now - just a hardcoded list of filename extensions:
         *  -- L.A. 4.0alpha1
         */
        if (dataFile.getName() != null && dataFile.getName().endsWith(".dta")) {
            return true;
        }
        return false;
    } 
    
    private boolean fileMetadataExtractable(DataFile dataFile) {
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

    private boolean ingestAsTabular(UploadedFile uFile, DataFile dataFile) throws IOException {
        boolean ingestSuccessful = false;

        // Locate ingest plugin for the file format by looking
        // it up with the Ingest Service Provider Registry:
        
        //TabularDataFileReader ingestPlugin = IngestSP.getTabDataReaderByMIMEType(dFile.getContentType());
        TabularDataFileReader ingestPlugin = new DTAFileReader(new DTAFileReaderSpi());

        TabularDataIngest tabDataIngest = ingestPlugin.read(new BufferedInputStream(uFile.getInputstream()), null);

        if (tabDataIngest != null) {
            File tabFile = tabDataIngest.getTabDelimitedFile();

            if (tabDataIngest.getDataTable() != null
                    && tabFile != null
                    && tabFile.exists()) {

                Logger.getLogger(DatasetPage.class.getName()).log(Level.INFO, "Tabular data successfully ingested; DataTable with "
                        + tabDataIngest.getDataTable().getVarQuantity() + " variables produced.");

                Logger.getLogger(DatasetPage.class.getName()).log(Level.INFO, "Tab-delimited file produced: " + tabFile.getAbsolutePath());

                dataFile.setName(dataFile.getName().replaceAll("\\.dta$", ".tab"));
                Files.copy(Paths.get(tabFile.getAbsolutePath()), dataFile.getFileSystemLocation(), StandardCopyOption.REPLACE_EXISTING);
                
                dataFile.setDataTable(tabDataIngest.getDataTable());
                tabDataIngest.getDataTable().setDataFile(dataFile);

                ingestSuccessful = true;
            }
        }
        return ingestSuccessful;
    }
    
    private boolean extractIndexableMetadata(UploadedFile uFile, DataFile dataFile) throws IOException {
        boolean ingestSuccessful = false; 
        
        // Locate metadata extraction plugin for the file format by looking
        // it up with the Ingest Service Provider Registry:
        
        //FileMetadataExtractor extractorPlugin = IngestSP.getMetadataExtractorByMIMEType(dfile.getContentType());
        FileMetadataExtractor extractorPlugin = new FITSFileMetadataExtractor();
        
        Map<String, Set<String>> extractedMetadata = extractorPlugin.ingest(new BufferedInputStream(uFile.getInputstream()));
        
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
    
    private void ingestFileLevelMetadata (Map<String, Set<String>> fileLevelMetadata, DataFile dataFile, FileMetadata fileMetadata, String fileFormatName) {
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
                    
                    metadataSummary = 
                            userEnteredFileDescription.concat("\n"+metadataSummary);
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
            
            
            Logger.getLogger(DatasetPage.class.getName()).log(Level.FINE, "Looking up file meta field "+mKey+", file format "+fileFormatName);
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
                    Logger.getLogger(DatasetPage.class.getName()).log(Level.WARNING, "Failed to save new file metadata field ("+mKey+"); skipping values.");
                    continue; 
                }
                
                Logger.getLogger(DatasetPage.class.getName()).log(Level.FINE, "Created file meta field "+mKey); 
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
                Logger.getLogger(DatasetPage.class.getName()).log(Level.FINE, "Attempting to create a file meta value for study file "+dataFile.getId()+", value "+fieldValueText);
                if (dataFile != null) {
                    fileMetaFieldValue =
                            new FileMetadataFieldValue(fileMetaField, dataFile, fieldValueText);
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
}
