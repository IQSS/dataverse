package edu.harvard.iq.dataverse.api.datadeposit;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetLock;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.impl.GetDraftDatasetVersionCommand;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import static java.util.stream.Collectors.joining;
import jakarta.ejb.EJB;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.i18n.iri.IRISyntaxException;
import org.apache.abdera.model.AtomDate;
import org.swordapp.server.AtomStatement;
import org.swordapp.server.AuthCredentials;
import org.swordapp.server.ResourcePart;
import org.swordapp.server.Statement;
import org.swordapp.server.StatementManager;
import org.swordapp.server.SwordAuthException;
import org.swordapp.server.SwordConfiguration;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;
import org.swordapp.server.UriRegistry;

public class StatementManagerImpl implements StatementManager {

    private static final Logger logger = Logger.getLogger(StatementManagerImpl.class.getCanonicalName());

    @EJB
    DatasetServiceBean datasetService;
    @EJB
    PermissionServiceBean permissionService;
    @Inject
    SwordAuth swordAuth;
    @Inject
    UrlManager urlManager;

    private HttpServletRequest httpRequest;

    @Override
    public Statement getStatement(String editUri, Map<String, String> map, AuthCredentials authCredentials, SwordConfiguration swordConfiguration) throws SwordServerException, SwordError, SwordAuthException {

        AuthenticatedUser user = swordAuth.auth(authCredentials);
        DataverseRequest dvReq = new DataverseRequest(user, httpRequest);
        urlManager.processUrl(editUri);
        String globalId = urlManager.getTargetIdentifier();
        if (urlManager.getTargetType().equals("study") && globalId != null) {

            logger.fine("request for sword statement by user " + user.getDisplayInfo().getTitle());
            Dataset dataset = datasetService.findByGlobalId(globalId);
            if (dataset == null) {
                throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "couldn't find dataset with global ID of " + globalId);
            }

            Dataverse dvThatOwnsDataset = dataset.getOwner();
            if (!permissionService.isUserAllowedOn(user, new GetDraftDatasetVersionCommand(dvReq, dataset), dataset)) {
                throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "user " + user.getDisplayInfo().getTitle() + " is not authorized to view dataset with global ID " + globalId);
            }
            String feedUri = urlManager.getHostnamePlusBaseUrlPath(editUri) + "/edit/study/" + dataset.getGlobalId().asString();
            String author = dataset.getLatestVersion().getAuthorsStr();
            String title = dataset.getLatestVersion().getTitle();
            // in the statement, the element is called "updated"
            Date lastUpdatedFinal = new Date();
            Date lastUpdateTime = dataset.getLatestVersion().getLastUpdateTime();
            dataset.getLatestVersion().getTermsOfUseAndAccess().setDatasetVersion(dataset.getLatestVersion());
            if (lastUpdateTime != null) {
                lastUpdatedFinal = lastUpdateTime;
            } else {
                logger.info("lastUpdateTime was null, trying createtime");
                Date createtime = dataset.getLatestVersion().getCreateTime();
                if (createtime != null) {
                    lastUpdatedFinal = createtime;
                } else {
                    logger.info("creatime was null, using \"now\"");
                    lastUpdatedFinal = new Date();
                }
            }
            AtomDate atomDate = new AtomDate(lastUpdatedFinal);
            String datedUpdated = atomDate.toString();
            Statement statement = new AtomStatement(feedUri, author, title, datedUpdated);
            Map<String, String> states = new HashMap<>();
            states.put("latestVersionState", dataset.getLatestVersion().getVersionState().toString());
            Boolean isMinorUpdate = dataset.getLatestVersion().isMinorUpdate();
            states.put("isMinorUpdate", isMinorUpdate.toString());
            
            if ( dataset.isLocked() ) {
                states.put("locked", "true");
                states.put("lockedDetail", dataset.getLocks().stream().map( l-> l.getInfo() ).collect( joining(",")) );
                Optional<DatasetLock> earliestLock = dataset.getLocks().stream().min((l1, l2) -> (int)Math.signum(l1.getStartTime().getTime()-l2.getStartTime().getTime()) );
                states.put("lockedStartTime", earliestLock.get().getStartTime().toString());
            } else {
                states.put("locked", "false");
            }
            
            statement.setStates(states);
            List<FileMetadata> fileMetadatas = dataset.getLatestVersion().getFileMetadatas();
            for (FileMetadata fileMetadata : fileMetadatas) {
                DataFile dataFile = fileMetadata.getDataFile();
                // We are exposing the filename for informational purposes. The file id is what you
                // actually operate on to delete a file, etc.
                //
                // Replace spaces to avoid IRISyntaxException
                String fileNameFinal = fileMetadata.getLabel().replace(' ', '_');
                // TODO: Consider where we would show the persistent identifiers for files via SWORD.
                String fileUrlString = urlManager.getHostnamePlusBaseUrlPath(editUri) + "/edit-media/file/" + dataFile.getId() + "/" + fileNameFinal;
                IRI fileUrl;
                try {
                    fileUrl = new IRI(fileUrlString);
                } catch (IRISyntaxException ex) {
                    throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Invalid URL for file ( " + fileUrlString + " ) resulted in " + ex.getMessage());
                }
                ResourcePart resourcePart = new ResourcePart(fileUrl.toString());
                // default to something that doesn't throw a org.apache.abdera.util.MimeTypeParseException
                String finalFileFormat = "application/octet-stream";
                String contentType = dataFile.getContentType();
                if (contentType != null) {
                    finalFileFormat = contentType;
                }
                resourcePart.setMediaType(finalFileFormat);
                /**
                 * @todo: Why are properties set on a ResourcePart not exposed
                 * when you GET a Statement? Asked about this at
                 * http://www.mail-archive.com/sword-app-tech@lists.sourceforge.net/msg00394.html
                 */
//                    Map<String, String> properties = new HashMap<String, String>();
//                    properties.put("filename", studyFile.getFileName());
//                    properties.put("category", studyFile.getLatestCategory());
//                    properties.put("originalFileType", studyFile.getOriginalFileType());
//                    properties.put("id", studyFile.getId().toString());
//                    properties.put("UNF", studyFile.getUnf());
//                    resourcePart.setProperties(properties);
                statement.addResource(resourcePart);
                /**
                 * @todo it's been noted at
                 * https://github.com/IQSS/dataverse/issues/892#issuecomment-54159284
                 * that at the file level the "updated" date is always "now",
                 * which seems to be set here:
                 * https://github.com/swordapp/JavaServer2.0/blob/sword2-server-1.0/src/main/java/org/swordapp/server/AtomStatement.java#L70
                 */
            }
            return statement;
        } else {
            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Could not determine target type or identifier from URL: " + editUri);
        }
    }

    public void setHttpRequest(HttpServletRequest httpRequest) {
        this.httpRequest = httpRequest;
    }

}
