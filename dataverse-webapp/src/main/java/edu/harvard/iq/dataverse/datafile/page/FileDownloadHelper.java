/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.datafile.page;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.datafile.FileDownloadServiceBean;
import edu.harvard.iq.dataverse.guestbook.GuestbookResponseServiceBean;
import edu.harvard.iq.dataverse.persistence.datafile.ExternalTool;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse.TermsOfUseType;
import edu.harvard.iq.dataverse.persistence.guestbook.GuestbookResponse;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.PrimefacesUtil;

import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.logging.Logger;

import static java.util.stream.Collectors.toList;

/**
 * Class joining downloads and guestbook response writing on UI
 * <p>
 * Note that following dialogs must be defined on page for proper functioning:
 * <ul>
 * <li>downloadPopup</li>
 * <li>downloadDataSubsetPopup</li>
 * <li>downloadPackagePopup</li>
 * </ul>
 * 
 * @author skraffmi
 */
@ViewScoped
@Named
public class FileDownloadHelper implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(FileDownloadHelper.class.getCanonicalName());
    
    private DataverseSession session;
    
    private PermissionsWrapper permissionsWrapper;

    private FileDownloadServiceBean fileDownloadService;

    private GuestbookResponseServiceBean guestbookResponseService;

    private RequestedDownloadType requestedDownloadType;

    private WholeDatasetDownloadUiLogger datasetDownloadUiLogger;


    // -------------------- CONSTRUCTORS --------------------
    
    @Deprecated
    public FileDownloadHelper() {
        
    }
    
    @Inject
    public FileDownloadHelper(DataverseSession session, PermissionsWrapper permissionsWrapper,
            FileDownloadServiceBean fileDownloadService, GuestbookResponseServiceBean guestbookResponseService,
            RequestedDownloadType requestedDownloadType, WholeDatasetDownloadUiLogger datasetDownloadUiLogger) {
        this.session = session;
        this.permissionsWrapper = permissionsWrapper;
        this.fileDownloadService = fileDownloadService;
        this.guestbookResponseService = guestbookResponseService;
        this.requestedDownloadType = requestedDownloadType;
        this.datasetDownloadUiLogger = datasetDownloadUiLogger;
    }
    
    // -------------------- LOGIC --------------------

    /**
     * Writes guestbook response and start download
     * 
     * @param userProvidedGuestbookResponse - guestbook response with values
     * that should be provided by user (already validated) (that is name, email, custom question responses, etc.)
     */
    public void writeGuestbookAndStartDownloadAccordingToType(GuestbookResponse userProvidedGuestbookResponse) {

        DownloadType fileFormat = requestedDownloadType.getFileFormat();
        List<FileMetadata> fileMetadatas = requestedDownloadType.getFileMetadatas();
        
        writeGuestbookResponsesForFiles(fileMetadatas, fileFormat, userProvidedGuestbookResponse);
        
        startDownloadAccordingToType(fileMetadatas, fileFormat, requestedDownloadType.getTool());
        
    }
    
    /**
     * Initializes {@link RequestedDownloadType} object and start
     * downloading process using {@link #requestDownload()}.
     * 
     * @param fileMetadatas - files to download
     * @param requestedOriginalDownload - true if should download original format of files
     * or false for default download
     */
    public void requestDownloadWithFiles(List<FileMetadata> fileMetadatas, boolean requestedOriginalDownload) {
        
        if (requestedOriginalDownload) {
            requestedDownloadType.initMultiOriginalDownloadType(fileMetadatas);
        } else {
            requestedDownloadType.initMultiDownloadType(fileMetadatas);
        }
        
        requestDownload();
    }
    
    /**
     * Starts downloading process according on values stored in {@link RequestedDownloadType}.
     * <p>
     * If guestbook fill is required then it shows proper dialog or handles download
     * directly otherwise.
     */
    public void requestDownload() {
        
        List<FileMetadata> fileMetadatas = requestedDownloadType.getFileMetadatas();
        DownloadType fileFormat = requestedDownloadType.getFileFormat();
        
        if (FileUtil.isDownloadPopupRequired(fileMetadatas.get(0).getDatasetVersion())) {
            PrimefacesUtil.showDialog("downloadPopup");
            return;
        }
        
        if (fileMetadatas.get(0).getDatasetVersion().isDraft()) {
            GuestbookResponse downloadOnlyGuestbook = guestbookResponseService.initGuestbookResponseForFragment(fileMetadatas.get(0), session);
            writeGuestbookResponsesForFiles(fileMetadatas, fileFormat, downloadOnlyGuestbook);
        }
        
        datasetDownloadUiLogger.incrementLogIfDownloadingWholeDataset(fileMetadatas);
        startDownloadAccordingToType(fileMetadatas, fileFormat, requestedDownloadType.getTool());
        
        return;
    }

    public boolean canUserDownloadFile(FileMetadata fileMetadata) {
        if (fileMetadata == null) {
            return false;
        }

        if ((fileMetadata.getId() == null) || (fileMetadata.getDataFile().getId() == null)) {
            return false;
        }

        if (fileMetadata.getDatasetVersion().isDeaccessioned()) {
            return permissionsWrapper.canCurrentUserUpdateDataset(fileMetadata.getDatasetVersion().getDataset());
        }


        boolean isRestrictedFile = fileMetadata.getTermsOfUse().getTermsOfUseType() == TermsOfUseType.RESTRICTED;
        if (!isRestrictedFile) {
            return true;
        }

        // See if the DataverseRequest, which contains IP Groups, has permission to download the file.
        if (permissionsWrapper.hasDownloadFilePermission(fileMetadata.getDataFile())) {
            logger.fine("The DataverseRequest (User plus IP address) has access to download the file.");
            return true;
        }

        return false;
    }


    // -------------------- PRIVATE --------------------

    private void startDownloadAccordingToType(List<FileMetadata> fileMetadatas, DownloadType fileFormat, ExternalTool tool) {
        if (fileMetadatas.size() > 1) {
            fileDownloadService.redirectToBatchDownloadAPI(
                    fileMetadatas.stream().map(x -> x.getDataFile().getId()).collect(toList()),
                    true, fileFormat.getApiBatchDownloadEquivalent());
            return;
        }

        FileMetadata fileMetadata = fileMetadatas.get(0);

        if (fileFormat.isCompatibleWithApiDownload()) {
            fileDownloadService.redirectToDownloadAPI(fileFormat.getApiDownloadEquivalent(), fileMetadata.getDataFile().getId(), true);

        } else if (fileFormat == DownloadType.SUBSET) {
            PrimefacesUtil.showDialog("downloadDataSubsetPopup");

        } else if (fileFormat == DownloadType.WORLDMAP) {
            fileDownloadService.startWorldMapDownloadLink(fileMetadata);

        } else if (fileFormat == DownloadType.EXTERNALTOOL) {
            fileDownloadService.explore(fileMetadata, tool);

        } else if (fileFormat == DownloadType.PACKAGE) {
            PrimefacesUtil.showDialogAndResize("downloadPackagePopup");
        }
    }

    private void writeGuestbookResponsesForFiles(List<FileMetadata> fileMetadatas, DownloadType fileFormat, 
            GuestbookResponse guestbookResponseBase) {
        
        guestbookResponseBase.setDataset(fileMetadatas.get(0).getDatasetVersion().getDataset());
        guestbookResponseBase.setDownloadtype(buildGuestbookResponseDownloadType(fileFormat, requestedDownloadType.getTool()));
        
        for (FileMetadata fileMetadata: fileMetadatas) {
            
            guestbookResponseBase.setDataFile(fileMetadata.getDataFile());
            
            fileDownloadService.writeGuestbookResponseRecord(guestbookResponseBase);
        }
    }
    
    private String buildGuestbookResponseDownloadType(DownloadType fileFormat, ExternalTool tool) {
        
        if (fileFormat == DownloadType.SUBSET) {
            return "Subset";
        }
        if (fileFormat == DownloadType.WORLDMAP) {
            return "WorldMap";
        }
        if (fileFormat == DownloadType.EXTERNALTOOL) {
            return tool.getDisplayName();
        }
        return "Download";
    }
}
