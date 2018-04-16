package edu.harvard.iq.dataverse.metrics;

import java.io.Serializable;
import java.util.List;
import javax.ejb.Stateless;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

@Stateless
public class MetricsServiceBean implements Serializable {

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

}
