package edu.harvard.iq.dataverse.metrics;

import java.io.Serializable;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.Stateless;
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

    public JsonArrayBuilder dataversesByCategory() {
        Query query = em.createNativeQuery(""
                + "select dataversetype, count(dataversetype) from dataverse\n"
                + "join dvobject on dvobject.id = dataverse.id\n"
                + "where dvobject.publicationdate is not null\n"
                + "group by dataversetype\n"
                + "order by count desc;"
        );
        List<Object[]> listOfObjectArrays = query.getResultList();
        return MetricsUtil.dataversesByCategoryToJson(listOfObjectArrays);
    }

    public JsonArrayBuilder downloadsByMonth() {
        Query query = em.createNativeQuery(""
                + "select date_trunc('month', guestbookresponse.responsetime) as months, count(guestbookresponse.id) AS downloads,\n"
                + "sum(count(guestbookresponse.id)) over (order by date_trunc('month', guestbookresponse.responsetime)) as cumulative\n"
                + "from guestbookresponse\n"
                + "join dvobject on dvobject.id = guestbookresponse.datafile_id\n"
                + "where dvobject.publicationdate is not null\n"
                + "and guestbookresponse.responsetime is not null\n"
                + "group by date_trunc('month', responsetime)\n"
                + "order by date_trunc('month', responsetime) desc\n"
                + "limit 12;"
        );
        logger.fine("query: " + query);
        List<Object[]> listOfObjectArrays = query.getResultList();
        return MetricsUtil.downloadsToJson(listOfObjectArrays);
    }

    public JsonArrayBuilder filesByMonth() {
        // TODO: Consider switching to a "to_char" version, a String instead of a Timestamp. See datasetsByMonth.
        Query query = em.createNativeQuery(""
                + "select date_trunc('month', dvobject.publicationdate) as months, count(dvobject.id) as new_files,\n"
                + "sum(count(dvobject.id)) over (order by date_trunc('month', dvobject.publicationdate)) as cumulative\n"
                + "from dvobject\n"
                + "join datafile on datafile.id = dvobject.id\n"
                + "join dataset on dataset.id = dvobject.owner_id\n"
                + "where dtype = 'DataFile'\n"
                + "and publicationdate is not null\n"
                + "and dataset.harvestingclient_id is null\n"
                + "group by date_trunc('month', dvobject.publicationdate)\n"
                + "order by date_trunc('month', dvobject.publicationdate) desc\n"
                + "limit 12;"
        );
        logger.fine("query: " + query);
        List<Object[]> listOfObjectArrays = query.getResultList();
        return MetricsUtil.filesByMonthToJson(listOfObjectArrays);
    }

    /**
     * A count of published files in the system now intended to match the file
     * count reported by Solr on the homepage.
     */
    public JsonObjectBuilder filesNow() {
        Query query = em.createNativeQuery(""
                + "select count(filemetadata.datafile_id)\n"
                + "from filemetadata\n"
                + "join datasetversion on datasetversion.id = filemetadata.datasetversion_id\n"
                + "where concat(datasetversion.dataset_id,':', datasetversion.versionnumber + (.1 * datasetversion.minorversionnumber)) in\n"
                + "(\n"
                + "select concat(datasetversion.dataset_id,':', max(datasetversion.versionnumber + (.1 * datasetversion.minorversionnumber))) as max\n"
                + "from datasetversion\n"
                + "join dataset on dataset.id = datasetversion.dataset_id\n"
                + "where versionstate='RELEASED'\n"
                + "and dataset.harvestingclient_id is null\n"
                + "group by dataset_id\n"
                + ");"
        );
        logger.fine("query: " + query);
        long count = (long) query.getSingleResult();
        return MetricsUtil.filesNowToJson(count);
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
