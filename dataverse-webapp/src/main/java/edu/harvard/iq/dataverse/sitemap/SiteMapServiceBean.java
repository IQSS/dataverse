package edu.harvard.iq.dataverse.sitemap;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.List;

@Stateless
public class SiteMapServiceBean {

    @Inject
    private SystemConfig systemConfig;

    @Asynchronous
    public void updateSiteMap(List<Dataverse> dataverses, List<Dataset> datasets) {
        SiteMapUtil.updateSiteMap(dataverses, datasets, systemConfig.getDataverseSiteUrl());
    }

}
