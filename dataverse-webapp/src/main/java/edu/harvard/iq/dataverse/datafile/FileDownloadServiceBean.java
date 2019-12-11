package edu.harvard.iq.dataverse.datafile;

import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.datasetutility.WorldMapPermissionHelper;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.CreateGuestbookResponseCommand;
import edu.harvard.iq.dataverse.externaltools.ExternalToolHandler;
import edu.harvard.iq.dataverse.persistence.GlobalId;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.ExternalTool;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.dataset.DataCitation;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.guestbook.GuestbookResponse;
import edu.harvard.iq.dataverse.persistence.user.ApiToken;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.User;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.FileUtil.ApiBatchDownloadType;
import edu.harvard.iq.dataverse.util.FileUtil.ApiDownloadType;
import org.primefaces.PrimeFaces;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author skraffmi
 * Handles All File Download processes
 * including Guestbook responses
 */
@Stateless
@Named("fileDownloadService")
public class FileDownloadServiceBean implements java.io.Serializable {

    @EJB
    DataFileServiceBean datafileService;
    @EJB
    AuthenticationServiceBean authService;
    @EJB
    ExternalToolHandler externalToolHandler;

    @Inject
    DataverseSession session;

    @EJB
    EjbDataverseEngine commandEngine;

    @Inject
    DataverseRequestServiceBean dvRequestService;

    @Inject
    WorldMapPermissionHelper worldMapPermissionHelper;

    private static final Logger logger = Logger.getLogger(FileDownloadServiceBean.class.getCanonicalName());


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
    public void redirectToBatchDownloadAPI(List<Long> fileIds, boolean guestbookRecordsAlreadyWritten, ApiBatchDownloadType downloadType) {
        String filesDownloadUrl = FileUtil.getBatchFilesDownloadUrlPath(fileIds, guestbookRecordsAlreadyWritten, downloadType);
        try {
            FacesContext.getCurrentInstance().getExternalContext().redirect(filesDownloadUrl);
        } catch (IOException ex) {
            logger.info("Failed to issue a redirect to file download url.");
        }
    }


    public void redirectToDownloadAPI(ApiDownloadType downloadType, Long fileId, boolean guestBookRecordAlreadyWritten) {
        String fileDownloadUrl = FileUtil.getFileDownloadUrlPath(downloadType, fileId, guestBookRecordAlreadyWritten);
        logger.fine("Redirecting to file download url: " + fileDownloadUrl);
        try {
            FacesContext.getCurrentInstance().getExternalContext().redirect(fileDownloadUrl);
        } catch (IOException ex) {
            logger.info("Failed to issue a redirect to file download url (" + fileDownloadUrl + "): " + ex);
        }
    }
    
    /**
     * Launch an "explore" tool which is a type of ExternalTool such as
     * TwoRavens or Data Explorer. This method may be invoked directly from the
     * xhtml if no popup is required (no terms of use, no guestbook, etc.).
     */
    public void explore(FileMetadata fmd, ExternalTool externalTool) {
        ApiToken apiToken = null;
        User user = session.getUser();
        if (user instanceof AuthenticatedUser) {
            AuthenticatedUser authenticatedUser = (AuthenticatedUser) user;
            apiToken = authService.findApiTokenByUser(authenticatedUser);
        }
        DataFile dataFile = fmd.getDataFile();
        //For tools to get the dataset and datasetversion ids, we need a full DataFile object (not a findCheapAndEasy() copy)
        if (dataFile.getFileMetadata() == null) {
            dataFile = datafileService.find(dataFile.getId());
        }
        // Back when we only had TwoRavens, the downloadType was always "Explore". Now we persist the name of the tool (i.e. "TwoRavens", "Data Explorer", etc.)
        String toolUrl = externalToolHandler.buildToolUrlWithQueryParams(externalTool, dataFile, apiToken);
        logger.fine("Exploring with " + toolUrl);
        PrimeFaces.current().executeScript("window.open('" + toolUrl + "', target='_blank');");
    }
    
    public String startWorldMapDownloadLink(FileMetadata fileMetadata) {
        
        DataFile file = fileMetadata.getDataFile();
        String retVal = worldMapPermissionHelper.getMapLayerMetadata(file).getLayerLink();

        try {
            FacesContext.getCurrentInstance().getExternalContext().redirect(retVal);
            return retVal;
        } catch (IOException ex) {
            logger.info("Failed to issue a redirect to file download url.");
        }
        return retVal;
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
        DataCitation citation = null;
        if (dataset != null) {
            citation = new DataCitation(dataset.getLatestVersion());
        } else {
            citation = new DataCitation(fileMetadata, direct);
        }
        FacesContext ctx = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) ctx.getExternalContext().getResponse();
        response.setContentType("text/xml");
        String fileNameString;
        if (fileMetadata == null || fileMetadata.getLabel() == null) {
            // Dataset-level citation: 
            fileNameString = "attachment;filename=" + getFileNameFromPid(citation.getPersistentId()) + ".xml";
        } else {
            // Datafile-level citation:
            fileNameString = "attachment;filename=" + getFileNameFromPid(citation.getPersistentId()) + "-" + FileUtil.getCiteDataFileFilename(citation.getFileTitle(), FileUtil.FileCitationExtension.ENDNOTE);
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
        DataCitation citation = null;
        if (dataset != null) {
            citation = new DataCitation(dataset.getLatestVersion());
        } else {
            citation = new DataCitation(fileMetadata, direct);
        }

        FacesContext ctx = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) ctx.getExternalContext().getResponse();
        response.setContentType("application/download");

        String fileNameString;
        if (fileMetadata == null || fileMetadata.getLabel() == null) {
            // Dataset-level citation: 
            fileNameString = "attachment;filename=" + getFileNameFromPid(citation.getPersistentId()) + ".ris";
        } else {
            // Datafile-level citation:
            fileNameString = "attachment;filename=" + getFileNameFromPid(citation.getPersistentId()) + "-" + FileUtil.getCiteDataFileFilename(citation.getFileTitle(), FileUtil.FileCitationExtension.RIS);
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

    private String getFileNameFromPid(GlobalId id) {
        return id.asString();
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
        DataCitation citation = null;
        if (dataset != null) {
            citation = new DataCitation(dataset.getLatestVersion());
        } else {
            citation = new DataCitation(fileMetadata, direct);
        }
        //SEK 12/3/2018 changing this to open the json in a new tab. 
        FacesContext ctx = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) ctx.getExternalContext().getResponse();
        // FIXME: BibTeX isn't JSON. Firefox will try to parse it and report "SyntaxError".
        response.setContentType("application/json");

        String fileNameString;
        if (fileMetadata == null || fileMetadata.getLabel() == null) {
            // Dataset-level citation:
            fileNameString = "inline;filename=" + getFileNameFromPid(citation.getPersistentId()) + ".bib";
        } else {
            // Datafile-level citation:
            fileNameString = "inline;filename=" + getFileNameFromPid(citation.getPersistentId()) + "-" + FileUtil.getCiteDataFileFilename(citation.getFileTitle(), FileUtil.FileCitationExtension.BIBTEX);
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


}