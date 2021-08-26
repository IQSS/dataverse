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
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.ExternalTool;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse.TermsOfUseType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.guestbook.GuestbookResponse;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.PrimefacesUtil;
import org.apache.commons.lang3.StringUtils;
import org.omnifaces.cdi.ViewScoped;
import org.primefaces.PrimeFaces;

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

    // -------------------- CONSTRUCTORS --------------------
    
    @Deprecated
    public FileDownloadHelper() {
        
    }
    
    @Inject
    public FileDownloadHelper(DataverseSession session, PermissionsWrapper permissionsWrapper,
                              FileDownloadServiceBean fileDownloadService, GuestbookResponseServiceBean guestbookResponseService,
                              RequestedDownloadType requestedDownloadType) {
        this.session = session;
        this.permissionsWrapper = permissionsWrapper;
        this.fileDownloadService = fileDownloadService;
        this.guestbookResponseService = guestbookResponseService;
        this.requestedDownloadType = requestedDownloadType;
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
        
        userProvidedGuestbookResponse.setDownloadtype(buildGuestbookResponseDownloadType(fileFormat, requestedDownloadType.getTool()));
        
        for (FileMetadata fileMetadata: fileMetadatas) {
            DataFile dataFile = fileMetadata.getDataFile();
            
            if (dataFile.isReleased()) {
                userProvidedGuestbookResponse.setDataFile(fileMetadata.getDataFile());
                fileDownloadService.writeGuestbookResponseRecord(userProvidedGuestbookResponse);
            }
        }

        startDownloadAccordingToType(fileMetadatas, fileFormat, requestedDownloadType.getTool(), true);
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

    public String requestDownloadOfWholeDataset(DatasetVersion dsv, boolean requestedOriginalDownload) {

        DownloadType downloadType = requestedOriginalDownload ? DownloadType.ORIGINAL : DownloadType.DOWNLOAD;

        String filesDownloadUrl = FileUtil.getDownloadWholeDatasetUrlPath(dsv, false, downloadType.getApiBatchDownloadEquivalent());
        PrimeFaces.current().ajax().addCallbackParam("apiDownloadLink", filesDownloadUrl);

        return StringUtils.EMPTY;
    }
    
    /**
     * Starts downloading process according on values stored in {@link RequestedDownloadType}.
     * <p>
     * If guestbook fill is required then it shows proper dialog or handles download
     * directly otherwise.
     */
    public String requestDownload() {
        
        List<FileMetadata> fileMetadatas = requestedDownloadType.getFileMetadatas();
        DownloadType fileFormat = requestedDownloadType.getFileFormat();
        
        if (FileUtil.isDownloadPopupRequired(fileMetadatas.get(0).getDatasetVersion())) {
            PrimefacesUtil.showDialog("downloadPopup");
            return StringUtils.EMPTY;
        }

        startDownloadAccordingToType(fileMetadatas, fileFormat, requestedDownloadType.getTool(), false);
        return StringUtils.EMPTY;
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

    private void startDownloadAccordingToType(List<FileMetadata> fileMetadatas, DownloadType fileFormat, ExternalTool tool, boolean guestbookRecordsAlreadyWritten) {
        if (fileMetadatas.size() > 1) {
            String filesDownloadUrl = FileUtil.getBatchFilesDownloadUrlPath(
                    fileMetadatas.stream().map(x -> x.getDataFile().getId()).collect(toList()), guestbookRecordsAlreadyWritten,
                    fileFormat.getApiBatchDownloadEquivalent());
            PrimeFaces.current().ajax().addCallbackParam("apiDownloadLink", filesDownloadUrl);
            
            return;
        }

        DataFile dataFile = fileMetadatas.get(0).getDataFile();

        if (fileFormat.isCompatibleWithApiDownload()) {
            String fileDownloadUrl = FileUtil.getFileDownloadUrlPath(
                        fileFormat.getApiDownloadEquivalent(),
                        dataFile.getId(),
                        guestbookRecordsAlreadyWritten);
            PrimeFaces.current().ajax().addCallbackParam("apiDownloadLink", fileDownloadUrl);

        } else if (fileFormat == DownloadType.SUBSET) {
            writeGuestbookResponseIfReleased(dataFile, DownloadType.SUBSET, null, guestbookRecordsAlreadyWritten);
            PrimefacesUtil.showDialog("downloadDataSubsetPopup");

        } else if (fileFormat == DownloadType.WORLDMAP) {
            writeGuestbookResponseIfReleased(dataFile, DownloadType.WORLDMAP, null, guestbookRecordsAlreadyWritten);
            fileDownloadService.startWorldMapDownloadLink(dataFile);

        } else if (fileFormat == DownloadType.EXTERNALTOOL) {
            writeGuestbookResponseIfReleased(dataFile, DownloadType.EXTERNALTOOL, tool, guestbookRecordsAlreadyWritten);
            fileDownloadService.explore(dataFile, tool);

        } else if (fileFormat == DownloadType.PACKAGE) {
            PrimefacesUtil.showDialogAndResize("downloadPackagePopup");
        }
    }

    private void writeGuestbookResponseIfReleased(DataFile dataFile, DownloadType fileFormat, ExternalTool tool, boolean guestbookRecordsAlreadyWritten) {
        if (!guestbookRecordsAlreadyWritten && dataFile.isReleased()) {

            GuestbookResponse downloadOnlyGuestbook = guestbookResponseService.initUIGuestbookResponseWithoutFile(
                    dataFile.getOwner(), session);
            downloadOnlyGuestbook.setDownloadtype(buildGuestbookResponseDownloadType(fileFormat, tool));
            downloadOnlyGuestbook.setDataFile(dataFile);
            fileDownloadService.writeGuestbookResponseRecord(downloadOnlyGuestbook);
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
