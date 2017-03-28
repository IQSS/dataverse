/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.datasetutility;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetPage;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.DataverseLinkingServiceBean;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.UserNotificationServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.ingest.IngestServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.json;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.json.JsonObjectBuilder;
import org.apache.commons.lang.StringUtils;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;

/**
 *
 * @author rmp553
 */
@ViewScoped
@Named("FileUploadTestPage")
public class FileUploadTestPage implements java.io.Serializable {
    
    private static final Logger logger = Logger.getLogger(DatasetPage.class.getCanonicalName());


    @EJB
    IngestServiceBean ingestService;
    @Inject 
    DataverseSession session;    
    @EJB
    DatasetServiceBean datasetService;
    @EJB
    DatasetVersionServiceBean datasetVersionService;
    @EJB
    DataFileServiceBean datafileService;
    @EJB
    UserNotificationServiceBean userNotificationService;
    @EJB
    SettingsServiceBean settingsService;
    @EJB
    AuthenticationServiceBean authService;
    @EJB
    SystemConfig systemConfig;
    @EJB
    DataverseLinkingServiceBean dvLinkingService;
    @Inject
    DataverseRequestServiceBean dvRequestService;
    @EJB
    PermissionServiceBean permissionService;
    @EJB
    EjbDataverseEngine commandEngine;
    
    
    private AddReplaceFileHelper addReplaceFileHelper;
    private boolean replaceOperation = false;
    private Long datasetId;
    private Dataset dataset;
    private DatasetVersion datasetVersion;
    private DataFile fileToReplace;
    private List<DataFile> newlyAddedFiles;
    private boolean phase1Success;
    
    public String init() {
        
        Map<String, String> params =FacesContext.getCurrentInstance().
                getExternalContext().getRequestParameterMap();
        
        
        msgt("params: " + params.toString());
        
        if (params.containsKey("ds_id")){
            String ds_id = params.get("ds_id");
            if ((!ds_id.isEmpty()) && (StringUtils.isNumeric(ds_id))){
                datasetId = Long.parseLong(ds_id);
                dataset = datasetService.find(datasetId);
                datasetVersion = dataset.getLatestVersion();
                checkRetrievalTest();
            }
        }
        
        if (params.containsKey("fid")){
            String fid = params.get("fid");
            if ((!fid.isEmpty()) && (StringUtils.isNumeric(fid))){
                fileToReplace = datafileService.find(Long.parseLong(fid));
            }
        }
        
        if (fileToReplace != null){       
            replaceOperation = true;
        }else{
            replaceOperation = false;
        }
        
        return null;
    }
    
    
    private void checkRetrievalTest(){
        msgt("checkRetrievalTest");
        if (dataset == null){
            msg("Dataset is null!!!");
        }
        boolean prettyPrint = true;

       List<HashMap> hashList = datasetVersionService.getBasicDatasetVersionInfo(dataset);
        if (hashList==null){
            msg("hashList is null!!!");
            return;
            
        }
                
       msgt("hashed! : " + hashList.size());
       
       
        msg(hashList.toString());
       
        GsonBuilder builder;
        if (prettyPrint){  // Add pretty printing
            builder = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setPrettyPrinting();
        }else{
            builder = new GsonBuilder().excludeFieldsWithoutExposeAnnotation();                        
        }
        
        Gson gson = builder.create();

        // ----------------------------------
        // serialize this object + add the id
        // ----------------------------------
        JsonElement jsonObj = gson.toJsonTree(hashList);
        msgt("Really?");
        msg(jsonObj.toString());
        
    }
    
    
    /**
     * Get new timestamp
     * 
     * @return 
     */
    public String getNewTimeTest(){
        msgt("getNewTimeTest");
        String timeStr = new SimpleDateFormat("yyyy:MM:dd:HH:mm:ss").format(new Date());
        msg("timeStr: " + timeStr);
        return timeStr;
    }

     /**
     * Get new timestamp
     * 
     * @return 
     */
    public String getNewTimeTestJSON(){
        msgt("getNewTimeTest");
        String timeStr = new SimpleDateFormat("yyyy:MM:dd:HH:mm:ss").format(new Date());
        msg("timeStr: " + timeStr);
        
        return "{\"timeStr\": \"" + timeStr + "\", \"dog\": \"blue\"}";
        //return timeStr;
    }
    
    
    public List<FileMetadata> getDatasetFileMetadatas(){

        dataset = datasetService.find(datasetId);
        if (dataset == null){            
            return null;
        }
        
        return dataset.getLatestVersion().getFileMetadatasSorted();
        /*
        if ((addReplaceFileHelper != null)&&(!addReplaceFileHelper.hasError())){
            return addReplaceFileHelper.getNewlyAddedFileMetadatas();
        }else{
        }
        */
    }
    
    public Long getMaxFileUploadSizeInBytes(){
        
        return systemConfig.getMaxFileUploadSize();
    }
            
    public String yesYes(){
        
        return "yes yes";
    }
    
    private void msg(String s){
        System.out.println(s);
    }
    
    public boolean showUploadComponent(){
        
        if ((datasetVersion==null)||(datasetVersion.getId()==null)){
            return false;
        }
        return true;
    }
    
    private void msgt(String s){
        msg("-------------------------------");
        msg(s);
        msg("-------------------------------");
    }
    
    public Dataset getDataset(){
        return dataset;
    }

    public DatasetVersion getDatasetVersion(){
        return datasetVersion;
    }

    public void setDataset(Dataset ds){
        dataset = ds;
    }

    public DataFile getFileToReplace(){
        return fileToReplace;
    }

    public void setFileToReplace(DataFile df){
        fileToReplace = df;
    }


    public String getReplacementFileMetadataAsJSON(){
        
        FileMetadata fm = getReplacementFileMetadata();
        if (fm==null){
            msgt("Something is wrong here!!! Write error response 1");
            return null;
        }
        
        JsonObjectBuilder jobj = json(fm.getDataFile(), fm);
        
        if (jobj == null){
            msgt("Something is wrong here!!! Write error response 2");
            return null;
        }
        
        return jobj.build().toString();
    }
    
    /**
     * Call if first step of replace has worked
     * 
     * Return the pending FileMetadata -- e.g. it can still be edited
     */
    public FileMetadata getReplacementFileMetadata(){
        if (addReplaceFileHelper == null){
            return null;
        }
        if (!didPhase1Succeed()){
            return null;
        }
                
        List<FileMetadata> fms = addReplaceFileHelper.getNewFileMetadatasBeforeSave();
        
        if (fms==null){
            throw new NullPointerException("Replacement file couldn not be found.");
        }
        
        if (fms.size() > 0){
            return fms.get(0);
        }
        
        return null;
    }
    
    public void handleFileUpload(FileUploadEvent event) {
        
        
        String foo = (String) event.getComponent().getAttributes().get("isReplaceOperation"); // bar
        msgt("Foo: " + foo);
        //FacesMessage message = new FacesMessage("Succesful", event.getFile().getFileName() + " is uploaded.");
        //FacesContext.getCurrentInstance().addMessage(null, message);
            
        
        UploadedFile uFile = event.getFile();

        msg("getFileName: " + uFile.getFileName());
        msg("getContentType: " + uFile.getContentType());
        
        addReplaceFile(uFile);
        //msg("file name: " + event.getFileName());
    //        dFileList = ingestService.createDataFiles(workingVersion, uFile.getInputstream(), uFile.getFileName(), uFile.getContentType());
      
    }
    
    /**
     * Check whether the addReplaceFileHelper encountered an error
     * 
     * @return 
     */
    public boolean hasAddReplaceError(){
        
        if (addReplaceFileHelper == null){
            msgt("hasAddReplaceError: addReplaceFileHelper is null ");
            return false;
        }
        return addReplaceFileHelper.hasError();
    }
    
    public List<String> getErrorMessageList(){
        
        List<String> errorMessages = addReplaceFileHelper.getErrorMessages();
        msgt("errorMessages: " + errorMessages);
        return errorMessages;
    }

    public String getErrorMessageString(){
 
        if (addReplaceFileHelper==null){
            throw new NullPointerException("addReplaceFileHelper cannot be null.  First check \"hasAddReplaceError()\"");
        }
        
        if (!hasAddReplaceError()){
            return null;
        }
        return addReplaceFileHelper.getErrorMessagesAsString("<br />");
    }

    public boolean runPhase2FileSave(){
        
        msgt("runPhase2FileSave");
        
        if (addReplaceFileHelper == null){
            throw new NullPointerException("addReplaceHelper cannot be null!");
        }
        
        msg("runPhase2FileSave 1");
        if (!didPhase1Succeed()){
            msgt("ERROR: runFileSave. Phase1 did not succeed!");
            return false;
        }
        
        UIViewRoot viewRoot = FacesContext.getCurrentInstance().getViewRoot();
        UIComponent component = viewRoot.findComponent("newFileForm:idNewDescription");
        String newDescription = (String)component.getAttributes().get("value");
        if (newDescription==null){
            newDescription = "";
        }
        
        component = viewRoot.findComponent("newFileForm:idNewLabel");
        String newLabel = (String)component.getAttributes().get("value");
        if (newLabel==null){
            newLabel = "";
        }
        
        component = viewRoot.findComponent("newFileForm:idNewRestricted");
        Boolean newRestricted = (Boolean)component.getAttributes().get("value");
        if (newRestricted==null){
            newRestricted = false;
        }
        
        
        addReplaceFileHelper.updateLabelDescriptionRestrictedFromUI(newLabel, newDescription, newRestricted);
        
        msg("runPhase2FileSave 2");
        addReplaceFileHelper.runReplaceFromUI_Phase2();
                
        if (addReplaceFileHelper.hasError()){
            msg("runPhase2FileSave 2a");
            msgt("save error");
            msg(addReplaceFileHelper.getErrorMessagesAsString("\n"));
            return false;
        }else{
             msg("runPhase2FileSave 2b");

            phase1Success = false;
            
            
            fileToReplace = addReplaceFileHelper.getNewlyAddedFiles().get(0);
                    
            //newlyAddedFiles = addReplaceFileHelper.getNewlyAddedFiles();
            msg("Look at that!  You replaced a file!");
            return true;
        }                

    }
    
    public boolean didPhase1Succeed(){
        return phase1Success;
    }
    
    public void addReplaceFile(UploadedFile laFile){
        
        phase1Success = false;
        
        //DataverseRequest dvRequest2 = createDataverseRequest(authUser);
        addReplaceFileHelper = new AddReplaceFileHelper(dvRequestService.getDataverseRequest(),
                                                ingestService,
                                                datasetService,
                                                datafileService,
                                                permissionService,
                                                commandEngine,
                                                systemConfig);
    
        
        InputStream inputStream = null;
        try {
            inputStream = laFile.getInputstream();
        } catch (IOException ex) {
            msgt("file io exception");

            Logger.getLogger(FileUploadTestPage.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        
        
        if (this.replaceOperation){
            addReplaceFileHelper.runReplaceFromUI_Phase1( fileToReplace.getId(),
                                laFile.getFileName(),
                                laFile.getContentType(),
                                inputStream,
                                null
                               );
        }
        /*else{
            addReplaceFileHelper.runAddFileByDataset(dataset,
                                laFile.getFileName(),
                                laFile.getContentType(),
                                inputStream,
                                null);
        }*/
        
        if (addReplaceFileHelper.hasError()){
            msgt("upload error");
            msg(addReplaceFileHelper.getErrorMessagesAsString("\n"));
        }else{
            phase1Success = true;

            //newlyAddedFiles = addReplaceFileHelper.getNewlyAddedFiles();
            msg("Look at that!  Phase 1 worked");
        }
    }
    
    public List<DataFile> getNewlyAddedFile(){
        
        return newlyAddedFiles;
    }
    
} // end class FileUploadTestPage
