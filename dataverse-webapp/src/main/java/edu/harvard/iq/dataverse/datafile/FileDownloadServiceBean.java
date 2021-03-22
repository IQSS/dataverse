package edu.harvard.iq.dataverse.datafile;

import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.citation.Citation;
import edu.harvard.iq.dataverse.citation.CitationData;
import edu.harvard.iq.dataverse.citation.CitationFactory;
import edu.harvard.iq.dataverse.datasetutility.WorldMapPermissionHelper;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.CreateGuestbookResponseCommand;
import edu.harvard.iq.dataverse.externaltools.ExternalToolHandler;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.ExternalTool;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.guestbook.GuestbookResponse;
import edu.harvard.iq.dataverse.persistence.user.ApiToken;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.User;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.FileUtil.ApiBatchDownloadType;
import edu.harvard.iq.dataverse.util.FileUtil.ApiDownloadType;
import edu.harvard.iq.dataverse.util.FileUtil.FileCitationExtension;
import org.apache.commons.io.IOUtils;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author skraffmi
 * Handles All File Download processes
 * including Guestbook responses
 */
@Stateless
@Named("fileDownloadService")
public class FileDownloadServiceBean implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(FileDownloadServiceBean.class.getCanonicalName());

    @EJB
    private DataFileServiceBean datafileService;
    @EJB
    private AuthenticationServiceBean authService;
    @EJB
    private ExternalToolHandler externalToolHandler;
    @Inject
    private DataverseSession session;
    @EJB
    private EjbDataverseEngine commandEngine;
    @Inject
    private DataverseRequestServiceBean dvRequestService;
    @Inject
    private WorldMapPermissionHelper worldMapPermissionHelper;
    @Inject
    private CitationFactory citationFactory;

    // -------------------- LOGIC --------------------

    public void writeGuestbookResponseRecord(GuestbookResponse guestbookResponse) {
        try {
            CreateGuestbookResponseCommand cmd = new CreateGuestbookResponseCommand(dvRequestService.getDataverseRequest(), guestbookResponse, guestbookResponse.getDataset());
            commandEngine.submit(cmd);
        } catch (CommandException e) {
            // if an error occurs here then download won't happen no need for response recs...
            logger.log(Level.WARNING, e, () -> "Command exception during creating guestbook response");
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
        }    }


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
        try {
            FacesContext.getCurrentInstance().getExternalContext().redirect(toolUrl);
        } catch (IOException ex) {
            logger.log(Level.INFO, "Failed to issue a redirect to external tool.", ex);
        }
    }

    public String startWorldMapDownloadLink(FileMetadata fileMetadata) {

        DataFile file = fileMetadata.getDataFile();
        String retVal = worldMapPermissionHelper.getMapLayerMetadata(file).getLayerLink();

        try {
            FacesContext.getCurrentInstance().getExternalContext().redirect(retVal);
            return retVal;
        } catch (IOException ex) {
            logger.log(Level.INFO, "Failed to issue a redirect to file download url.", ex);
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
        downloadCitation(fileMetadata, dataset, direct, "attachment",
                Citation::toEndNoteString,
                (f, c) -> createFileNameString(f, c, "attachment", FileCitationExtension.ENDNOTE));
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
        downloadCitation(fileMetadata, dataset, direct, "application/download",
                Citation::toRISString,
                (f, c) -> createFileNameString(f, c, "attachment", FileCitationExtension.RIS));
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
        downloadCitation(fileMetadata, dataset, direct,
                "application/json", // FIXME: BibTeX isn't JSON. Firefox will try to parse it and report "SyntaxError".
                Citation::toBibtexString,
                (f, c) -> createFileNameString(f, c, "inline", FileCitationExtension.BIBTEX));
    }

    // -------------------- PRIVATE --------------------

    private void downloadCitation(FileMetadata fileMetadata, Dataset dataset, boolean direct,
                                  String contentType,
                                  Function<Citation, String> citationCreator,
                                  BiFunction<FileMetadata, Citation, String> fileNameCreator) {
        Citation citation = createCitation(fileMetadata, dataset, direct);
        FacesContext facesContext = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();
        response.setContentType(contentType);
        response.setHeader("Content-Disposition", fileNameCreator.apply(fileMetadata, citation));

        try {
            ServletOutputStream outputStream = response.getOutputStream();
            String citationText = citationCreator.apply(citation);
            IOUtils.write(citationText, outputStream, StandardCharsets.UTF_8);
            outputStream.flush();
            facesContext.responseComplete();
        } catch (IOException ioe) {
            logger.log(Level.WARNING, ioe, () -> "Error while writing response");
        }
    }

    private Citation createCitation(FileMetadata fileMetadata, Dataset dataset, boolean direct) {
        return dataset != null
                ? citationFactory.create(dataset.getLatestVersion())
                : citationFactory.create(fileMetadata, direct);
    }

    private String createFileNameString(FileMetadata fileMetadata, Citation citation, String type,
                                        FileCitationExtension extension) {
        CitationData citationData = citation.getCitationData();
        String nameEnd = (fileMetadata == null || fileMetadata.getLabel() == null)
                ? extension.getExtension() // Dataset-level citation
                : FileUtil.getCiteDataFileFilename(citationData.getFileTitle(), extension); // Datafile-level citation
        return type + ";" + "filename=" + citationData.getPersistentId().asString() + nameEnd;
    }

    // -------------------- INNER CLASSES --------------------

    private interface ThrowingBiConsumer<T, U, X extends Throwable> {
        void accept(T t, U u) throws X;
    }
}