package edu.harvard.iq.dataverse.metrics;

import edu.harvard.iq.dataverse.Metric;
import static edu.harvard.iq.dataverse.metrics.MetricsUtil.*;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
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

    
//MAD: I know some of these shouldn't have had dataLocation added
    
    /** Dataverses */
    
    //MAD: I totally misunderstood how to get harvested entries.
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
    
    public long dataversesPastDays(String dataLocation, int days) throws Exception {
        Query query = em.createNativeQuery(""
                + "select count(dvobject.id)\n"
                + "from dataverse\n"
                + "join dvobject on dvobject.id = dataverse.id\n"
                + "where dvobject.publicationdate is not null\n"
                + "and publicationdate > current_date - interval '"+days+"' day;\n"
        );

        logger.fine("query: " + query);

        return (long) query.getSingleResult();
    }
    
    public List<Object[]> dataversesByCategory(String dataLocation) throws Exception {

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
    
    public List<Object[]> dataversesBySubject(String dataLocation) {
        Query query = em.createNativeQuery(""
                + "select cvv.strvalue, count(dataverse_id) from dataversesubjects\n"
                + "join controlledvocabularyvalue cvv ON cvv.id = controlledvocabularyvalue_id\n"
                + "group by cvv.strvalue\n"
                + "order by count desc;"
              
        );
        logger.info("query: " + query);

        return query.getResultList();
    }
    
    /** Datasets */
    
    public List<Object[]> datasetsBySubjectToMonth(String dataLocation, String yyyymm) {        
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
                + "and date_trunc('month', releasetime) <=  to_date('" + yyyymm + "','YYYY-MM')\n"
                + "group by dataset_id \n"
                + ")\n"
                + "AND datasetfieldtype.name = 'subject'\n"
                + "GROUP BY strvalue\n"
                + "ORDER BY count(dataset.id) desc;"
        );
        logger.info("query: " + query);

        return query.getResultList();
    }
    
    
    /**
     * @param yyyymm Month in YYYY-MM format.
     */
    public long datasetsToMonth(String dataLocation, String yyyymm) throws Exception {
        String dataLocationLine = "and dataset.harvestingclient_id is null\n"; //Default is DATA_LOCATION_LOCAL
        if (DATA_LOCATION_REMOTE.equals(dataLocation)) {
            dataLocationLine = "and dataset.harvestingclient_id is not null\n";
        } else if(DATA_LOCATION_ALL.equals(dataLocation)) {
            dataLocationLine = ""; //no specification will return all
        }
        
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
                + dataLocationLine
                + "group by dataset_id \n"
                + ");"
        );
        logger.fine("query: " + query);

        return (long) query.getSingleResult();
    }
    
    public long datasetsPastDays(String dataLocation, int days) throws Exception {

        Query query = em.createNativeQuery(
            "select count(*)\n" +
            "from datasetversion\n" +
            "where datasetversion.dataset_id || ':' || datasetversion.versionnumber + (.1 * datasetversion.minorversionnumber) in \n" +
            "(\n" +
            "	select datasetversion.dataset_id || ':' || max(datasetversion.versionnumber + (.1 * datasetversion.minorversionnumber)) as max \n" +
            "	from datasetversion\n" +
            "	join dataset on dataset.id = datasetversion.dataset_id\n" +
            "	where versionstate='RELEASED' \n" +
            "	and releasetime > current_date - interval '"+days+"' day\n" +
            "	and dataset.harvestingclient_id is null\n" +
            "	group by dataset_id \n" +
            ");"
        );
        logger.fine("query: " + query);

        return (long) query.getSingleResult();
    }


    /** Files */
    
    /**
     * @param yyyymm Month in YYYY-MM format.
     */
    public long filesToMonth(String dataLocation, String yyyymm) throws Exception {
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
                + "and date_trunc('month', releasetime) <=  to_date('" + yyyymm + "','YYYY-MM')\n"
                + "and dataset.harvestingclient_id is null\n"
                + "group by dataset_id \n"
                + ");"
        );
        logger.fine("query: " + query);
        return (long) query.getSingleResult();
    }
    
    public long filesPastDays(String dataLocation, int days) throws Exception {
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
                + "and releasetime > current_date - interval '"+days+"' day\n"
                + "and dataset.harvestingclient_id is null\n"
                + "group by dataset_id \n"
                + ");"
        );

        logger.fine("query: " + query);

        return (long) query.getSingleResult();
    }

    /** Downloads */
    
    /**
     * @param yyyymm Month in YYYY-MM format.
     */
    public long downloadsToMonth(String dataLocation, String yyyymm) throws Exception {
        Query query = em.createNativeQuery(""
                + "select count(id)\n"
                + "from guestbookresponse\n"
                + "where date_trunc('month', responsetime) <=  to_date('" + yyyymm + "','YYYY-MM');"
        );
        logger.fine("query: " + query);
        return (long) query.getSingleResult();
    }

    public long downloadsPastDays(String dataLocation, int days) throws Exception {
        Query query = em.createNativeQuery(""
                + "select count(id)\n"
                + "from guestbookresponse\n"
                + "where responsetime > current_date - interval '"+days+"' day;\n"
        );

        logger.fine("query: " + query);

        return (long) query.getSingleResult();
    }
    
    /** Helper functions for metric caching */
    
    //MAD: hopefully these can all go away if we are moving everything correctly into database columns
    public String returnUnexpiredCacheDayBased(String metricName, String dataLocation, String days) throws Exception {
        Metric queriedMetric = getMetric(metricName, dataLocation, days);

        if (!doWeQueryAgainDayBased(queriedMetric)) {
            return queriedMetric.getMetricValue();
        }
        return null;
    }
    
    public String returnUnexpiredCacheMonthly(String metricName, String dataLocation, String yyyymm) throws Exception {
        Metric queriedMetric = getMetric(metricName, dataLocation, yyyymm);

        if (!doWeQueryAgainMonthly(queriedMetric)) {
            return queriedMetric.getMetricValue();
        }
        return null;
    }

    public String returnUnexpiredCacheAllTime(String metricName, String dataLocation) throws Exception {
        Metric queriedMetric = getMetric(metricName, dataLocation, null); //MAD: not passing a date

        if (!doWeQueryAgainAllTime(queriedMetric)) {
            return queriedMetric.getMetricValue();
        }
        return null;
    }
        
    //For day based metrics we check to see if the metric has been pulled today
    public boolean doWeQueryAgainDayBased(Metric queriedMetric) {
        if (null == queriedMetric) { //never queried before
            return true;
        }

        LocalDate lastCalled = LocalDate.from(queriedMetric.getLastCalledDate().toInstant().atZone(ZoneId.systemDefault()));
        LocalDate todayDate = LocalDate.now(ZoneId.systemDefault());
        

        if(!lastCalled.equals(todayDate)) {
            return true;
        } else {
            return false;
        }        
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

    public Metric save(Metric newMetric) throws Exception {
        Metric oldMetric = getMetric(newMetric.getMetricTitle(), newMetric.getMetricDataLocation(), newMetric.getMetricDateString());

        if (oldMetric != null) {
            em.remove(oldMetric);
            em.flush();
        }
        em.persist(newMetric);
        return em.merge(newMetric);
    }

    //This works for date and day based metrics
    public Metric getMetric(String metricTitle, String dataLocation, String dayString) throws Exception {
        //MAD: Add the other parameters
        Query query = em.createQuery("select object(o) from Metric as o"
                + " where o.metricName = :metricName"
                + " and o.metricDataLocation" + (dataLocation == null ? " is null" : " = :metricDataLocation")
                + " and o.metricDayString" + (dayString == null ? " is null" :  " = :metricDayString")
                , Metric.class);
        query.setParameter("metricName", metricTitle);
        if(dataLocation != null){ query.setParameter("metricDataLocation", dataLocation);}
        if(dayString != null) {query.setParameter("metricDayString", dayString);}
        
        logger.log(Level.INFO, "getMetric query: {0}", query);
        
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
