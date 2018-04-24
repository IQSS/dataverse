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

    public JsonArrayBuilder dataversesByMonth() {
        // TODO: Consider switching to a "to_char" version, a String instead of a Timestamp. See datasetsByMonth.
        Query query = em.createNativeQuery(""
                + "select date_trunc('month', dvobject.publicationdate) as months, count(dvobject.id) AS new_dataverses,\n"
                + "sum(count(dvobject.id)) over (order by date_trunc('month', dvobject.publicationdate)) as cumulative\n"
                + "from dvobject\n"
                + "join dataverse on dataverse.id = dvobject.id\n"
                + "where dtype = 'Dataverse'\n"
                + "and publicationdate is not null\n"
                + "group by date_trunc('month', dvobject.publicationdate)\n"
                + "order by date_trunc('month', dvobject.publicationdate) desc\n"
                + "limit 12;"
        );
        logger.fine("query: " + query);
        List<Object[]> listOfObjectArrays = query.getResultList();
        return MetricsUtil.dataversesByMonthToJson(listOfObjectArrays);
    }

    public JsonArrayBuilder datasetsByMonth() {
        // TODO: Consider switching to the "to_char" version, a String instead of a Timestamp.
        Query query = em.createNativeQuery(""
                //                + "select to_char(date_trunc('month', dvobject.createdate), 'Mon YYYY') as months, count(dvobject.id) AS new_datasets,\n"
                + "select date_trunc('month', dvobject.publicationdate) as months, count(dvobject.id) AS new_datasets,\n"
                + "sum(count(dvobject.id)) over (order by date_trunc('month', dvobject.publicationdate)) as cumulative\n"
                + "from dvobject\n"
                + "join dataset on dataset.id = dvobject.id\n"
                + "where dtype = 'Dataset'\n"
                + "and publicationdate is not null\n"
                + "and dataset.harvestingclient_id is null\n"
                + "group by date_trunc('month', dvobject.publicationdate)\n"
                + "order by date_trunc('month', dvobject.publicationdate) desc\n"
                + "limit 12;"
        );
        logger.fine("query: " + query);
        List<Object[]> listOfObjectArrays = query.getResultList();
        return MetricsUtil.datasetsByMonthToJson(listOfObjectArrays);
    }

    public JsonArrayBuilder datasetsBySubject() {
        Query query = em.createNativeQuery(""
                + "SELECT strvalue, count(dataset.id)\n"
                + "FROM datasetfield_controlledvocabularyvalue \n"
                + "JOIN controlledvocabularyvalue ON controlledvocabularyvalue.id = datasetfield_controlledvocabularyvalue.controlledvocabularyvalues_id\n"
                + "JOIN datasetfield ON datasetfield.id = datasetfield_controlledvocabularyvalue.datasetfield_id\n"
                + "JOIN datasetfieldtype ON datasetfieldtype.id = controlledvocabularyvalue.datasetfieldtype_id\n"
                + "JOIN datasetversion ON datasetversion.id = datasetfield.datasetversion_id\n"
                + "JOIN dvobject ON dvobject.id = datasetversion.dataset_id\n"
                + "JOIN dataset ON dataset.id = datasetversion.dataset_id\n"
                + "WHERE\n"
                + "datasetfieldtype.name = 'subject'\n"
                + "AND dvobject.publicationdate is NOT NULL\n"
                + "AND dataset.harvestingclient_id IS NULL\n"
                + "GROUP BY strvalue\n"
                + "ORDER BY count(dataset.id) desc;"
        );
        logger.info("query: " + query);
        List<Object[]> listOfObjectArrays = query.getResultList();
        return MetricsUtil.datasetsBySubjectToJson(listOfObjectArrays);
    }

}
