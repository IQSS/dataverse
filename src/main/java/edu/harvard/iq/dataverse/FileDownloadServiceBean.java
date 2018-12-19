package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.datasetutility.WorldMapPermissionHelper;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.CreateGuestbookResponseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.RequestAccessCommand;
import edu.harvard.iq.dataverse.externaltools.ExternalTool;
import edu.harvard.iq.dataverse.externaltools.ExternalToolHandler;
import edu.harvard.iq.dataverse.util.FileUtil;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author skraffmi
 * Handles All File Download processes
 * including Guestbook responses
 */
@Stateless
@Named
public class FileDownloadServiceBean implements java.io.Serializable {

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    
    @EJB
    GuestbookResponseServiceBean guestbookResponseService;
    @EJB
    DatasetServiceBean datasetService;
    @EJB
    DatasetVersionServiceBean datasetVersionService;
    @EJB
    DataFileServiceBean datafileService;
    @EJB
    PermissionServiceBean permissionService;
    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    UserNotificationServiceBean userNotificationService;
    @EJB
    AuthenticationServiceBean authService;
    
    @Inject
    DataverseSession session;
    
    @EJB
    EjbDataverseEngine commandEngine;
    
    @Inject
    DataverseRequestServiceBean dvRequestService;
    
    @Inject WorldMapPermissionHelper worldMapPermissionHelper;
    @Inject FileDownloadHelper fileDownloadHelper;

    private static final Logger logger = Logger.getLogger(FileDownloadServiceBean.class.getCanonicalName());   
    
    public void writeGuestbookAndStartBatchDownload(GuestbookResponse guestbookResponse){ 
        writeGuestbookAndStartBatchDownload(guestbookResponse, false);
    }
    
    public void writeGuestbookAndStartBatchDownload(GuestbookResponse guestbookResponse, Boolean doNotSaveGuestbookRecord){
        if (guestbookResponse == null || guestbookResponse.getSelectedFileIds() == null) {
            return;
        }

        // Let's intercept the case where a multiple download method was called, 
        // with only 1 file on the list. We'll treat it like a single file download 
        // instead:
        String[] fileIds = guestbookResponse.getSelectedFileIds().split(",");
        if (fileIds.length == 1) {
            Long fileId;
            try {
                fileId = Long.parseLong(fileIds[0]);
            } catch (NumberFormatException nfe) {
                logger.warning("A file id passed to the writeGuestbookAndStartBatchDownload method as a string could not be converted back to Long: " + fileIds[0]);
                return;
            }
            // If we need to create a GuestBookResponse record, we have to 
            // look up the DataFile object for this file: 
            if (!doNotSaveGuestbookRecord) {
                DataFile df = datafileService.findCheapAndEasy(Long.parseLong(fileIds[0]));
                guestbookResponse.setDataFile(df);
                writeGuestbookResponseRecord(guestbookResponse);
            }
        
            redirectToDownloadAPI(guestbookResponse.getFileFormat(), fileId, true);
            return;
        }
        
        // OK, this is a real batch (multi-file) download. 
        // Do we need to write GuestbookRecord entries for the files? 
        if (!doNotSaveGuestbookRecord) {

            List<String> list = new ArrayList<>(Arrays.asList(guestbookResponse.getSelectedFileIds().split(",")));

            for (String idAsString : list) {
                DataFile df = datafileService.findCheapAndEasy(new Long(idAsString));
                if (df != null) {
                    guestbookResponse.setDataFile(df);
                    writeGuestbookResponseRecord(guestbookResponse);
                }
            }
        }


        redirectToBatchDownloadAPI(guestbookResponse.getSelectedFileIds(), "original".equals(guestbookResponse.getFileFormat()));
    }
    
    public void writeGuestbookAndStartFileDownload(GuestbookResponse guestbookResponse, FileMetadata fileMetadata, String format) {
        if(!fileMetadata.getDatasetVersion().isDraft()){
            guestbookResponse = guestbookResponseService.modifyDatafileAndFormat(guestbookResponse, fileMetadata, format);
            writeGuestbookResponseRecord(guestbookResponse);
        }
        // Make sure to set the "do not write Guestbook response" flag to TRUE when calling the Access API:
        redirectToDownloadAPI(format, fileMetadata.getDataFile().getId(), true);
        logger.fine("issued file download redirect for filemetadata "+fileMetadata.getId()+", datafile "+fileMetadata.getDataFile().getId());
    }
    
    public void writeGuestbookAndStartFileDownload(GuestbookResponse guestbookResponse) {
        if (guestbookResponse.getDataFile() == null) {
            logger.warning("writeGuestbookAndStartFileDownload(GuestbookResponse) called without the DataFile in the GuestbookResponse.");
            return;
        }
        writeGuestbookResponseRecord(guestbookResponse);
        redirectToDownloadAPI(guestbookResponse.getFileFormat(), guestbookResponse.getDataFile().getId());
        logger.fine("issued file download redirect for datafile "+guestbookResponse.getDataFile().getId());
    }

    public void writeGuestbookResponseRecord(GuestbookResponse guestbookResponse, FileMetadata fileMetadata, String format) {
        if(!fileMetadata.getDatasetVersion().isDraft()){
            guestbookResponse = guestbookResponseService.modifyDatafileAndFormat(guestbookResponse, fileMetadata, format);
            writeGuestbookResponseRecord(guestbookResponse);
        }
    }
    
    public void writeGuestbookResponseRecord(GuestbookResponse guestbookResponse) {
        try {
            CreateGuestbookResponseCommand cmd = new CreateGuestbookResponseCommand(dvRequestService.getDataverseRequest(), guestbookResponse, guestbookResponse.getDataset());
            commandEngine.submit(cmd);
        } catch (CommandException e) {
            //if an error occurs here then download won't happen no need for response recs...
        }
    }
    
    // The "guestBookRecord(s)AlreadyWritten" parameter in the 2 methods 
    // below (redirectToBatchDownloadAPI() and redirectToDownloadAPI(), for the 
    // multiple- and single-file downloads respectively) are passed to the 
    // Download API, where it is treated as a "SKIP writing the GuestbookResponse 
    // record for this download on the API side" flag. In other words, we want 
    // to create and save this record *either* on the UI, or the API side - but 
    // not both. 
    // As of now (Aug. 2018) we always set this flag to true when redirecting the 
    // user to the Access API. That's because we have either just created the 
    // record ourselves, on the UI side; or we have skipped creating one, 
    // because this was a draft file and we don't want to count the download. 
    // But either way, it is NEVER the API side's job to count the download that 
    // was initiated in the GUI. 
    // But note that this may change - there may be some future situations where it will 
    // become necessary again, to pass the job of creating the access record 
    // to the API.
    private void redirectToBatchDownloadAPI(String multiFileString, Boolean guestbookRecordsAlreadyWritten, Boolean downloadOriginal){

        String fileDownloadUrl = "/api/access/datafiles/" + multiFileString;
        if (guestbookRecordsAlreadyWritten && !downloadOriginal){
            fileDownloadUrl += "?gbrecs=true";
        } else if (guestbookRecordsAlreadyWritten && downloadOriginal){
            fileDownloadUrl += "?gbrecs=true&format=original";
        } else if (!guestbookRecordsAlreadyWritten && downloadOriginal){
            fileDownloadUrl += "?format=original";
        }
        
        try {
            FacesContext.getCurrentInstance().getExternalContext().redirect(fileDownloadUrl);
        } catch (IOException ex) {
            logger.info("Failed to issue a redirect to file download url.");
        }

    }

    private void redirectToDownloadAPI(String downloadType, Long fileId, boolean guestBookRecordAlreadyWritten) {
        String fileDownloadUrl = FileUtil.getFileDownloadUrlPath(downloadType, fileId, guestBookRecordAlreadyWritten);
        logger.fine("Redirecting to file download url: " + fileDownloadUrl);
        try {
            FacesContext.getCurrentInstance().getExternalContext().redirect(fileDownloadUrl);
        } catch (IOException ex) {
            logger.info("Failed to issue a redirect to file download url (" + fileDownloadUrl + "): " + ex);
        }
    }
    
    private void redirectToDownloadAPI(String downloadType, Long fileId) {
        redirectToDownloadAPI(downloadType, fileId, true);
    }
    
    private void redirectToBatchDownloadAPI(String multiFileString, Boolean downloadOriginal){
        redirectToBatchDownloadAPI(multiFileString, true, downloadOriginal);
    }

    
    /**
     * Launch an "explore" tool which is a type of ExternalTool such as
     * TwoRavens or Data Explorer. This method may be invoked directly from the
     * xhtml if no popup is required (no terms of use, no guestbook, etc.).
     */
    public void explore(GuestbookResponse guestbookResponse, FileMetadata fmd, ExternalTool externalTool) {
        ApiToken apiToken = null;
        User user = session.getUser();
        if (user instanceof AuthenticatedUser) {
            AuthenticatedUser authenticatedUser = (AuthenticatedUser) user;
            apiToken = authService.findApiTokenByUser(authenticatedUser);
        }
        DataFile dataFile = null;
        if (fmd != null) {
            dataFile = fmd.getDataFile();
        } else {
            if (guestbookResponse != null) {
                dataFile = guestbookResponse.getDataFile();
            }
        }
        ExternalToolHandler externalToolHandler = new ExternalToolHandler(externalTool, dataFile, apiToken);
        // Back when we only had TwoRavens, the downloadType was always "Explore". Now we persist the name of the tool (i.e. "TwoRavens", "Data Explorer", etc.)
        guestbookResponse.setDownloadtype(externalTool.getDisplayName());
        String toolUrl = externalToolHandler.getToolUrlWithQueryParams();
        logger.fine("Exploring with " + toolUrl);
        try {
            FacesContext.getCurrentInstance().getExternalContext().redirect(toolUrl);
        } catch (IOException ex) {
            logger.info("Problem exploring with " + toolUrl + " - " + ex);
        }
        // This is the old logic from TwoRavens, null checks and all.
        if (guestbookResponse != null && guestbookResponse.isWriteResponse()
                && ((fmd != null && fmd.getDataFile() != null) || guestbookResponse.getDataFile() != null)) {
            if (guestbookResponse.getDataFile() == null && fmd != null) {
                guestbookResponse.setDataFile(fmd.getDataFile());
            }
            if (fmd == null || !fmd.getDatasetVersion().isDraft()) {
                writeGuestbookResponseRecord(guestbookResponse);
            }
        }
    }

    public String startWorldMapDownloadLink(GuestbookResponse guestbookResponse, FileMetadata fmd){
                
        if (guestbookResponse != null  && guestbookResponse.isWriteResponse() && ((fmd != null && fmd.getDataFile() != null) || guestbookResponse.getDataFile() != null)){
            if(guestbookResponse.getDataFile() == null && fmd != null){
                guestbookResponse.setDataFile(fmd.getDataFile());
            }
            if (fmd == null || !fmd.getDatasetVersion().isDraft()){
                writeGuestbookResponseRecord(guestbookResponse);
            }
        }
        DataFile file = null;
        if (fmd != null){
            file  = fmd.getDataFile();
        }
        if (guestbookResponse != null && guestbookResponse.getDataFile() != null && file == null){
            file  = guestbookResponse.getDataFile();
        }
        

        String retVal = worldMapPermissionHelper.getMapLayerMetadata(file).getLayerLink();
        
        try {
            FacesContext.getCurrentInstance().getExternalContext().redirect(retVal);
            return retVal;
        } catch (IOException ex) {
            logger.info("Failed to issue a redirect to file download url.");
        }
        return retVal;
    }

    public Boolean canSeeTwoRavensExploreButton(){
        return false;
    }
    
    
    public Boolean canUserSeeExploreWorldMapButton(){
        return false;
    }
    
    public void downloadDatasetCitationXML(Dataset dataset) {
        downloadCitationXML(null, dataset, false);
    }

    public void downloadDatafileCitationXML(FileMetadata fileMetadata) {
        downloadCitationXML(fileMetadata, null, false);
    }
    
    public void downloadDirectDatafileCitationXML(FileMetadata fileMetadata) {
        downloadCitationXML(fileMetadata, null, true);
    }

    public void downloadCitationXML(FileMetadata fileMetadata, Dataset dataset, boolean direct) {
    	DataCitation citation=null;
        if (dataset != null){
        	citation = new DataCitation(dataset.getLatestVersion());
        } else {
            citation= new DataCitation(fileMetadata, direct);
        }
        FacesContext ctx = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) ctx.getExternalContext().getResponse();
        response.setContentType("text/xml");
        String fileNameString;
        if (fileMetadata == null || fileMetadata.getLabel() == null) {
            // Dataset-level citation: 
            fileNameString = "attachment;filename=" + getFileNameDOI(citation.getPersistentId()) + ".xml";
        } else {
            // Datafile-level citation:
            fileNameString = "attachment;filename=" + getFileNameDOI(citation.getPersistentId()) + "-" + FileUtil.getCiteDataFileFilename(citation.getFileTitle(), FileUtil.FileCitationExtension.ENDNOTE);
        }
        response.setHeader("Content-Disposition", fileNameString);
        try {
            ServletOutputStream out = response.getOutputStream();
            citation.writeAsEndNoteCitation(out);
            out.flush();
            ctx.responseComplete();
        } catch (IOException e) {

        }
    }
    
    public void downloadDatasetCitationRIS(Dataset dataset) {

        downloadCitationRIS(null, dataset, false);

    }

    public void downloadDatafileCitationRIS(FileMetadata fileMetadata) {
        downloadCitationRIS(fileMetadata, null, false);
    }
    
    public void downloadDirectDatafileCitationRIS(FileMetadata fileMetadata) {
        downloadCitationRIS(fileMetadata, null, true);
    }

    public void downloadCitationRIS(FileMetadata fileMetadata, Dataset dataset, boolean direct) {
    	DataCitation citation=null;
        if (dataset != null){
        	citation = new DataCitation(dataset.getLatestVersion());
        } else {
            citation= new DataCitation(fileMetadata, direct);
        }

        FacesContext ctx = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) ctx.getExternalContext().getResponse();
        response.setContentType("application/download");

        String fileNameString;
        if (fileMetadata == null || fileMetadata.getLabel() == null) {
            // Dataset-level citation: 
            fileNameString = "attachment;filename=" + getFileNameDOI(citation.getPersistentId()) + ".ris";
        } else {
            // Datafile-level citation:
            fileNameString = "attachment;filename=" + getFileNameDOI(citation.getPersistentId()) + "-" + FileUtil.getCiteDataFileFilename(citation.getFileTitle(), FileUtil.FileCitationExtension.RIS);
        }
        response.setHeader("Content-Disposition", fileNameString);

        try {
            ServletOutputStream out = response.getOutputStream();
            citation.writeAsRISCitation(out);
            out.flush();
            ctx.responseComplete();
        } catch (IOException e) {

        }
    }
    
    private String getFileNameDOI(GlobalId id) {
        return "DOI:" + id.getAuthority() + "_" + id.getIdentifier();
    }

    public void downloadDatasetCitationBibtex(Dataset dataset) {

        downloadCitationBibtex(null, dataset, false);

    }

    public void downloadDatafileCitationBibtex(FileMetadata fileMetadata) {
        downloadCitationBibtex(fileMetadata, null, false);
    }

    public void downloadDirectDatafileCitationBibtex(FileMetadata fileMetadata) {
        downloadCitationBibtex(fileMetadata, null, true);
    }
    
    public void downloadCitationBibtex(FileMetadata fileMetadata, Dataset dataset, boolean direct) {
    	DataCitation citation=null;
        if (dataset != null){
        	citation = new DataCitation(dataset.getLatestVersion());
        } else {
            citation= new DataCitation(fileMetadata, direct);
        }
        //SEK 12/3/2018 changing this to open the json in a new tab. 
        FacesContext ctx = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) ctx.getExternalContext().getResponse();
        response.setContentType("application/json");

        String fileNameString;
        if (fileMetadata == null || fileMetadata.getLabel() == null) {
            // Dataset-level citation:
            fileNameString = "inline;filename=" + getFileNameDOI(citation.getPersistentId()) + ".bib";
        } else {
            // Datafile-level citation:
            fileNameString = "inline;filename=" + getFileNameDOI(citation.getPersistentId()) + "-" + FileUtil.getCiteDataFileFilename(citation.getFileTitle(), FileUtil.FileCitationExtension.BIBTEX);
        }
        response.setHeader("Content-Disposition", fileNameString);

        try {
            ServletOutputStream out = response.getOutputStream();
            citation.writeAsBibtexCitation(out);
            out.flush();
            ctx.responseComplete();
        } catch (IOException e) {

        }
    }
    
    public boolean requestAccess(Long fileId) {   
        if (dvRequestService.getDataverseRequest().getAuthenticatedUser() == null){
            return false;
        }
        DataFile file = datafileService.find(fileId);
        if (!file.getFileAccessRequesters().contains((AuthenticatedUser)session.getUser())) {
            try {
                commandEngine.submit(new RequestAccessCommand(dvRequestService.getDataverseRequest(), file));                        
                return true;
            } catch (CommandException ex) {
                logger.info("Unable to request access for file id " + fileId + ". Exception: " + ex);
            }             
        }
        
        return false;
    }    
    
    public void sendRequestFileAccessNotification(Dataset dataset, Long fileId, AuthenticatedUser requestor) {
        permissionService.getUsersWithPermissionOn(Permission.ManageDatasetPermissions, dataset).stream().forEach((au) -> {
            userNotificationService.sendNotification(au, new Timestamp(new Date().getTime()), UserNotification.Type.REQUESTFILEACCESS, fileId, null, requestor);
        });

    }    


    
}