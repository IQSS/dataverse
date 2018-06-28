package edu.harvard.iq.dataverse.metrics;

import edu.harvard.iq.dataverse.Metric;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

@Stateless
public class MetricsServiceBean implements Serializable {

    private static final Logger logger = Logger.getLogger(MetricsServiceBean.class.getCanonicalName());

    private static final SimpleDateFormat yyyymmFormat = new SimpleDateFormat(MetricsUtil.YEAR_AND_MONTH_PATTERN);

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    @EJB
    SystemConfig systemConfig;

    /**
     * @param yyyymm Month in YYYY-MM format.
     */
    public long dataversesToMonth(String yyyymm) throws Exception {
        Query query = em.createNativeQuery(""
                + "select count(dvobject.id)\n"
                + "from dataverse\n"
                + "join dvobject on dvobject.id = dataverse.id\n"
                + "where dvobject.publicationdate is not null\n"
                + "and date_trunc('month', publicationdate) <=  to_date('" + yyyymm + "','YYYY-MM');"
        );
        logger.fine("query: " + query);

        return (long) query.getSingleResult();
    }

    /**
     * @param yyyymm Month in YYYY-MM format.
     */
    public long datasetsToMonth(String yyyymm) throws Exception {
        Query query = em.createNativeQuery(""
                + "select count(*)\n"
                + "from datasetversion\n"
                + "where datasetversion.dataset_id || ':' || datasetversion.versionnumber + (.1 * datasetversion.minorversionnumber) in \n"
                + "(\n"
                + "select datasetversion.dataset_id || ':' || max(datasetversion.versionnumber + (.1 * datasetversion.minorversionnumber)) as max \n"
                + "from datasetversion\n"
                + "join dataset on dataset.id = datasetversion.dataset_id\n"
                + "where versionstate='RELEASED' \n"
                + "and date_trunc('month', releasetime) <=  to_date('" + yyyymm + "','YYYY-MM')\n"
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
    public long filesToMonth(String yyyymm) throws Exception {
        Query query = em.createNativeQuery(""
                + "select count(*)\n"
                + "from filemetadata\n"
                + "join datasetversion on datasetversion.id = filemetadata.datasetversion_id\n"
                + "where datasetversion.dataset_id || ':' || datasetversion.versionnumber + (.1 * datasetversion.minorversionnumber) in \n"
                + "(\n"
                + "select datasetversion.dataset_id || ':' || max(datasetversion.versionnumber + (.1 * datasetversion.minorversionnumber)) as max \n"
                + "from datasetversion\n"
                + "join dataset on dataset.id = datasetversion.dataset_id\n"
                + "where versionstate='RELEASED'\n"
                //                + "and date_trunc('month', releasetime) <=  to_date('2018-03','YYYY-MM')\n"
                + "and date_trunc('month', releasetime) <=  to_date('" + yyyymm + "','YYYY-MM')\n"
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
    public long downloadsToMonth(String yyyymm) throws Exception {
        Query query = em.createNativeQuery(""
                + "select count(id)\n"
                + "from guestbookresponse\n"
                + "where date_trunc('month', responsetime) <=  to_date('" + yyyymm + "','YYYY-MM');"
        );
        logger.fine("query: " + query);
        return (long) query.getSingleResult();
    }

    public List<Object[]> dataversesByCategory() throws Exception {

        Query query = em.createNativeQuery(""
                + "select dataversetype, count(dataversetype) from dataverse\n"
                + "join dvobject on dvobject.id = dataverse.id\n"
                + "where dvobject.publicationdate is not null\n"
                + "group by dataversetype\n"
                + "order by count desc;"
        );

        logger.fine("query: " + query);
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
               + "datasetversion.dataset_id || ':' || datasetversion.versionnumber + (.1 * datasetversion.minorversionnumber) in \n"
               + "(\n"
               + "select datasetversion.dataset_id || ':' || max(datasetversion.versionnumber + (.1 * datasetversion.minorversionnumber)) as max \n"
               + "from datasetversion\n"
               + "join dataset on dataset.id = datasetversion.dataset_id\n"
               + "where versionstate='RELEASED'\n"
               + "and dataset.harvestingclient_id is null\n"
               + "group by dataset_id \n"
               + ")\n"
               + "AND datasetfieldtype.name = 'subject'\n"
               + "GROUP BY strvalue\n"
               + "ORDER BY count(dataset.id) desc;"
        );
        logger.info("query: " + query);

        return query.getResultList();
    }

    /* Helper functions for metric caching */
    public String returnUnexpiredCacheMonthly(String metricName, String yyyymm) throws Exception {
        Metric queriedMetric = getMetric(metricName, yyyymm);

        if (!doWeQueryAgainMonthly(queriedMetric)) {
            return queriedMetric.getMetricValue();
        }
        return null;
    }

    public String returnUnexpiredCacheAllTime(String metricName) throws Exception {
        Metric queriedMetric = getMetric(metricName);

        if (!doWeQueryAgainAllTime(queriedMetric)) {
            return queriedMetric.getMetricValue();
        }
        return null;
    }

    //This is for deciding whether to used a cached value on monthly queries
    //Assumes the metric passed in is sane (e.g. not run for past the current month, not a garbled date string, etc)
    public boolean doWeQueryAgainMonthly(Metric queriedMetric) {
        if (null == queriedMetric) { //never queried before
            return true;
        }

        String yyyymm = queriedMetric.getMetricDateString();
        String thisMonthYYYYMM = MetricsUtil.getCurrentMonth();

        Date lastCalled = queriedMetric.getLastCalledDate();
        LocalDateTime ldt = LocalDateTime.ofInstant((new Date()).toInstant(), ZoneId.systemDefault());

        int minutesUntilNextQuery = systemConfig.getMetricsCacheTimeoutMinutes();

        if (yyyymm.equals(thisMonthYYYYMM)) { //if this month
            LocalDateTime ldtMinus = ldt.minusMinutes(minutesUntilNextQuery);
            Date todayMinus = Date.from(ldtMinus.atZone(ZoneId.systemDefault()).toInstant());

            //allow if today minus query wait is after last called time
            return (todayMinus.after(lastCalled));
        } else {
            String lastRunYYYYMM = yyyymmFormat.format(lastCalled);

            //if queried was last run during the month it was querying.  
            //Allows one requery of a past month to make it up to date.
            return (lastRunYYYYMM.equals(yyyymm));
        }
    }

    //This is for deciding whether to used a cached value over all time
    public boolean doWeQueryAgainAllTime(Metric queriedMetric) {
        if (null == queriedMetric) { //never queried before
            return true;
        }

        int minutesUntilNextQuery = systemConfig.getMetricsCacheTimeoutMinutes();
        Date lastCalled = queriedMetric.getLastCalledDate();
        LocalDateTime ldt = LocalDateTime.ofInstant((new Date()).toInstant(), ZoneId.systemDefault());

        LocalDateTime ldtMinus = ldt.minusMinutes(minutesUntilNextQuery);
        Date todayMinus = Date.from(ldtMinus.atZone(ZoneId.systemDefault()).toInstant());

        //allow if today minus query wait is after last called time
        return (todayMinus.after(lastCalled));
    }

    public Metric save(Metric newMetric, boolean monthly) throws Exception {
        Metric oldMetric;
        if (monthly) {
            oldMetric = getMetric(newMetric.getMetricTitle(), newMetric.getMetricDateString());
        } else {
            oldMetric = getMetric(newMetric.getMetricTitle());
        }
        if (oldMetric != null) {
            em.remove(oldMetric);
            em.flush();
        }
        em.persist(newMetric);
        return em.merge(newMetric);
    }

    public Metric getMetric(String metricTitle, String yymmmm) throws Exception {
        String searchMetricName = Metric.generateMetricName(metricTitle, yymmmm);

        return getMetric(searchMetricName);
    }

    public Metric getMetric(String searchMetricName) throws Exception {
        Query query = em.createQuery("select object(o) from Metric as o where o.metricName = :metricName", Metric.class);
        query.setParameter("metricName", searchMetricName);
        Metric metric = null;
        try {
            metric = (Metric) query.getSingleResult();
        } catch (javax.persistence.NoResultException nr) {
            //do nothing
        } catch (NonUniqueResultException nur) {
            throw new Exception("Multiple cached results found for this query. Contact your system administrator.");
        }
        return metric;
    }

}
