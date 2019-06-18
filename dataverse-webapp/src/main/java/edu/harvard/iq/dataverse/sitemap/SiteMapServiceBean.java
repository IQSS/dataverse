package edu.harvard.iq.dataverse.sitemap;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.Dataverse;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import java.util.List;

@Stateless
public class SiteMapServiceBean {

    @Asynchronous
    public void updateSiteMap(List<Dataverse> dataverses, List<Dataset> datasets) {
        SiteMapUtil.updateSiteMap(dataverses, datasets);
    }

}
