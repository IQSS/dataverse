package edu.harvard.iq.dataverse.api.datadeposit;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetLock;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseUser;
import edu.harvard.iq.dataverse.FileMetadata;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.inject.Inject;
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
    @Inject
    SwordAuth swordAuth;
    @Inject
    UrlManager urlManager;
    SwordConfigurationImpl swordConfiguration = new SwordConfigurationImpl();

    @Override
    public Statement getStatement(String editUri, Map<String, String> map, AuthCredentials authCredentials, SwordConfiguration swordConfiguration) throws SwordServerException, SwordError, SwordAuthException {
        this.swordConfiguration = (SwordConfigurationImpl) swordConfiguration;
        swordConfiguration = (SwordConfigurationImpl) swordConfiguration;
        if (authCredentials == null) {
            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "auth credentials are null");
        }
        if (swordAuth == null) {
            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "swordAuth is null");
        }

        DataverseUser dataverseUser = swordAuth.auth(authCredentials);
        urlManager.processUrl(editUri);
        String globalId = urlManager.getTargetIdentifier();
        if (urlManager.getTargetType().equals("study") && globalId != null) {

            logger.fine("request for sword statement by user " + dataverseUser.getUserName());
            Dataset dataset = datasetService.findByGlobalId(globalId);
            if (dataset == null) {
                throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "couldn't find dataset with global ID of " + globalId);
            }

            Dataverse dvThatOwnsDataset = dataset.getOwner();
            if (swordAuth.hasAccessToModifyDataverse(dataverseUser, dvThatOwnsDataset)) {
                String feedUri = urlManager.getHostnamePlusBaseUrlPath(editUri) + "/edit/study/" + dataset.getGlobalId();
                /**
                 * @todo did the format of getAuthorsStr() change? It looks more
                 * or less the same.
                 */
                String author = dataset.getLatestVersion().getAuthorsStr();
                String title = dataset.getLatestVersion().getTitle();
                // in the statement, the element is called "updated"
                Date lastUpdatedFinal = new Date();
                Date lastUpdateTime = dataset.getLatestVersion().getLastUpdateTime();
                if (lastUpdateTime != null) {
                    lastUpdatedFinal = lastUpdateTime;
                } else {
                    /**
                     * @todo In DVN 3.x lastUpdated was set on the service bean:
                     * https://github.com/IQSS/dvn/blob/8ca34aded90511730c35ca32ace844770c24c68e/DVN-root/DVN-web/src/main/java/edu/harvard/iq/dvn/core/study/StudyServiceBean.java#L1803
                     *
                     * In 4.0, lastUpdateTime is always null.
                     */
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
                Map<String, String> states = new HashMap<String, String>();
                states.put("latestVersionState", dataset.getLatestVersion().getVersionState().toString());
                DatasetLock lock = dataset.getDatasetLock();
                if (lock != null) {
                    states.put("locked", "true");
                    states.put("lockedDetail", lock.getInfo());
                    states.put("lockedStartTime", lock.getStartTime().toString());
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
                    String studyFileUrlString = urlManager.getHostnamePlusBaseUrlPath(editUri) + "/edit-media/file/" + dataFile.getId() + "/" + fileNameFinal;
                    IRI studyFileUrl;
                    try {
                        studyFileUrl = new IRI(studyFileUrlString);
                    } catch (IRISyntaxException ex) {
                        throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Invalid URL for file ( " + studyFileUrlString + " ) resulted in " + ex.getMessage());
                    }
                    ResourcePart resourcePart = new ResourcePart(studyFileUrl.toString());
                    // default to something that doesn't throw a org.apache.abdera.util.MimeTypeParseException
                    String finalFileFormat = "application/octet-stream";
                    String contentType = dataFile.getContentType();
                    if (contentType != null) {
                        finalFileFormat = contentType;
                    }
                    resourcePart.setMediaType(finalFileFormat);
                    /**
                     * @todo: Why are properties set on a ResourcePart not
                     * exposed when you GET a Statement?
                     */
//                    Map<String, String> properties = new HashMap<String, String>();
//                    properties.put("filename", studyFile.getFileName());
//                    properties.put("category", studyFile.getLatestCategory());
//                    properties.put("originalFileType", studyFile.getOriginalFileType());
//                    properties.put("id", studyFile.getId().toString());
//                    properties.put("UNF", studyFile.getUnf());
//                    resourcePart.setProperties(properties);
                    statement.addResource(resourcePart);
                }
                return statement;
            } else {
                throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "user " + dataverseUser.getUserName() + " is not authorized to view study with global ID " + globalId);
            }
        } else {
            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Could not determine target type or identifier from URL: " + editUri);
        }
    }

}
