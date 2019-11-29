package edu.harvard.iq.dataverse.dataset;

import edu.harvard.iq.dataverse.DatasetDao;
import edu.harvard.iq.dataverse.DatasetPage;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.api.AbstractApiBean;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.engine.command.exception.NotAuthenticatedException;
import edu.harvard.iq.dataverse.engine.command.impl.CreateNewDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetThumbnailCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetVersionCommand;
import edu.harvard.iq.dataverse.ingest.IngestServiceBean;
import edu.harvard.iq.dataverse.notification.NotificationObjectType;
import edu.harvard.iq.dataverse.notification.UserNotificationService;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.Template;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.NotificationType;
import edu.harvard.iq.dataverse.provenance.ProvPopupFragmentBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class DatasetService {

    private static final Logger logger = Logger.getLogger(DatasetPage.class.getCanonicalName());


    private EjbDataverseEngine commandEngine;
    private UserNotificationService userNotificationService;
    private DatasetDao datasetDao;
    private DataverseSession session;
    private DataverseRequestServiceBean dvRequestService;
    private IngestServiceBean ingestService;
    private SettingsServiceBean settingsService;
    private ProvPopupFragmentBean provPopupFragmentBean;


    // -------------------- CONSTRUCTORS --------------------

    @Deprecated
    public DatasetService() {
    }

    @Inject
    public DatasetService(EjbDataverseEngine commandEngine, UserNotificationService userNotificationService,
                          DatasetDao datasetDao, DataverseSession session, DataverseRequestServiceBean dvRequestService,
                          IngestServiceBean ingestService, SettingsServiceBean settingsService,
                          ProvPopupFragmentBean provPopupFragmentBean) {
        this.commandEngine = commandEngine;
        this.userNotificationService = userNotificationService;
        this.datasetDao = datasetDao;
        this.session = session;
        this.dvRequestService = dvRequestService;
        this.ingestService = ingestService;
        this.settingsService = settingsService;
        this.provPopupFragmentBean = provPopupFragmentBean;
    }


    // -------------------- LOGIC --------------------

    public Dataset createDataset(Dataset dataset, Template usedTemplate) {

        AuthenticatedUser user = retrieveAuthenticatedUser();

        CreateNewDatasetCommand createCommand = new CreateNewDatasetCommand(dataset, dvRequestService.getDataverseRequest(), false, usedTemplate);
        dataset = commandEngine.submit(createCommand);


        userNotificationService.sendNotificationWithEmail(user, dataset.getCreateDate(), NotificationType.CREATEDS,
                                                          dataset.getLatestVersion().getId(), NotificationObjectType.DATASET_VERSION);

        return dataset;
    }

    public AddFilesResult addFilesToDataset(long datasetId, List<DataFile> newFiles) {

        Dataset dataset = datasetDao.find(datasetId);
        AuthenticatedUser user = retrieveAuthenticatedUser();

        if (settingsService.isTrueForKey(SettingsServiceBean.Key.ProvCollectionEnabled)) {
            provPopupFragmentBean.saveStageProvFreeformToLatestVersion();
        }

        List<DataFile> savedFiles = ingestService.saveAndAddFilesToDataset(dataset.getEditVersion(), newFiles, new DataAccess());

        int notSavedFilesCount = newFiles.size() - savedFiles.size();

        if (savedFiles.size() == 0) {
            return new AddFilesResult(dataset, notSavedFilesCount, false);
        }

        dataset = commandEngine.submit(new UpdateDatasetVersionCommand(dataset, dvRequestService.getDataverseRequest()));


        // Call Ingest Service one more time, to
        // queue the data ingest jobs for asynchronous execution: 
        ingestService.startIngestJobsForDataset(dataset, user);

        //After dataset saved, then persist prov json data
        boolean hasProvenanceErrors = false;

        if (settingsService.isTrueForKey(SettingsServiceBean.Key.ProvCollectionEnabled)) {
            try {
                provPopupFragmentBean.saveStagedProvJson(false, dataset.getLatestVersion().getFileMetadatas());
            } catch (AbstractApiBean.WrappedResponse ex) {
                logger.log(Level.SEVERE, null, ex);
                hasProvenanceErrors = true;
            }
        }

        return new AddFilesResult(dataset, notSavedFilesCount, hasProvenanceErrors);
    }

    /**
     * Replaces thumbnail (default if none is set) with the one provided.
     *
     * @param datasetForNewThumbnail dataset that will have new thumbnail
     * @param thumbnailFile          thumbnail that will be set for dataset
     */
    public DatasetThumbnail changeDatasetThumbnail(Dataset datasetForNewThumbnail, DataFile thumbnailFile) {
        return commandEngine.submit(new UpdateDatasetThumbnailCommand(dvRequestService.getDataverseRequest(),
                                                                      datasetForNewThumbnail,
                                                                      UpdateDatasetThumbnailCommand.UserIntent.setDatasetFileAsThumbnail,
                                                                      thumbnailFile.getId(),
                                                                      null));

    }

    /**
     * Replaces thumbnail (default if none is set) with the one provided.
     *
     * @param datasetForNewThumbnail dataset that will have new thumbnail
     * @param fileStream             thumbnail that will be set for dataset
     */
    public DatasetThumbnail changeDatasetThumbnail(Dataset datasetForNewThumbnail, InputStream fileStream) {
        return commandEngine.submit(new UpdateDatasetThumbnailCommand(dvRequestService.getDataverseRequest(),
                                                                      datasetForNewThumbnail,
                                                                      UpdateDatasetThumbnailCommand.UserIntent.setNonDatasetFileAsThumbnail,
                                                                      null,
                                                                      fileStream));

    }

    public DatasetThumbnail removeDatasetThumbnail(Dataset datasetWithThumbnail) {
        return commandEngine.submit(new UpdateDatasetThumbnailCommand(dvRequestService.getDataverseRequest(),
                                                                      datasetWithThumbnail,
                                                                      UpdateDatasetThumbnailCommand.UserIntent.removeThumbnail,
                                                                      null,
                                                                      null));

    }

    // -------------------- PRIVATE --------------------

    private AuthenticatedUser retrieveAuthenticatedUser() {
        if (!session.getUser().isAuthenticated()) {
            throw new NotAuthenticatedException();
        }
        return (AuthenticatedUser) session.getUser();
    }
}
