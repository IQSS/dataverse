package edu.harvard.iq.dataverse.metrics;

import edu.harvard.iq.dataverse.Metric;
import java.io.Serializable;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

@Stateless
public class MetricsServiceBean implements Serializable {

    private static final Logger logger = Logger.getLogger(MetricsServiceBean.class.getCanonicalName());

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    
    public Metric save(Metric metric) {
        em.persist(metric);
        return em.merge(metric);
    }
    
    /**
     * @param yyyymm Month in YYYY-MM format.
     */
    public long dataversesByMonth(String yyyymm) throws Exception {
        String sanitizedyyyymm = MetricsUtil.sanitizeYearMonthUserInput(yyyymm);
        Query query = em.createNativeQuery(""
                + "select count(dvobject.id)\n"
                + "from dataverse\n"
                + "join dvobject on dvobject.id = dataverse.id\n"
                + "where dvobject.publicationdate is not null\n"
                + "and date_trunc('month', publicationdate) <=  to_date('" + sanitizedyyyymm + "','YYYY-MM');"
        );
        logger.fine("query: " + query);
        //save(new Metric("test",01,01,99999));
        return (long) query.getSingleResult();
    }

    /**
     * @param yyyymm Month in YYYY-MM format.
     */
    public long datasetsByMonth(String yyyymm) throws Exception {
        String sanitizedyyyymm = MetricsUtil.sanitizeYearMonthUserInput(yyyymm);
        Query query = em.createNativeQuery(""
                + "select count(*)\n"
                + "from datasetversion\n"
                + "where concat(datasetversion.dataset_id,':', datasetversion.versionnumber + (.1 * datasetversion.minorversionnumber)) in \n"
                + "(\n"
                + "select concat(datasetversion.dataset_id,':', max(datasetversion.versionnumber + (.1 * datasetversion.minorversionnumber))) as max \n"
                + "from datasetversion\n"
                + "join dataset on dataset.id = datasetversion.dataset_id\n"
                + "where versionstate='RELEASED' \n"
                + "and date_trunc('month', releasetime) <=  to_date('" + sanitizedyyyymm + "','YYYY-MM')\n"
                + "and dataset.harvestingclient_id is null\n"
                + "group by dataset_id \n"
                + ");"
        );
        logger.fine("query: " + query);
        return (long) query.getSingleResult();
    }

    /**
     * @param yyyymm Month in YYYY-MM format.
     */
    public long filesByMonth(String yyyymm) throws Exception {
        String sanitizedyyyymm = MetricsUtil.sanitizeYearMonthUserInput(yyyymm);
        Query query = em.createNativeQuery(""
                + "select count(*)\n"
                + "from filemetadata\n"
                + "join datasetversion on datasetversion.id = filemetadata.datasetversion_id\n"
                + "where concat(datasetversion.dataset_id,':', datasetversion.versionnumber + (.1 * datasetversion.minorversionnumber)) in \n"
                + "(\n"
                + "select concat(datasetversion.dataset_id,':', max(datasetversion.versionnumber + (.1 * datasetversion.minorversionnumber))) as max \n"
                + "from datasetversion\n"
                + "join dataset on dataset.id = datasetversion.dataset_id\n"
                + "where versionstate='RELEASED'\n"
                //                + "and date_trunc('month', releasetime) <=  to_date('2018-03','YYYY-MM')\n"
                // FIXME: Remove SQL injection vector: https://software-security.sans.org/developer-how-to/fix-sql-injection-in-java-persistence-api-jpa
                + "and date_trunc('month', releasetime) <=  to_date('" + sanitizedyyyymm + "','YYYY-MM')\n"
                + "and dataset.harvestingclient_id is null\n"
                + "group by dataset_id \n"
                + ");"
        );
        logger.fine("query: " + query);
        return (long) query.getSingleResult();
    }

    /**
     * @param yyyymm Month in YYYY-MM format.
     */
    public long downloadsByMonth(String yyyymm) throws Exception {
        String sanitizedyyyymm = MetricsUtil.sanitizeYearMonthUserInput(yyyymm);
        Query query = em.createNativeQuery(""
                + "select count(id)\n"
                + "from guestbookresponse\n"
                + "where date_trunc('month', responsetime) <=  to_date('" + sanitizedyyyymm + "','YYYY-MM');"
        );
        logger.fine("query: " + query);
        return (long) query.getSingleResult();
    }

    public List<Object[]> dataversesByCategory() {
        Query query = em.createNativeQuery(""
                + "select dataversetype, count(dataversetype) from dataverse\n"
                + "join dvobject on dvobject.id = dataverse.id\n"
                + "where dvobject.publicationdate is not null\n"
                + "group by dataversetype\n"
                + "order by count desc;"
        );
        return query.getResultList();
    }

    public List<Object[]> datasetsBySubject() {
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
        return query.getResultList();
        
    }

}
