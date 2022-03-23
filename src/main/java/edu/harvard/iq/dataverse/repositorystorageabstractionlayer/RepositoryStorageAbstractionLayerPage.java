package edu.harvard.iq.dataverse.repositorystorageabstractionlayer;

import edu.harvard.iq.dataverse.locality.StorageSiteServiceBean;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.locality.StorageSite;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import java.util.List;
import java.util.logging.Logger;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Named;
import jakarta.json.JsonArray;

@Stateless
@Named
public class RepositoryStorageAbstractionLayerPage {

    private static final Logger logger = Logger.getLogger(RepositoryStorageAbstractionLayerPage.class.getCanonicalName());

    @EJB
    SettingsServiceBean settingsService;
    @EJB
    StorageSiteServiceBean storageSiteServiceBean;

    public String getLocalDataAccessDirectory(DatasetVersion datasetVersion) {
        String localDataAccessParentDir = settingsService.getValueForKey(SettingsServiceBean.Key.LocalDataAccessPath);
        return RepositoryStorageAbstractionLayerUtil.getLocalDataAccessDirectory(localDataAccessParentDir, datasetVersion.getDataset());
    }

    public List<RsyncSite> getRsyncSites(DatasetVersion datasetVersion) {
        List<StorageSite> storageSites = storageSiteServiceBean.findAll();
        JsonArray storageSitesAsJson = RepositoryStorageAbstractionLayerUtil.getStorageSitesAsJson(storageSites);
        return RepositoryStorageAbstractionLayerUtil.getRsyncSites(datasetVersion.getDataset(), storageSitesAsJson);
    }

    public String getVerifyDataCommand(DatasetVersion datasetVersion) {
        return RepositoryStorageAbstractionLayerUtil.getVerifyDataCommand(datasetVersion.getDataset());
    }

}
