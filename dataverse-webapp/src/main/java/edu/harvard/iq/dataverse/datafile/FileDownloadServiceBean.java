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
import edu.harvard.iq.dataverse.persistence.GlobalId;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.ExternalTool;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.guestbook.GuestbookResponse;
import edu.harvard.iq.dataverse.persistence.user.ApiToken;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.User;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.FileUtil.FileCitationExtension;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
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

    /**
     * Launch an "explore" tool which is a type of ExternalTool such as
     * TwoRavens or Data Explorer. This method may be invoked directly from the
     * xhtml if no popup is required (no terms of use, no guestbook, etc.).
     */
    public void explore(DataFile dataFile, ExternalTool externalTool) {
        ApiToken apiToken = null;
        User user = session.getUser();
        if (user instanceof AuthenticatedUser) {
            AuthenticatedUser authenticatedUser = (AuthenticatedUser) user;
            apiToken = authService.findApiTokenByUser(authenticatedUser);
        }
        //For tools to get the dataset and datasetversion ids, we need a full DataFile object (not a findCheapAndEasy() copy)
        if (dataFile.getFileMetadata() == null) {
            dataFile = datafileService.find(dataFile.getId());
        }
        // Back when we only had TwoRavens, the downloadType was always "Explore".
        // Now we persist the name of the tool (i.e. "TwoRavens", "Data Explorer", etc.)
        String toolUrl = externalToolHandler.buildToolUrlWithQueryParams(externalTool, dataFile, apiToken, session.getLocaleCode());
        logger.finest(() -> "Exploring with " + toolUrl);
        try {
            FacesContext.getCurrentInstance().getExternalContext().redirect(toolUrl);
        } catch (IOException ex) {
            logger.log(Level.INFO, "Failed to issue a redirect to external tool.", ex);
        }
    }

    public String startWorldMapDownloadLink(DataFile file) {

        String retVal = worldMapPermissionHelper.getMapLayerMetadata(file).getLayerLink();

        try {
            FacesContext.getCurrentInstance().getExternalContext().redirect(retVal);
            return retVal;
        } catch (IOException ex) {
            logger.log(Level.INFO, "Failed to issue a redirect to file download url.", ex);
        }
        return retVal;
    }

    public void downloadDatasetCitationXML(DatasetVersion datasetVersion) {
        downloadCitationXML(null, datasetVersion, false);
    }

    public void downloadDatafileCitationXML(FileMetadata fileMetadata) {
        downloadCitationXML(fileMetadata, null, false);
    }

    public void downloadDirectDatafileCitationXML(FileMetadata fileMetadata) {
        downloadCitationXML(fileMetadata, null, true);
    }

    public void downloadCitationXML(FileMetadata fileMetadata, DatasetVersion datasetVersion, boolean direct) {
        downloadCitation(fileMetadata, datasetVersion, direct, "attachment",
                Citation::toEndNoteString,
                (f, c) -> createFileNameString(f, c, "attachment", FileCitationExtension.ENDNOTE));
    }

    public void downloadDatasetCitationRIS(DatasetVersion datasetVersion) {
        downloadCitationRIS(null, datasetVersion, false);
    }

    public void downloadDatafileCitationRIS(FileMetadata fileMetadata) {
        downloadCitationRIS(fileMetadata, null, false);
    }

    public void downloadDirectDatafileCitationRIS(FileMetadata fileMetadata) {
        downloadCitationRIS(fileMetadata, null, true);
    }

    public void downloadCitationRIS(FileMetadata fileMetadata, DatasetVersion datasetVersion, boolean direct) {
        downloadCitation(fileMetadata, datasetVersion, direct, "application/download",
                Citation::toRISString,
                (f, c) -> createFileNameString(f, c, "attachment", FileCitationExtension.RIS));
    }

    public void downloadDatasetCitationBibtex(DatasetVersion datasetVersion) {
        downloadCitationBibtex(null, datasetVersion, false);
    }

    public void downloadDatafileCitationBibtex(FileMetadata fileMetadata) {
        downloadCitationBibtex(fileMetadata, null, false);
    }

    public void downloadDirectDatafileCitationBibtex(FileMetadata fileMetadata) {
        downloadCitationBibtex(fileMetadata, null, true);
    }

    public void downloadCitationBibtex(FileMetadata fileMetadata, DatasetVersion datasetVersion, boolean direct) {
        downloadCitation(fileMetadata, datasetVersion, direct,
                "text/plain; charset=UTF-8", //BibTeX isn't JSON. Firefox will fail to parse it if set to json content type.
                Citation::toBibtexString,
                (f, c) -> createFileNameString(f, c, "inline", FileCitationExtension.BIBTEX));
    }

    // -------------------- PRIVATE --------------------

    private void downloadCitation(FileMetadata fileMetadata, DatasetVersion datasetVersion, boolean direct,
                                  String contentType,
                                  Function<Citation, String> citationCreator,
                                  BiFunction<FileMetadata, Citation, String> fileNameCreator) {
        Citation citation = createCitation(fileMetadata, datasetVersion, direct);
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

    private Citation createCitation(FileMetadata fileMetadata, DatasetVersion datasetVersion, boolean direct) {
        return datasetVersion != null
                ? citationFactory.create(datasetVersion)
                : citationFactory.create(fileMetadata, direct);
    }

    private String createFileNameString(FileMetadata fileMetadata, Citation citation, String type,
                                        FileCitationExtension extension) {
        CitationData citationData = citation.getCitationData();
        String nameEnd = (fileMetadata == null || fileMetadata.getLabel() == null)
                ? extension.getExtension() // Dataset-level citation
                : FileUtil.getCiteDataFileFilename(citationData.getFileTitle(), extension); // Datafile-level citation
        String pid = Optional.ofNullable(citationData.getPersistentId())
                .map(GlobalId::asString)
                .orElse(StringUtils.EMPTY);
        return type + ";" + "filename=" + pid + nameEnd;
    }
}
