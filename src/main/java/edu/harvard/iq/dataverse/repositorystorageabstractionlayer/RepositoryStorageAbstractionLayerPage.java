package edu.harvard.iq.dataverse.repositorystorageabstractionlayer;

import edu.harvard.iq.dataverse.DatasetVersion;
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

    public String getLocalDataAccessDirectory(DatasetVersion datasetVersion) {
        String localDataAccessParentDir = settingsService.getValueForKey(SettingsServiceBean.Key.LocalDataAccessPath);
        return RepositoryStorageAbstractionLayerUtil.getLocalDataAccessDirectory(localDataAccessParentDir, datasetVersion.getDataset());
    }

    public List<RsyncSite> getRsyncSites(DatasetVersion datasetVersion) {
        String replicatationSitesSetting = settingsService.getValueForKey(SettingsServiceBean.Key.ReplicationSites);
        JsonArray replicationSites = RepositoryStorageAbstractionLayerUtil.getSitesFromDb(replicatationSitesSetting);
        return RepositoryStorageAbstractionLayerUtil.getRsyncSites(datasetVersion.getDataset(), replicationSites);
    }

    public String getVerifyDataCommand(DatasetVersion datasetVersion) {
        return RepositoryStorageAbstractionLayerUtil.getVerifyDataCommand(datasetVersion.getDataset());
    }

}
