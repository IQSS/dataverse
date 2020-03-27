package edu.harvard.iq.dataverse.metrics;

import edu.harvard.iq.dataverse.persistence.cache.Metric;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static edu.harvard.iq.dataverse.metrics.MetricsUtil.DATA_LOCATION_ALL;
import static edu.harvard.iq.dataverse.metrics.MetricsUtil.DATA_LOCATION_LOCAL;
import static edu.harvard.iq.dataverse.metrics.MetricsUtil.DATA_LOCATION_REMOTE;

@Stateless
public class MetricsServiceBean implements Serializable {

    private static final Logger logger = Logger.getLogger(MetricsServiceBean.class.getCanonicalName());

    private static final SimpleDateFormat yyyymmFormat = new SimpleDateFormat(MetricsUtil.YEAR_AND_MONTH_PATTERN);

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    @Inject
    private SettingsServiceBean settingsService;


    /** Dataverses */

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
        logger.log(Level.FINE, "Metric query: {0}", query);

        return (long) query.getSingleResult();
    }

    public long dataversesPastDays(int days) throws Exception {
        Query query = em.createNativeQuery(""
                                                   + "select count(dvobject.id)\n"
                                                   + "from dataverse\n"
                                                   + "join dvobject on dvobject.id = dataverse.id\n"
                                                   + "where dvobject.publicationdate is not null\n"
                                                   + "and publicationdate > current_date - interval '" + days + "' day;\n"
        );
        logger.log(Level.FINE, "Metric query: {0}", query);

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
        logger.log(Level.FINE, "Metric query: {0}", query);

        return query.getResultList();
    }

    public List<Object[]> dataversesBySubject() {
        Query query = em.createNativeQuery(""
                                                   + "select cvv.strvalue, count(dataverse_id) from dataversesubjects\n"
                                                   + "join controlledvocabularyvalue cvv ON cvv.id = controlledvocabularyvalue_id \n"
                                                   //+ "where dataverse_id != ( select id from dvobject where owner_id is null) \n" //removes root, we decided to do this in the homepage js instead
                                                   + "group by cvv.strvalue\n"
                                                   + "order by count desc;"

        );
        logger.log(Level.FINE, "Metric query: {0}", query);

        return query.getResultList();
    }

    /**
     * Datasets
     */
    public List<ChartMetrics> countPublishedDatasets() {
        return mapToChartMetrics(em.createNativeQuery(
                "SELECT\n" +
                        "    EXTRACT(YEAR FROM dsv.lastupdatetime) as year,\n" +
                        "    EXTRACT(MONTH FROM dsv.lastupdatetime) as month,\n" +
                        "    count (dsv.id)\n" +
                        "    FROM datasetversion dsv\n" +
                        "    WHERE\n" +
                        "        dsv.versionnumber = 1 and\n" +
                        "        dsv.minorversionnumber = 0 and\n" +
                        "        dsv.releasetime is not null\n" +
                        "GROUP BY year, month")
                                            .getResultList());
    }

    /**
     * Authenticated users
     */
    public List<ChartMetrics> countAuthenticatedUsers() {
        return mapToChartMetrics(em.createNativeQuery(
                "SELECT\n" +
                        "    EXTRACT(YEAR FROM au.createdtime) as year,\n" +
                        "    EXTRACT(MONTH FROM au.createdtime) as month,\n" +
                        "    count (au.id)\n" +
                        "    FROM authenticateduser au\n" +
                        "    GROUP BY year, month")
                .getResultList());
    }

    /**
     * Published files
     */
    public List<ChartMetrics> countPublishedFiles() {
        return mapToChartMetrics(em.createNativeQuery(
                "select \n" +
                        "extract(year from sub.first_publishDate) as year,\n" +
                        "extract(month from sub.first_publishDate) as month,\n" +
                        "count(sub.datafile_id) as published_files_counter\n" +
                        "from (select \n" +
                        "       fm.datafile_id,\n" +
                        "       min(dsv.releasetime) as first_publishDate\n" +
                        "       from filemetadata fm\n" +
                        "       inner join datasetversion dsv\n" +
                        "       on fm.datasetversion_id = dsv.id\n" +
                        "       where dsv.releasetime is not null\n" +
                        "       group by fm.datafile_id\n" +
                        "       order by fm.datafile_id ) sub\n" +
                        "group by year, month\n" +
                        "order by year, month\n")
                .getResultList());
    }

    /**
     * Published files size
     */
    public List<ChartMetrics> countPublishedFilesStorage() {
        return mapToChartMetrics(em.createNativeQuery(
            "select \n" +
                    "extract(year from sub.first_publishDate) as year,\n" +
                    "extract(month from sub.first_publishDate) as month,\n" +
                    "ceiling(sum(sub.filesize) / (1024 * 1024)) as published_files_counter\n" +
                    "from (select \n" +
                    "       df.filesize,\n" +
                    "       min(dsv.releasetime) as first_publishDate\n" +
                    "       from filemetadata fm\n" +
                    "       inner join datasetversion dsv\n" +
                    "       on fm.datasetversion_id = dsv.id\n" +
                    "       left join datafile df\n" +
                    "       on fm.datafile_id = df.id\n" +
                    "       where dsv.releasetime is not null\n" +
                    "       group by fm.datafile_id, df.filesize\n" +
                    "       order by fm.datafile_id ) sub\n" +
                    "group by year, month\n" +
                    "order by year, month")
                .getResultList());
    }

    /**
     * Downloaded Files
     */
    public List<ChartMetrics> countDownloadedFiles() {
        return mapToChartMetrics(em.createNativeQuery(
                "SELECT" +
                        "    EXTRACT(YEAR FROM gr.responsetime) as year," +
                        "    EXTRACT(MONTH FROM gr.responsetime) as month," +
                        "    count (gr.id)" +
                        "    FROM guestbookresponse gr" +
                        "    GROUP BY year, month")
                .getResultList());
    }

    /**
     * Downloaded Datasets
     */
    public List<ChartMetrics> countDownloadedDatasets() {
        return mapToChartMetrics(em.createNativeQuery(
                "SELECT" +
                        "    EXTRACT(YEAR FROM ddl.downloaddate) as year," +
                        "    EXTRACT(MONTH FROM ddl.downloaddate) as month," +
                        "    count (ddl.id)" +
                        "    FROM downloaddatasetlog ddl" +
                        "    GROUP BY year, month")
                .getResultList());
    }

    private List<ChartMetrics> mapToChartMetrics(List<Object[]> result) {
        return result.stream()
                .peek(dm -> dm[2] = dm[2] instanceof BigDecimal ?  ((BigDecimal) dm[2]).longValue() : dm[2])
                .map(dm -> new ChartMetrics((Double) dm[0], (Double) dm[1], (Long) dm[2]))
                .collect(Collectors.toList());
    }


    /**
     * @param yyyymm Month in YYYY-MM format.
     */
    public long datasetsToMonth(String yyyymm, String dataLocation) throws Exception {
        String dataLocationLine = "(date_trunc('month', releasetime) <=  to_date('" + yyyymm + "','YYYY-MM') and dataset.harvestingclient_id IS NULL)\n";

        if (!DATA_LOCATION_LOCAL.equals(dataLocation)) { //Default api state is DATA_LOCATION_LOCAL
            //we have to use createtime for harvest as post dvn3 harvests do not have releasetime populated
            String harvestBaseLine = "(date_trunc('month', createtime) <=  to_date('" + yyyymm + "','YYYY-MM') and dataset.harvestingclient_id IS NOT NULL)\n";
            if (DATA_LOCATION_REMOTE.equals(dataLocation)) {
                dataLocationLine = harvestBaseLine; //replace
            } else if (DATA_LOCATION_ALL.equals(dataLocation)) {
                dataLocationLine = "(" + dataLocationLine + " OR " + harvestBaseLine + ")\n"; //append
            }
        }

        // Note that this SQL line in the code below: 
        // datasetversion.dataset_id || ':' || max(datasetversion.versionnumber + (.1 * datasetversion.minorversionnumber))
        // behaves somewhat counter-intuitively if the versionnumber and/or 
        // minorversionnumber is/are NULL - it results in an empty string 
        // (NOT the string "{dataset_id}:", in other words). Some harvested 
        // versions do not have version numbers (only the ones harvested from 
        // other Dataverses!) It works fine 
        // for our purposes below, because we are simply counting the selected 
        // lines - i.e. we don't care if some of these lines are empty. 
        // But do not use this notation if you need the values returned to 
        // meaningfully identify the datasets!

        Query query = em.createNativeQuery(
                "select count(*)\n"
                        + "from (\n"
                        + "select datasetversion.dataset_id || ':' || max(datasetversion.versionnumber + (.1 * datasetversion.minorversionnumber))\n"
                        + "from datasetversion\n"
                        + "join dataset on dataset.id = datasetversion.dataset_id\n"
                        + "where versionstate='RELEASED' \n"
                        + "and \n"
                        + dataLocationLine //be careful about adding more and statements after this line.
                        + "group by dataset_id \n"
                        + ") sub_temp"
        );
        logger.log(Level.FINE, "Metric query: {0}", query);

        return (long) query.getSingleResult();
    }

    public List<Object[]> datasetsBySubjectToMonth(String yyyymm, String dataLocation) {
        // The SQL code below selects the local, non-harvested dataset versions:
        // A published local datasets may have more than one released version!
        // So that's why we have to jump through some extra hoops below
        // in order to select the latest one:
        String originClause = "(datasetversion.dataset_id || ':' || datasetversion.versionnumber + (.1 * datasetversion.minorversionnumber) in\n" +
                "(\n" +
                "select datasetversion.dataset_id || ':' || max(datasetversion.versionnumber + (.1 * datasetversion.minorversionnumber))\n" +
                "       from datasetversion\n" +
                "       join dataset on dataset.id = datasetversion.dataset_id\n" +
                "       where versionstate='RELEASED'\n" +
                "       	     and dataset.harvestingclient_id is null\n" +
                "       	     and date_trunc('month', releasetime) <=  to_date('" + yyyymm + "','YYYY-MM')\n" +
                "       group by dataset_id\n" +
                "))\n";

        if (!DATA_LOCATION_LOCAL.equals(dataLocation)) { //Default api state is DATA_LOCATION_LOCAL
            //we have to use createtime for harvest as post dvn3 harvests do not have releasetime populated
            // But we can operate on the assumption that all the harvested datasets
            // are published, and there is always only one version per dataset - 
            // so the query is simpler:
            String harvestOriginClause = "(\n" +
                    "   datasetversion.dataset_id = dataset.id\n" +
                    "   AND dataset.harvestingclient_id IS NOT null \n" +
                    "   AND date_trunc('month', datasetversion.createtime) <=  to_date('" + yyyymm + "','YYYY-MM')\n" +
                    ")\n";

            if (DATA_LOCATION_REMOTE.equals(dataLocation)) {
                originClause = harvestOriginClause; //replace
            } else if (DATA_LOCATION_ALL.equals(dataLocation)) {
                originClause = "(" + originClause + " OR " + harvestOriginClause + ")\n"; //append
            }
        }

        Query query = em.createNativeQuery(""
                                                   + "SELECT strvalue, count(dataset.id)\n"
                                                   + "FROM datasetfield_controlledvocabularyvalue \n"
                                                   + "JOIN controlledvocabularyvalue ON controlledvocabularyvalue.id = datasetfield_controlledvocabularyvalue.controlledvocabularyvalues_id\n"
                                                   + "JOIN datasetfield ON datasetfield.id = datasetfield_controlledvocabularyvalue.datasetfield_id\n"
                                                   + "JOIN datasetfieldtype ON datasetfieldtype.id = controlledvocabularyvalue.datasetfieldtype_id\n"
                                                   + "JOIN datasetversion ON datasetversion.id = datasetfield.datasetversion_id\n"
                                                   + "JOIN dataset ON dataset.id = datasetversion.dataset_id\n"
                                                   + "WHERE\n"
                                                   + originClause
                                                   + "AND datasetfieldtype.name = 'subject'\n"
                                                   + "GROUP BY strvalue\n"
                                                   + "ORDER BY count(dataset.id) desc;"
        );
        logger.log(Level.FINE, "Metric query: {0}", query);

        return query.getResultList();
    }

    public long datasetsPastDays(int days, String dataLocation) throws Exception {
        String dataLocationLine = "(releasetime > current_date - interval '" + days + "' day and dataset.harvestingclient_id IS NULL)\n";

        if (!DATA_LOCATION_LOCAL.equals(dataLocation)) { //Default api state is DATA_LOCATION_LOCAL
            //we have to use createtime for harvest as post dvn3 harvests do not have releasetime populated
            String harvestBaseLine = "(createtime > current_date - interval '" + days + "' day and dataset.harvestingclient_id IS NOT NULL)\n";
            if (DATA_LOCATION_REMOTE.equals(dataLocation)) {
                dataLocationLine = harvestBaseLine; //replace
            } else if (DATA_LOCATION_ALL.equals(dataLocation)) {
                dataLocationLine += " or " + harvestBaseLine; //append
            }
        }

        Query query = em.createNativeQuery(
                "select count(*)\n"
                        + "from (\n"
                        + "select datasetversion.dataset_id || ':' || max(datasetversion.versionnumber + (.1 * datasetversion.minorversionnumber)) as max\n"
                        + "from datasetversion\n"
                        + "join dataset on dataset.id = datasetversion.dataset_id\n"
                        + "where versionstate='RELEASED' \n"
                        + "and \n"
                        + dataLocationLine //be careful about adding more and statements after this line.
                        + "group by dataset_id \n"
                        + ") sub_temp"
        );
        logger.log(Level.FINE, "Metric query: {0}", query);

        return (long) query.getSingleResult();
    }


    /** Files */

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
                                                   + "and date_trunc('month', releasetime) <=  to_date('" + yyyymm + "','YYYY-MM')\n"
                                                   + "and dataset.harvestingclient_id is null\n"
                                                   + "group by dataset_id \n"
                                                   + ");"
        );
        logger.log(Level.FINE, "Metric query: {0}", query);

        return (long) query.getSingleResult();
    }

    public long filesPastDays(int days) throws Exception {
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
                                                   + "and releasetime > current_date - interval '" + days + "' day\n"
                                                   + "and dataset.harvestingclient_id is null\n"
                                                   + "group by dataset_id \n"
                                                   + ");"
        );
        logger.log(Level.FINE, "Metric query: {0}", query);

        return (long) query.getSingleResult();
    }

    /** Downloads */

    /**
     * This includes getting historic download without a timestamp if query
     * is earlier than earliest timestamped record
     *
     * @param yyyymm Month in YYYY-MM format.
     */
    public long downloadsToMonth(String yyyymm) throws Exception {
        Query earlyDateQuery = em.createNativeQuery(""
                                                            + "select responsetime from guestbookresponse\n"
                                                            + "ORDER BY responsetime LIMIT 1;"
        );

        try {
            Timestamp earlyDateTimestamp = (Timestamp) earlyDateQuery.getSingleResult();
            Date earliestDate = new Date(earlyDateTimestamp.getTime());
            SimpleDateFormat formatter2 = new SimpleDateFormat("yyyy-MM");
            Date dateQueried = formatter2.parse(yyyymm);

            if (!dateQueried.before(earliestDate)) {
                Query query = em.createNativeQuery(""
                                                           + "select count(id)\n"
                                                           + "from guestbookresponse\n"
                                                           + "where date_trunc('month', responsetime) <=  to_date('" + yyyymm + "','YYYY-MM')"
                                                           + "or responsetime is NULL;" //includes historic guestbook records without date
                );
                logger.log(Level.FINE, "Metric query: {0}", query);
                return (long) query.getSingleResult();
            } else {
                //When we query before the earliest dated record, return 0;
                return 0L;
            }
        } catch (NoResultException e) {
            //If earlyDateQuery.getSingleResult is null, then there are no guestbooks and we can return 0
            return 0L;
        }

    }

    public long downloadsPastDays(int days) throws Exception {
        Query query = em.createNativeQuery(""
                                                   + "select count(id)\n"
                                                   + "from guestbookresponse\n"
                                                   + "where responsetime > current_date - interval '" + days + "' day;\n"
        );
        logger.log(Level.FINE, "Metric query: {0}", query);

        return (long) query.getSingleResult();
    }

    /**
     * Helper functions for metric caching
     */

    public String returnUnexpiredCacheDayBased(String metricName, String days, String dataLocation) throws Exception {
        Metric queriedMetric = getMetric(metricName, dataLocation, days);

        if (!doWeQueryAgainDayBased(queriedMetric)) {
            return queriedMetric.getValueJson();
        }
        return null;
    }

    public String returnUnexpiredCacheMonthly(String metricName, String yyyymm, String dataLocation) throws Exception {
        Metric queriedMetric = getMetric(metricName, dataLocation, yyyymm);

        if (!doWeQueryAgainMonthly(queriedMetric)) {
            return queriedMetric.getValueJson();
        }
        return null;
    }

    public String returnUnexpiredCacheAllTime(String metricName, String dataLocation) throws Exception {
        Metric queriedMetric = getMetric(metricName, dataLocation, null); //MAD: not passing a date

        if (!doWeQueryAgainAllTime(queriedMetric)) {
            return queriedMetric.getValueJson();
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


        return !lastCalled.equals(todayDate);
    }

    //This is for deciding whether to used a cached value on monthly queries
    //Assumes the metric passed in is sane (e.g. not run for past the current month, not a garbled date string, etc)
    public boolean doWeQueryAgainMonthly(Metric queriedMetric) {
        if (null == queriedMetric) { //never queried before
            return true;
        }

        String yyyymm = queriedMetric.getDateString();
        String thisMonthYYYYMM = MetricsUtil.getCurrentMonth();

        Date lastCalled = queriedMetric.getLastCalledDate();
        LocalDateTime ldt = LocalDateTime.ofInstant((new Date()).toInstant(), ZoneId.systemDefault());

        long minutesUntilNextQuery = settingsService.getValueForKeyAsLong(SettingsServiceBean.Key.MetricsCacheTimeoutMinutes);

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

        long minutesUntilNextQuery = settingsService.getValueForKeyAsLong(SettingsServiceBean.Key.MetricsCacheTimeoutMinutes);
        Date lastCalled = queriedMetric.getLastCalledDate();
        LocalDateTime ldt = LocalDateTime.ofInstant((new Date()).toInstant(), ZoneId.systemDefault());

        LocalDateTime ldtMinus = ldt.minusMinutes(minutesUntilNextQuery);
        Date todayMinus = Date.from(ldtMinus.atZone(ZoneId.systemDefault()).toInstant());

        //allow if today minus query wait is after last called time
        return (todayMinus.after(lastCalled));
    }

    public Metric save(Metric newMetric) throws Exception {
        Metric oldMetric = getMetric(newMetric.getName(), newMetric.getDataLocation(), newMetric.getDateString());

        if (oldMetric != null) {
            em.remove(oldMetric);
            em.flush();
        }
        em.persist(newMetric);
        return em.merge(newMetric);
    }

    //This works for date and day based metrics
    //It is ok to pass null for dataLocation and dayString
    public Metric getMetric(String name, String dataLocation, String dayString) throws Exception {
        Query query = em.createQuery("select object(o) from Metric as o"
                                             + " where o.name = :name"
                                             + " and o.dataLocation" + (dataLocation == null ? " is null" : " = :dataLocation")
                                             + " and o.dayString" + (dayString == null ? " is null" : " = :dayString")
                , Metric.class);
        query.setParameter("name", name);
        if (dataLocation != null) {
            query.setParameter("dataLocation", dataLocation);
        }
        if (dayString != null) {
            query.setParameter("dayString", dayString);
        }

        logger.log(Level.FINE, "getMetric query: {0}", query);

        Metric metric = null;
        try {
            metric = (Metric) query.getSingleResult();
        } catch (javax.persistence.NoResultException nr) {
            //do nothing
        } catch (NonUniqueResultException nur) {
            //duplicates can happen when a new/requeried metric is called twice and saved twice before one can use the cache
            //this remove all but the 0th index one in that case
            for (int i = 1; i < query.getResultList().size(); i++) {
                Metric extraMetric = (Metric) query.getResultList().get(i);
                em.remove(extraMetric);
                em.flush();
            }
            metric = (Metric) query.getResultList().get(0);
        }
        return metric;
    }

}
