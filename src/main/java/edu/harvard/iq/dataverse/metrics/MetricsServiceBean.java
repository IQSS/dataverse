package edu.harvard.iq.dataverse.metrics;

import java.io.Serializable;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

@Stateless
public class MetricsServiceBean implements Serializable {

    private static final Logger logger = Logger.getLogger(MetricsServiceBean.class.getCanonicalName());

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    // Adapted from https://github.com/IQSS/miniverse/blob/195aae8cfd430e2fbfa865cdc6af04c261d598b0/dv_apps/metrics/stats_util_dataverses.py#L274
    public JsonArrayBuilder dataversesByCategory() {
        JsonArrayBuilder jab = Json.createArrayBuilder();
        long totalDataversesLong = (long) em.createNativeQuery("SELECT COUNT(*) FROM dataverse JOIN dvobject on dvobject.id = dataverse.id WHERE dvobject.publicationdate IS NOT NULL;").getSingleResult();
        Query query = em.createNativeQuery("SELECT COUNT(dataversetype), dataversetype FROM dataverse JOIN dvobject on dvobject.id = dataverse.id WHERE dvobject.publicationdate IS NOT NULL GROUP BY dataversetype;");
        List<Object[]> listOfObjectArrays = query.getResultList();
        for (Object[] arrayOfObjects : listOfObjectArrays) {
            JsonObjectBuilder job = Json.createObjectBuilder();
            long categoryCount = (long) arrayOfObjects[0];
            String categoryName = (String) arrayOfObjects[1];
            String percentage = String.format("%.1f", categoryCount * 100f / totalDataversesLong) + "%";
            job.add("dataverse count", categoryCount);
            job.add("name", categoryName + " (" + percentage + ")");
            job.add("percent_label", percentage);
            jab.add(job);
        }
        return jab;
    }

    public JsonArrayBuilder downloadsByMonth() {
        // FIXME: We limit to 13 because the first row is a total but not the running total we need. Get the real running total.
        Query query = em.createNativeQuery(""
                + "SELECT date_trunc('month', responsetime), count(id)\n"
                + "FROM guestbookresponse\n"
                + "GROUP BY date_trunc('month', responsetime)\n"
                + "ORDER BY date_trunc('month', responsetime) DESC\n"
                + "LIMIT 13;"
        );
        logger.fine("query: " + query);
        List<Object[]> listOfObjectArrays = query.getResultList();
        return MetricsUtil.downloadsToJson(listOfObjectArrays);
    }

    public JsonArrayBuilder datasetsByMonth() {
        // Note that "dvobject.publicationdate IS NOT NULL" isn't having any effect.
        Query query = em.createNativeQuery(""
                + "SELECT date_trunc('month', dvobject.createdate), count(dvobject.id)\n"
                + "FROM dataset\n"
                + "JOIN dvobject ON dvobject.id = dataset.id\n"
                + "WHERE dvobject.publicationdate IS NOT NULL\n"
                + "AND dataset.harvestingclient_id IS NULL\n"
                + "GROUP BY date_trunc('month', dvobject.createdate)\n"
                + "ORDER BY date_trunc('month', dvobject.createdate) DESC\n"
                + "LIMIT 12;");
        logger.fine("query: " + query);
        List<Object[]> listOfObjectArrays = query.getResultList();
        return MetricsUtil.datasetsByMonthToJson(listOfObjectArrays);
    }

}
