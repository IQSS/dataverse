package edu.harvard.iq.dataverse.repositorystorageabstractionlayer;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.json.JsonArray;

@Stateless
@Named
public class RepositoryStorageAbstractionLayerPage {

    private static final Logger logger = Logger.getLogger(RepositoryStorageAbstractionLayerPage.class.getCanonicalName());

    @EJB
    SettingsServiceBean settingsService;

    public String getLocalDataAccessDirectory(Dataset dataset, FileMetadata fileMetadata) {
        String localDataAccessParentDir = settingsService.getValueForKey(SettingsServiceBean.Key.LocalDataAccessPath);
        return RepositoryStorageAbstractionLayerUtil.getLocalDataAccessDirectory(localDataAccessParentDir, dataset, fileMetadata);
    }

    public List<RsyncSite> getRsyncSites(Dataset dataset, FileMetadata fileMetadata) {
        String replicatationSitesSetting = settingsService.getValueForKey(SettingsServiceBean.Key.ReplicationSites);
        JsonArray replicationSites = RepositoryStorageAbstractionLayerUtil.getSitesFromDb(replicatationSitesSetting);
        return RepositoryStorageAbstractionLayerUtil.getRsyncSites(dataset, fileMetadata, replicationSites);
    }

    public String getVerifyDataCommand(Dataset dataset, FileMetadata fileMetadata) {
        return RepositoryStorageAbstractionLayerUtil.getVerifyDataCommand(dataset, fileMetadata);
    }

}
