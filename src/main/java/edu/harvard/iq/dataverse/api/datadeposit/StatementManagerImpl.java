package edu.harvard.iq.dataverse.api.datadeposit;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
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

        DataverseUser vdcUser = swordAuth.auth(authCredentials);
        urlManager.processUrl(editUri);
        String globalId = urlManager.getTargetIdentifier();
        if (urlManager.getTargetType().equals("study") && globalId != null) {

            logger.fine("request for sword statement by user " + vdcUser.getUserName());

//            Study study = null;
            /**
             * @todo don't hard code this, obviously. In DVN 3.x we had a method
             * for editStudyService.getStudyByGlobalId(globalId)
             */
//            Study study = editStudyService.getStudyByGlobalId(globalId);
            long databaseIdForRoastingAtHomeDataset = 10;
            Dataset study = datasetService.find(databaseIdForRoastingAtHomeDataset);
//            try {
//                study = studyService.getStudyByGlobalId(globalId);
//            } catch (EJBException ex) {
//                throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Could not find study based on global id (" + globalId + ") in URL: " + editUri);
//            }
            Long studyId;
            try {
                studyId = study.getId();
            } catch (NullPointerException ex) {
                throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "couldn't find study with global ID of " + globalId);
            }

            Dataverse dvThatOwnsStudy = study.getOwner();
            if (swordAuth.hasAccessToModifyDataverse(vdcUser, dvThatOwnsStudy)) {
                /**
                 * @todo getIdentifier is equivalent to getGlobalId, right?
                 */
//                String feedUri = urlManager.getHostnamePlusBaseUrlPath(editUri) + "/edit/study/" + study.getGlobalId();
                String feedUri = urlManager.getHostnamePlusBaseUrlPath(editUri) + "/edit/study/" + study.getIdentifier();
                /**
                 * @todo is it safe to use this?
                 */
                String author = study.getLatestVersion().getAuthorsStr();
                String title = study.getLatestVersion().getTitle();
                Date lastUpdated = study.getLatestVersion().getLastUpdateTime();
                if (lastUpdated == null) {
                    /**
                     * @todo why is this date null?
                     */
                    logger.info("why is lastUpdated null?");
                    lastUpdated = new Date();
                }
                AtomDate atomDate = new AtomDate(lastUpdated);
//                AtomDate atomDate = new AtomDate(study.getLatestVersion().getLastUpdateTime());
                String datedUpdated = atomDate.toString();
                Statement statement = new AtomStatement(feedUri, author, title, datedUpdated);
                Map<String, String> states = new HashMap<String, String>();
                states.put("latestVersionState", study.getLatestVersion().getVersionState().toString());
                /**
                 * @todo DVN 3.x had a studyLock. What's the equivalent in 4.0?
                 */
//                StudyLock lock = study.getStudyLock();
//                if (lock != null) {
//                    states.put("locked", "true");
//                    states.put("lockedDetail", lock.getDetail());
//                    states.put("lockedStartTime", lock.getStartTime().toString());
//                } else {
//                    states.put("locked", "false");
//                }
                statement.setStates(states);
                List<FileMetadata> fileMetadatas = study.getLatestVersion().getFileMetadatas();
                for (FileMetadata fileMetadata : fileMetadatas) {
                    DataFile studyFile = fileMetadata.getDataFile();
                    // We are exposing the filename for informational purposes. The file id is what you
                    // actually operate on to delete a file, etc.
                    //
                    // Replace spaces to avoid IRISyntaxException
                    String fileNameFinal = fileMetadata.getLabel().replace(' ', '_');
                    String studyFileUrlString = urlManager.getHostnamePlusBaseUrlPath(editUri) + "/edit-media/file/" + studyFile.getId() + "/" + fileNameFinal;
                    IRI studyFileUrl;
                    try {
                        studyFileUrl = new IRI(studyFileUrlString);
                    } catch (IRISyntaxException ex) {
                        throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Invalid URL for file ( " + studyFileUrlString + " ) resulted in " + ex.getMessage());
                    }
                    ResourcePart resourcePart = new ResourcePart(studyFileUrl.toString());
                    /**
                     * @todo get this working. show the actual file type
                     */
//                    resourcePart.setMediaType(studyFile.getOriginalFileFormat());
                    resourcePart.setMediaType("application/octet-stream");
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
                throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "user " + vdcUser.getUserName() + " is not authorized to view study with global ID " + globalId);
            }
        } else {
            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Could not determine target type or identifier from URL: " + editUri);
        }
    }

}
