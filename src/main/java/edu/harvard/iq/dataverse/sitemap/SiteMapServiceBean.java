package edu.harvard.iq.dataverse.sitemap;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.Dataverse;
import java.util.List;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.Stateless;

@Stateless
public class SiteMapServiceBean {

    @Asynchronous
    public void updateSiteMap(List<Dataverse> dataverses, List<Dataset> datasets) {
        SiteMapUtil.updateSiteMap(dataverses, datasets);
    }

}
