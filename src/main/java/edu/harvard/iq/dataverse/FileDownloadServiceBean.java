package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.datasetutility.TwoRavensHelper;
import edu.harvard.iq.dataverse.datasetutility.WorldMapPermissionHelper;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.CreateGuestbookResponseCommand;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import org.primefaces.context.RequestContext;

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
    
    @Inject
    DataverseSession session;
    
    @EJB
    EjbDataverseEngine commandEngine;
    
    @Inject
    DataverseRequestServiceBean dvRequestService;
    
    @Inject TwoRavensHelper twoRavensHelper;
    @Inject WorldMapPermissionHelper worldMapPermissionHelper;

    private static final Logger logger = Logger.getLogger(FileDownloadServiceBean.class.getCanonicalName());
    
    
    public void writeGuestbookAndStartDownload(GuestbookResponse guestbookResponse){
        if (guestbookResponse != null && guestbookResponse.getDataFile() != null     ){
            writeGuestbookResponseRecord(guestbookResponse);
            callDownloadServlet(guestbookResponse.getFileFormat(), guestbookResponse.getDataFile().getId(), guestbookResponse.isWriteResponse());
        }
        
        if (guestbookResponse != null && guestbookResponse.getSelectedFileIds() != null     ){
            List<String> list = new ArrayList<>(Arrays.asList(guestbookResponse.getSelectedFileIds().split(",")));

            for (String idAsString : list) {
                DataFile df = datafileService.findCheapAndEasy(new Long(idAsString)) ;
                if (df != null) {
                    guestbookResponse.setDataFile(df);
                    writeGuestbookResponseRecord(guestbookResponse);
                }
            }
            
            callDownloadServlet(guestbookResponse.getSelectedFileIds(), true);
        }
        
        
    }
    
    public void writeGuestbookResponseRecord(GuestbookResponse guestbookResponse) {

        try {
            Command cmd = new CreateGuestbookResponseCommand(dvRequestService.getDataverseRequest(), guestbookResponse, guestbookResponse.getDataset());
            commandEngine.submit(cmd);
        } catch (CommandException e) {
            //if an error occurs here then download won't happen no need for response recs...

        }

    }

    public void callDownloadServlet(String multiFileString, Boolean gbRecordsWritten){

        String fileDownloadUrl = "/api/access/datafiles/" + multiFileString;
        if (gbRecordsWritten){
            fileDownloadUrl += "?gbrecs=true";
        }
        try {
            FacesContext.getCurrentInstance().getExternalContext().redirect(fileDownloadUrl);
        } catch (IOException ex) {
            logger.info("Failed to issue a redirect to file download url.");
        }

        //return fileDownloadUrl;
    }
    
    //private String callDownloadServlet( String downloadType, Long fileId){
    public void callDownloadServlet( String downloadType, Long fileId, Boolean gbRecordsWritten){
        
        String fileDownloadUrl = "/api/access/datafile/" + fileId;
                    
        if (downloadType != null && downloadType.equals("bundle")){
            fileDownloadUrl = "/api/access/datafile/bundle/" + fileId;
        }
        if (downloadType != null && downloadType.equals("original")){
            fileDownloadUrl = "/api/access/datafile/" + fileId + "?format=original";
        }
        if (downloadType != null && downloadType.equals("RData")){
            fileDownloadUrl = "/api/access/datafile/" + fileId + "?format=RData";
        }
        if (downloadType != null && downloadType.equals("var")){
            fileDownloadUrl = "/api/meta/datafile/" + fileId;
        }
        if (downloadType != null && downloadType.equals("tab")){
            fileDownloadUrl = "/api/access/datafile/" + fileId+ "?format=tab";
        }
        if (gbRecordsWritten){
            if(downloadType != null && ( downloadType.equals("original") || downloadType.equals("RData") || downloadType.equals("tab")) ){
                fileDownloadUrl += "&gbrecs=true"; 
            } else {
                fileDownloadUrl += "?gbrecs=true"; 
            }
           
        }
        logger.fine("Returning file download url: " + fileDownloadUrl);
        try {
            FacesContext.getCurrentInstance().getExternalContext().redirect(fileDownloadUrl);
        } catch (IOException ex) {
            logger.info("Failed to issue a redirect to file download url.");
        }
        //return fileDownloadUrl;       
    }
    
        //public String startFileDownload(FileMetadata fileMetadata, String format) {
    public void startFileDownload(GuestbookResponse guestbookResponse, FileMetadata fileMetadata, String format) {
        boolean recordsWritten = false;
        if(!fileMetadata.getDatasetVersion().isDraft()){
           guestbookResponse = guestbookResponseService.modifyDatafileAndFormat(guestbookResponse, fileMetadata, format);
           writeGuestbookResponseRecord(guestbookResponse);
            recordsWritten = true;
        }
        callDownloadServlet(format, fileMetadata.getDataFile().getId(), recordsWritten);
        logger.fine("issued file download redirect for filemetadata "+fileMetadata.getId()+", datafile "+fileMetadata.getDataFile().getId());
    }
    
    public String startExploreDownloadLink(GuestbookResponse guestbookResponse, FileMetadata fmd){

        if (guestbookResponse != null && guestbookResponse.isWriteResponse() 
                && (( fmd != null && fmd.getDataFile() != null) || guestbookResponse.getDataFile() != null)){
            if(guestbookResponse.getDataFile() == null  && fmd != null){                
                guestbookResponse.setDataFile(fmd.getDataFile());
            }
            if (fmd == null || !fmd.getDatasetVersion().isDraft()){
                writeGuestbookResponseRecord(guestbookResponse);
            }
        }
        
        Long datafileId;
        
        if (fmd == null && guestbookResponse != null && guestbookResponse.getDataFile() != null){
            datafileId = guestbookResponse.getDataFile().getId();
        } else {
            datafileId = fmd.getDataFile().getId();
        }
        String retVal = twoRavensHelper.getDataExploreURLComplete(datafileId);
        
        try {
            FacesContext.getCurrentInstance().getExternalContext().redirect(retVal);
            return retVal;
        } catch (IOException ex) {
            logger.info("Failed to issue a redirect to file download url.");
        }
        return retVal;
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
        if (guestbookResponse != null && guestbookResponse.getDataFile() != null){
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
          
    public boolean isDownloadPopupRequired(DatasetVersion datasetVersion) {
        // Each of these conditions is sufficient reason to have to 
        // present the user with the popup: 
        if (datasetVersion == null){
            return false;
        }
        //0. if version is draft then Popup "not required"    
        if (!datasetVersion.isReleased()){
            return false;
        }
        // 1. License and Terms of Use:
        if (datasetVersion.getTermsOfUseAndAccess() != null) {
            if (!TermsOfUseAndAccess.License.CC0.equals(datasetVersion.getTermsOfUseAndAccess().getLicense())
                    && !(datasetVersion.getTermsOfUseAndAccess().getTermsOfUse() == null
                    || datasetVersion.getTermsOfUseAndAccess().getTermsOfUse().equals(""))) {
                return true;
            }

            // 2. Terms of Access:
            if (!(datasetVersion.getTermsOfUseAndAccess().getTermsOfAccess() == null) && !datasetVersion.getTermsOfUseAndAccess().getTermsOfAccess().equals("")) {
                return true;
            }
        }

        // 3. Guest Book: 
        if (datasetVersion.getDataset().getGuestbook() != null && datasetVersion.getDataset().getGuestbook().isEnabled() && datasetVersion.getDataset().getGuestbook().getDataverse() != null ) {
            return true;
        }

        return false;
    }
    
    public Boolean canSeeTwoRavensExploreButton(){
        return false;
    }
    
    
    public Boolean canUserSeeExploreWorldMapButton(){
        return false;
    }
    
    public void downloadDatasetCitationXML(Dataset dataset) {
        downloadCitationXML(null, dataset);
    }

    public void downloadDatafileCitationXML(FileMetadata fileMetadata) {
        downloadCitationXML(fileMetadata, null);
    }

    public void downloadCitationXML(FileMetadata fileMetadata, Dataset dataset) {
        DatasetVersion workingVersion;
        if (dataset != null){
            workingVersion = dataset.getLatestVersion();
        } else {
            workingVersion = fileMetadata.getDatasetVersion();
        }
        String xml = datasetService.createCitationXML(workingVersion, fileMetadata);
        FacesContext ctx = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) ctx.getExternalContext().getResponse();
        response.setContentType("text/xml");
        String fileNameString = "";
        if (fileMetadata == null || fileMetadata.getLabel() == null) {
            // Dataset-level citation: 
            fileNameString = "attachment;filename=" + getFileNameDOI(workingVersion) + ".xml";
        } else {
            // Datafile-level citation:
            fileNameString = "attachment;filename=" + getFileNameDOI(workingVersion) + "-" + fileMetadata.getLabel().replaceAll("\\.tab$", "-endnote.xml");
        }
        response.setHeader("Content-Disposition", fileNameString);
        try {
            ServletOutputStream out = response.getOutputStream();
            out.write(xml.getBytes());
            out.flush();
            ctx.responseComplete();
        } catch (Exception e) {

        }
    }
    
    public void downloadDatasetCitationRIS(Dataset dataset) {

        downloadCitationRIS(null, dataset);

    }

    public void downloadDatafileCitationRIS(FileMetadata fileMetadata) {
        downloadCitationRIS(fileMetadata, null);
    }

    public void downloadCitationRIS(FileMetadata fileMetadata, Dataset dataset) {
        DatasetVersion workingVersion;
        if (dataset != null){
            workingVersion = dataset.getLatestVersion();
        } else {
            workingVersion = fileMetadata.getDatasetVersion();
        }
        String risFormatDowload = datasetService.createCitationRIS(workingVersion, fileMetadata);
        FacesContext ctx = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) ctx.getExternalContext().getResponse();
        response.setContentType("application/download");

        String fileNameString = "";
        if (fileMetadata == null || fileMetadata.getLabel() == null) {
            // Dataset-level citation: 
            fileNameString = "attachment;filename=" + getFileNameDOI(workingVersion) + ".ris";
        } else {
            // Datafile-level citation:
            fileNameString = "attachment;filename=" + getFileNameDOI(workingVersion) + "-" + fileMetadata.getLabel().replaceAll("\\.tab$", ".ris");
        }
        response.setHeader("Content-Disposition", fileNameString);

        try {
            ServletOutputStream out = response.getOutputStream();
            out.write(risFormatDowload.getBytes());
            out.flush();
            ctx.responseComplete();
        } catch (Exception e) {

        }
    }
    
    private String getFileNameDOI(DatasetVersion workingVersion) {
        Dataset ds = workingVersion.getDataset();
        return "DOI:" + ds.getAuthority() + "_" + ds.getIdentifier().toString();
    }

    public void downloadDatasetCitationBibtex(Dataset dataset) {

        downloadCitationBibtex(null, dataset);

    }

    public void downloadDatafileCitationBibtex(FileMetadata fileMetadata) {
        downloadCitationBibtex(fileMetadata, null);
    }

    public void downloadCitationBibtex(FileMetadata fileMetadata, Dataset dataset) {
        DatasetVersion workingVersion;
        if (dataset != null){
            workingVersion = dataset.getLatestVersion();
        } else {
            workingVersion = fileMetadata.getDatasetVersion();
        }
        String bibFormatDowload = new BibtexCitation(workingVersion).toString();
        FacesContext ctx = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) ctx.getExternalContext().getResponse();
        response.setContentType("application/download");

        String fileNameString = "";
        if (fileMetadata == null || fileMetadata.getLabel() == null) {
            // Dataset-level citation:
            fileNameString = "attachment;filename=" + getFileNameDOI(workingVersion) + ".bib";
        } else {
            // Datafile-level citation:
            fileNameString = "attachment;filename=" + getFileNameDOI(workingVersion) + "-" + fileMetadata.getLabel().replaceAll("\\.tab$", ".bib");
        }
        response.setHeader("Content-Disposition", fileNameString);

        try {
            ServletOutputStream out = response.getOutputStream();
            out.write(bibFormatDowload.getBytes());
            out.flush();
            ctx.responseComplete();
        } catch (Exception e) {

        }
    }

    
    public void requestAccess(DataFile file, boolean sendNotification) {
        if (!file.getFileAccessRequesters().contains((AuthenticatedUser) session.getUser())) {
            file.getFileAccessRequesters().add((AuthenticatedUser) session.getUser());
            datafileService.save(file);

            // create notifications
            if (sendNotification) {
                sendRequestFileAccessNotification(file);

            }
        }
    }

    private void sendRequestFileAccessNotification(DataFile file) {
        for (AuthenticatedUser au : permissionService.getUsersWithPermissionOn(Permission.ManageDatasetPermissions, file.getOwner())) {
            userNotificationService.sendNotification(au, new Timestamp(new Date().getTime()), UserNotification.Type.REQUESTFILEACCESS, file.getId());
        }

    }

    
}