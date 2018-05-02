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
    
    
    public void writeGuestbookAndStartDownload(GuestbookResponse guestbookResponse){

        if (guestbookResponse != null && guestbookResponse.getDataFile() != null     ){
            writeGuestbookResponseRecord(guestbookResponse);
            logger.info("wGBandSD: " + guestbookResponse.isWriteResponse());
            callDownloadServlet(guestbookResponse.getFileFormat(), guestbookResponse.getDataFile().getId(), guestbookResponse.isWriteResponse());
        }
        
        if (guestbookResponse != null && guestbookResponse.getSelectedFileIds() != null     ){
            List<String> list = new ArrayList<>(Arrays.asList(guestbookResponse.getSelectedFileIds().split(",")));

            for (String idAsString : list) {
                DataFile df = datafileService.findCheapAndEasy(new Long(idAsString)) ;
                if (df != null) {
                    guestbookResponse.setDataFile(df);
                    logger.info("wGBandSD multi: " + guestbookResponse.isWriteResponse());
                    writeGuestbookResponseRecord(guestbookResponse);
                }
            }
            
            callDownloadServlet(guestbookResponse.getSelectedFileIds(), true);
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

    public void callDownloadServlet(String multiFileString, Boolean gbRecordsWritten){

        String fileDownloadUrl = "/api/access/datafiles/" + multiFileString;
        if (gbRecordsWritten){
            fileDownloadUrl += "?gbrecs=true";
        }
     logger.info("callDownloadServlet: " + fileDownloadUrl);   
        try {
            FacesContext.getCurrentInstance().getExternalContext().redirect(fileDownloadUrl);
        } catch (IOException ex) {
            logger.info("Failed to issue a redirect to file download url.");
        }

        //return fileDownloadUrl;
    }

    public void callDownloadServlet(String downloadType, Long fileId, boolean gbRecordsWritten) {
        String fileDownloadUrl = FileUtil.getFileDownloadUrlPath(downloadType, fileId, gbRecordsWritten);
        logger.info("callDownloadServlet: " + fileDownloadUrl);
        logger.fine("Redirecting to file download url: " + fileDownloadUrl);
        try {
            FacesContext.getCurrentInstance().getExternalContext().redirect(fileDownloadUrl);
        } catch (IOException ex) {
            logger.info("Failed to issue a redirect to file download url (" + fileDownloadUrl + "): " + ex);
        }
    }
    

    //public String startFileDownload(FileMetadata fileMetadata, String format) {
    public void startFileDownload(GuestbookResponse guestbookResponse, FileMetadata fileMetadata, String format) {
        boolean recordsWritten = false;
        logger.info("SFD: fm.getDVersion.isDraft : " + fileMetadata.getDatasetVersion().isDraft());
        if(!fileMetadata.getDatasetVersion().isDraft()){
           guestbookResponse = guestbookResponseService.modifyDatafileAndFormat(guestbookResponse, fileMetadata, format);
           logger.info("SFD: writeResponse : " + guestbookResponse.isWriteResponse());
           writeGuestbookResponseRecord(guestbookResponse);
            recordsWritten = true;
        }
        callDownloadServlet(format, fileMetadata.getDataFile().getId(), recordsWritten);
        logger.fine("issued file download redirect for filemetadata "+fileMetadata.getId()+", datafile "+fileMetadata.getDataFile().getId());
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
        String fileNameString;
        if (fileMetadata == null || fileMetadata.getLabel() == null) {
            // Dataset-level citation: 
            fileNameString = "attachment;filename=" + getFileNameDOI(workingVersion) + ".xml";
        } else {
            // Datafile-level citation:
            fileNameString = "attachment;filename=" + getFileNameDOI(workingVersion) + "-" + FileUtil.getCiteDataFileFilename(fileMetadata, FileUtil.FileCitationExtension.ENDNOTE);
        }
        response.setHeader("Content-Disposition", fileNameString);
        try {
            ServletOutputStream out = response.getOutputStream();
            out.write(xml.getBytes());
            out.flush();
            ctx.responseComplete();
        } catch (IOException e) {

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

        String fileNameString;
        if (fileMetadata == null || fileMetadata.getLabel() == null) {
            // Dataset-level citation: 
            fileNameString = "attachment;filename=" + getFileNameDOI(workingVersion) + ".ris";
        } else {
            // Datafile-level citation:
            fileNameString = "attachment;filename=" + getFileNameDOI(workingVersion) + "-" + FileUtil.getCiteDataFileFilename(fileMetadata, FileUtil.FileCitationExtension.RIS);
        }
        response.setHeader("Content-Disposition", fileNameString);

        try {
            ServletOutputStream out = response.getOutputStream();
            out.write(risFormatDowload.getBytes());
            out.flush();
            ctx.responseComplete();
        } catch (IOException e) {

        }
    }
    
    private String getFileNameDOI(DatasetVersion workingVersion) {
        Dataset ds = workingVersion.getDataset();
        return "DOI:" + ds.getAuthority() + "_" + ds.getIdentifier();
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

        String fileNameString;
        if (fileMetadata == null || fileMetadata.getLabel() == null) {
            // Dataset-level citation:
            fileNameString = "attachment;filename=" + getFileNameDOI(workingVersion) + ".bib";
        } else {
            // Datafile-level citation:
            fileNameString = "attachment;filename=" + getFileNameDOI(workingVersion) + "-" + FileUtil.getCiteDataFileFilename(fileMetadata, FileUtil.FileCitationExtension.BIBTEX);
        }
        response.setHeader("Content-Disposition", fileNameString);

        try {
            ServletOutputStream out = response.getOutputStream();
            out.write(bibFormatDowload.getBytes());
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
    
    public void sendRequestFileAccessNotification(Dataset dataset, Long fileId) {
        permissionService.getUsersWithPermissionOn(Permission.ManageDatasetPermissions, dataset).stream().forEach((au) -> {
            userNotificationService.sendNotification(au, new Timestamp(new Date().getTime()), UserNotification.Type.REQUESTFILEACCESS, fileId);
        });

    }    


    
}