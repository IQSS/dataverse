package edu.harvard.iq.dataverse.metrics;

import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.Metric;
import edu.harvard.iq.dataverse.makedatacount.DatasetMetrics;
import edu.harvard.iq.dataverse.makedatacount.MakeDataCountUtil.MetricType;

import static edu.harvard.iq.dataverse.metrics.MetricsUtil.*;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.ws.rs.core.UriInfo;

@Stateless
public class MetricsServiceBean implements Serializable {

    private static final Logger logger = Logger.getLogger(MetricsServiceBean.class.getCanonicalName());

    private static final SimpleDateFormat yyyymmFormat = new SimpleDateFormat(MetricsUtil.YEAR_AND_MONTH_PATTERN);

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    @EJB
    SystemConfig systemConfig;

    /** Dataverses */

    
    public JsonArray getDataversesTimeSeries(UriInfo uriInfo, Dataverse d) {
        Query query = em.createNativeQuery(""
                + "select distinct to_char(date_trunc('month', dvobject.publicationdate),'YYYY-MM') as month, count(date_trunc('month', dvobject.publicationdate))\n"
                + "from dataverse\n"
                + "join dvobject on dvobject.id = dataverse.id\n"
                + "where dvobject.publicationdate is not null\n"
                + ((d == null) ? "" : "and dvobject.id in (" + getCommaSeparatedIdStringForSubtree(d, "Dataverse") + ")\n")
                + "group by  date_trunc('month', publicationdate);");
        logger.log(Level.FINE, "Metric query: {0}", query);
        List<Object[]> results = query.getResultList();
        return MetricsUtil.timeSeriesToJson(results);
    }
    
    /**
     * @param yyyymm Month in YYYY-MM format.
     * @param d
     */
    public long dataversesToMonth(String yyyymm, Dataverse d) {
        Query query = em.createNativeQuery(""
                + "select count(dvobject.id)\n"
                + "from dataverse\n"
                + "join dvobject on dvobject.id = dataverse.id\n"
                + "where dvobject.publicationdate is not null\n"
                + ((d == null) ? "" : "and dvobject.owner_id in (" + getCommaSeparatedIdStringForSubtree(d, "Dataverse") + ")\n")
                + "and date_trunc('month', publicationdate) <=  to_date('" + yyyymm + "','YYYY-MM');"
        );
        logger.log(Level.FINE, "Metric query: {0}", query);

        return (long) query.getSingleResult();
    }

    public long dataversesPastDays(int days, Dataverse d) {
        Query query = em.createNativeQuery(""
                + "select count(dvobject.id)\n"
                + "from dataverse\n"
                + "join dvobject on dvobject.id = dataverse.id\n"
                + "where dvobject.publicationdate is not null\n"
                + ((d == null) ? "" : "and dvobject.owner_id in (" + getCommaSeparatedIdStringForSubtree(d, "Dataverse") + ")\n")
                + "and publicationdate > current_date - interval '"+days+"' day;\n"
        );
        logger.log(Level.FINE, "Metric query: {0}", query);

        return (long) query.getSingleResult();
    }

    public List<Object[]> dataversesByCategory(Dataverse d) {

        Query query = em.createNativeQuery(""
                + "select dataversetype, count(dataversetype) from dataverse\n"
                + "join dvobject on dvobject.id = dataverse.id\n"
                + "where dvobject.publicationdate is not null\n"
                + ((d == null) ? "" : "and dvobject.owner_id in (" + getCommaSeparatedIdStringForSubtree(d, "Dataverse") + ")\n")
                + "group by dataversetype\n"
                + "order by count desc;"
        );
        logger.log(Level.FINE, "Metric query: {0}", query);

        return query.getResultList();
    }

    public List<Object[]> dataversesBySubject(Dataverse d) {
        // ToDo - published only?
        Query query = em.createNativeQuery(""
                + "select cvv.strvalue, count(dataverse_id) from dataversesubjects\n"
                + "join controlledvocabularyvalue cvv ON cvv.id = controlledvocabularyvalue_id \n"
                //+ "where dataverse_id != ( select id from dvobject where owner_id is null) \n" //removes root, we decided to do this in the homepage js instead
                + ((d == null) ? "" : "and dataverse_id in (" + getCommaSeparatedIdStringForSubtree(d, "Dataverse") + ")\n")
                + "group by cvv.strvalue\n"
                + "order by count desc;"
        );
        logger.log(Level.FINE, "Metric query: {0}", query);

        return query.getResultList();
    }

    /** Datasets */

    
    public JsonArray getDatasetsTimeSeries(UriInfo uriInfo, String dataLocation, Dataverse d) {
        Query query = em.createNativeQuery(                
                "select distinct date, count(dataset_id)\n"
                + "from (\n"
                + "select min(to_char(COALESCE(releasetime, createtime), 'YYYY-MM')) as date, dataset_id\n"
                + "from datasetversion\n"
                + "where versionstate='RELEASED' \n"
                + (((d == null)&&(DATA_LOCATION_ALL.equals(dataLocation))) ? "" : "and dataset_id in (select dataset.id from dataset, dvobject where dataset.id=dvobject.id\n")
                + ((DATA_LOCATION_LOCAL.equals(dataLocation)) ? "and dataset.harvestingclient_id IS NULL and publicationdate is not null\n " : "")
                + ((DATA_LOCATION_REMOTE.equals(dataLocation)) ? "and dataset.harvestingclient_id IS NOT NULL\n "  : "")
                + ((d == null) ? "" : "and dvobject.owner_id in (" + getCommaSeparatedIdStringForSubtree(d, "Dataverse") + ")\n ")
                + (((d == null)&&(DATA_LOCATION_ALL.equals(dataLocation))) ? "" : ")\n")
                + "group by dataset_id) as subq group by subq.date order by date;"

        );
        logger.log(Level.FINE, "Metric query: {0}", query);
        List<Object[]> results = query.getResultList();
        return MetricsUtil.timeSeriesToJson(results);
    }
    
    
    /**
     * @param yyyymm Month in YYYY-MM format.
     * @param d
     */
    public long datasetsToMonth(String yyyymm, String dataLocation, Dataverse d) {
        String dataLocationLine = "(date_trunc('month', releasetime) <=  to_date('" + yyyymm + "','YYYY-MM') and dataset.harvestingclient_id IS NULL)\n";

        if (!DATA_LOCATION_LOCAL.equals(dataLocation)) { // Default api state is DATA_LOCATION_LOCAL
            //we have to use createtime for harvest as post dvn3 harvests do not have releasetime populated
            String harvestBaseLine = "(date_trunc('month', createtime) <=  to_date('" + yyyymm + "','YYYY-MM') and dataset.harvestingclient_id IS NOT NULL)\n";
            if (DATA_LOCATION_REMOTE.equals(dataLocation)) {
                dataLocationLine = harvestBaseLine; // replace
            } else if (DATA_LOCATION_ALL.equals(dataLocation)) {
                dataLocationLine = "(" + dataLocationLine + " OR " + harvestBaseLine + ")\n"; // append
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
                        + ((d == null) ? "" : "join dvobject on dvobject.id = dataset.id\n")
                        + "where versionstate='RELEASED' \n"
                        + ((d == null) ? "" : "and dvobject.owner_id in (" + getCommaSeparatedIdStringForSubtree(d, "Dataverse") + ")\n ")
                        + "and \n"
                        + dataLocationLine // be careful about adding more and statements after this line.
                        + "group by dataset_id \n"
            +") sub_temp"
        );
        logger.log(Level.FINE, "Metric query: {0}", query);

        return (long) query.getSingleResult();
    }

    public List<Object[]> datasetsBySubjectToMonth(String yyyymm, String dataLocation, Dataverse d) {
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

        if (!DATA_LOCATION_LOCAL.equals(dataLocation)) { // Default api state is DATA_LOCATION_LOCAL
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
                originClause = harvestOriginClause; // replace
            } else if (DATA_LOCATION_ALL.equals(dataLocation)) {
                originClause = "(" + originClause + " OR " + harvestOriginClause + ")\n"; // append
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
                + ((d == null) ? "" : "JOIN dvobject ON dvobject.id = dataset.id\n")
                + "WHERE\n"
                + originClause
                + "AND datasetfieldtype.name = 'subject'\n"
                + ((d == null) ? "" : "AND dvobject.owner_id in (" + getCommaSeparatedIdStringForSubtree(d, "Dataverse") + ")\n")
                + "GROUP BY strvalue\n"
                + "ORDER BY count(dataset.id) desc;"
        );
        logger.log(Level.FINE, "Metric query: {0}", query);

        return query.getResultList();
    }

    public long datasetsPastDays(int days, String dataLocation, Dataverse d) {
        String dataLocationLine = "(releasetime > current_date - interval '" + days + "' day and dataset.harvestingclient_id IS NULL)\n";

        if (!DATA_LOCATION_LOCAL.equals(dataLocation)) { // Default api state is DATA_LOCATION_LOCAL
            //we have to use createtime for harvest as post dvn3 harvests do not have releasetime populated
            String harvestBaseLine = "(createtime > current_date - interval '" + days + "' day and dataset.harvestingclient_id IS NOT NULL)\n";
            if (DATA_LOCATION_REMOTE.equals(dataLocation)) {
                dataLocationLine = harvestBaseLine; // replace
            } else if (DATA_LOCATION_ALL.equals(dataLocation)) {
                dataLocationLine += " or " + harvestBaseLine; // append
            }
        }

        Query query = em.createNativeQuery(
                "select count(*)\n"
                        + "from (\n"
                        + "select datasetversion.dataset_id || ':' || max(datasetversion.versionnumber + (.1 * datasetversion.minorversionnumber)) as max\n"
                        + "from datasetversion\n"
                        + "join dataset on dataset.id = datasetversion.dataset_id\n"
                        + ((d == null) ? "" : "join dvobject on dvobject.id = dataset.id\n")
                        + "where versionstate='RELEASED' \n"
                        + ((d == null) ? "" : "and dvobject.owner_id in (" + getCommaSeparatedIdStringForSubtree(d, "Dataverse") + ")\n")
                        + "and \n"
                        + dataLocationLine // be careful about adding more and statements after this line.
                        + "group by dataset_id \n"
            +") sub_temp"
        );
        logger.log(Level.FINE, "Metric query: {0}", query);

        return (long) query.getSingleResult();
    }

    /** Files */

    /**
     * @param yyyymm Month in YYYY-MM format.
     * @param d
     */
    public JsonArray filesTimeSeries(Dataverse d) {
        Query query = em.createNativeQuery(
                "select distinct date, count(id)\n"
                        + "from (\n"
                        + "select min(to_char(COALESCE(releasetime, createtime), 'YYYY-MM')) as date, filemetadata.id as id\n"
                        + "from datasetversion, filemetadata\n"
                        + "where datasetversion.id=filemetadata.datasetversion_id\n"
                        + "and versionstate='RELEASED' \n"
                        + "and dataset_id in (select dataset.id from dataset, dvobject where dataset.id=dvobject.id\n"
                        + "and dataset.harvestingclient_id IS NULL and publicationdate is not null\n "
                        + ((d == null) ? ")" : "and dvobject.owner_id in (" + getCommaSeparatedIdStringForSubtree(d, "Dataverse") + "))\n ")
                        + "group by filemetadata.id) as subq group by subq.date order by date;");
        logger.log(Level.FINE, "Metric query: {0}", query);
        List<Object[]> results = query.getResultList();
        return MetricsUtil.timeSeriesToJson(results);
    }

    
    /**
     * @param yyyymm Month in YYYY-MM format.
     * @param d
     */
    public long filesToMonth(String yyyymm, Dataverse d) {
        Query query = em.createNativeQuery(""
                + "select count(*)\n"
                + "from filemetadata\n"
                + "join datasetversion on datasetversion.id = filemetadata.datasetversion_id\n"
                + "where datasetversion.dataset_id || ':' || datasetversion.versionnumber + (.1 * datasetversion.minorversionnumber) in \n"
                + "(\n"
                + "select datasetversion.dataset_id || ':' || max(datasetversion.versionnumber + (.1 * datasetversion.minorversionnumber)) as max \n"
                + "from datasetversion\n"
                + "join dataset on dataset.id = datasetversion.dataset_id\n"
                + ((d == null) ? "" : "join dvobject on dvobject.id = dataset.id\n")
                + "where versionstate='RELEASED'\n"
                + ((d == null) ? "" : "and dvobject.owner_id in (" + getCommaSeparatedIdStringForSubtree(d, "Dataverse") + ")\n")
                + "and date_trunc('month', releasetime) <=  to_date('" + yyyymm + "','YYYY-MM')\n"
                + "and dataset.harvestingclient_id is null\n"
                + "group by dataset_id \n"
                + ");"
        );
        logger.log(Level.FINE, "Metric query: {0}", query);

        return (long) query.getSingleResult();
    }

    public long filesPastDays(int days, Dataverse d) {
        Query query = em.createNativeQuery(""
                + "select count(*)\n"
                + "from filemetadata\n"
                + "join datasetversion on datasetversion.id = filemetadata.datasetversion_id\n"
                + "where datasetversion.dataset_id || ':' || datasetversion.versionnumber + (.1 * datasetversion.minorversionnumber) in \n"
                + "(\n"
                + "select datasetversion.dataset_id || ':' || max(datasetversion.versionnumber + (.1 * datasetversion.minorversionnumber)) as max \n"
                + "from datasetversion\n"
                + "join dataset on dataset.id = datasetversion.dataset_id\n"
                + ((d == null) ? "" : "join dvobject on dvobject.id = dataset.id\n")
                + "where versionstate='RELEASED'\n"
                + "and releasetime > current_date - interval '" + days + "' day\n"
                + ((d == null) ? "" : "AND dvobject.owner_id in (" + getCommaSeparatedIdStringForSubtree(d, "Dataverse") + ")\n")
                + "and dataset.harvestingclient_id is null\n"
                + "group by dataset_id \n"
                + ");"
        );
        logger.log(Level.FINE, "Metric query: {0}", query);

        return (long) query.getSingleResult();
    }


    public JsonArray filesByType(Dataverse d) {
        // SELECT DISTINCT df.contenttype, sum(df.filesize) FROM datafile df, dvObject ob where ob.id = df.id and dob.owner_id< group by df.contenttype
        // ToDo - published only?
        Query query = em.createNativeQuery("SELECT DISTINCT df.contenttype, count(df.id), coalesce(sum(df.filesize), 0) "
                + " FROM DataFile df, DvObject ob"
                + " where ob.id = df.id "
                + ((d == null) ? "" : "and ob.owner_id in (" + getCommaSeparatedIdStringForSubtree(d, "Dataset") + ")\n")
                + "group by df.contenttype;");
        JsonArrayBuilder jab = Json.createArrayBuilder();
        try {
            List<Object[]> results = query.getResultList();
            for (Object[] result : results) {
                if((BigDecimal)result[2]==BigDecimal.ZERO) {
                    logger.warning("File(s) of type " + (String) result[0] + " are reported as having 0 total size");
                }
                JsonObject stats = Json.createObjectBuilder().add(MetricsUtil.CONTENTTYPE, (String) result[0]).add(MetricsUtil.COUNT, (long) result[1]).add(MetricsUtil.SIZE, (BigDecimal) result[2]).build();
                jab.add(stats);
            }

        } catch (javax.persistence.NoResultException nr) {
            // do nothing
        }
        return jab.build();

    }
    
    public JsonArray filesByTypeTimeSeries(Dataverse d, boolean published) {
        Query query = em.createNativeQuery("SELECT DISTINCT to_char(" + (published ? "ob.publicationdate" : "ob.createdate") + ",'YYYY-MM') as date, df.contenttype, count(df.id), coalesce(sum(df.filesize),0) "
                + " FROM DataFile df, DvObject ob"
                + " where ob.id = df.id "
                + (published ? "and publicationdate is not null\n" : " and createdate is not null\n")
                + ((d == null) ? "" : "and ob.owner_id in (" + getCommaSeparatedIdStringForSubtree(d, "Dataset") + ")\n")
                + "group by df.contenttype,\n"
                + (published ? " to_char(ob.publicationdate,'YYYY-MM') order by  to_char(ob.publicationdate,'YYYY-MM');" : " to_char(ob.createdate,'YYYY-MM') order by  to_char(ob.createdate,'YYYY-MM');")
                );
        logger.log(Level.FINE, "Metric query: {0}", query);
        List<Object[]> results = query.getResultList();
        return MetricsUtil.timeSeriesByTypeToJson(results);
        
    }
    /** Downloads 
     * @param d
     * @throws ParseException */

    
    public JsonArray downloadsTimeSeries(Dataverse d) {
        // ToDo - published only?
        Query earlyDateQuery = em.createNativeQuery(""
                + "select responsetime from guestbookresponse\n"
                + "ORDER BY responsetime LIMIT 1;");

        Timestamp earlyDateTimestamp = (Timestamp) earlyDateQuery.getSingleResult();
        LocalDate earliestDate = earlyDateTimestamp.toLocalDateTime().toLocalDate().minusMonths(1);
        String earliest = earliestDate.format(DateTimeFormatter.ofPattern(YEAR_AND_MONTH_PATTERN));

        // Counts historic guestbook records without date as occurring in the month
        // prior to the first dated counts
        Query query = em.createNativeQuery(""
                + "select  distinct COALESCE(to_char(responsetime, 'YYYY-MM'),'" + earliest + "') as date, count(id)\n"
                + "from guestbookresponse\n"
                + ((d == null) ? "" : "where dataset_id in (" + getCommaSeparatedIdStringForSubtree(d, "Dataset") + ")")
                + " group by COALESCE(to_char(responsetime, 'YYYY-MM'),'" + earliest + "') order by  COALESCE(to_char(responsetime, 'YYYY-MM'),'" + earliest + "');");

        logger.log(Level.FINE, "Metric query: {0}", query);
        List<Object[]> results = query.getResultList();
        return MetricsUtil.timeSeriesToJson(results);
    }
    
    /*
     * This includes getting historic download without a timestamp if query
     * is earlier than earliest timestamped record
     * 
     * @param yyyymm Month in YYYY-MM format.
     */
    public long downloadsToMonth(String yyyymm, Dataverse d) throws ParseException {
        // ToDo - published only?
        Query earlyDateQuery = em.createNativeQuery(""
                + "select responsetime from guestbookresponse\n"
               + "ORDER BY responsetime LIMIT 1;"
        );

        try {
            Timestamp earlyDateTimestamp = (Timestamp) earlyDateQuery.getSingleResult();
            Date earliestDate = new Date(earlyDateTimestamp.getTime());

            Date dateQueried = yyyymmFormat.parse(yyyymm);

            if (!dateQueried.before(earliestDate)) {
                Query query = em.createNativeQuery(""
                        + "select count(id)\n"
                        + "from guestbookresponse\n"
                        + "where (date_trunc('month', responsetime) <=  to_date('" + yyyymm + "','YYYY-MM')"
                        + "or responsetime is NULL)\n" // includes historic guestbook records without date
                    + ((d==null) ? ";": "AND dataset_id in (" + getCommaSeparatedIdStringForSubtree(d, "Dataset") + ");") 
                );
                logger.log(Level.FINE, "Metric query: {0}", query);
                return (long) query.getSingleResult();
            } else {
                // When we query before the earliest dated record, return 0;
                return 0L;
            }
        } catch (NoResultException e) {
            //If earlyDateQuery.getSingleResult is null, then there are no guestbooks and we can return 0
            return 0L;
        }

    }

    public long downloadsPastDays(int days, Dataverse d) {
        // ToDo - published only?
        Query query = em.createNativeQuery(""
                + "select count(id)\n"
                + "from guestbookresponse\n"
                + "where responsetime > current_date - interval '" + days + "' day\n"
                + ((d==null) ? ";": "AND dataset_id in (" + getCommaSeparatedIdStringForSubtree(d, "Dataset") + ");")
        );
        logger.log(Level.FINE, "Metric query: {0}", query);

        return (long) query.getSingleResult();
    }
    
    public JsonArray fileDownloadsTimeSeries(Dataverse d, boolean uniqueCounts) {
        Query query = em.createNativeQuery("select distinct to_char(gb.responsetime, 'YYYY-MM') as date, ob.id, ob.protocol || ':' || ob.authority || '/' || ob.identifier as pid, count(" + (uniqueCounts ? "distinct email" : "*") + ") "
                + " FROM guestbookresponse gb, DvObject ob"
                + " where ob.id = gb.datafile_id "
                + ((d == null) ? "" : " and ob.owner_id in (" + getCommaSeparatedIdStringForSubtree(d, "Dataset") + ")\n")
                + "group by gb.datafile_id, ob.id, ob.protocol, ob.authority, ob.identifier, to_char(gb.responsetime, 'YYYY-MM') order by to_char(gb.responsetime, 'YYYY-MM');");

        logger.log(Level.FINE, "Metric query: {0}", query);
        List<Object[]> results = query.getResultList();
        return MetricsUtil.timeSeriesByIDAndPIDToJson(results);

    }
    
    public JsonArray fileDownloads(String yyyymm, Dataverse d, boolean uniqueCounts) {
        Query query = em.createNativeQuery("select ob.id, ob.protocol || ':' || ob.authority || '/' || ob.identifier as pid, count(" + (uniqueCounts ? "distinct email" : "*") + ") "
                + " FROM guestbookresponse gb, DvObject ob"
                + " where ob.id = gb.datafile_id "
                + ((d == null) ? "" : " and ob.owner_id in (" + getCommaSeparatedIdStringForSubtree(d, "Dataset") + ")\n")
                + " and date_trunc('month', gb.responsetime) <=  to_date('" + yyyymm + "','YYYY-MM')\n"
                + "group by gb.datafile_id, ob.id, ob.protocol, ob.authority, ob.identifier order by count desc;");

        logger.log(Level.FINE, "Metric query: {0}", query);
        JsonArrayBuilder jab = Json.createArrayBuilder();
        try {
            List<Object[]> results = query.getResultList();
            for (Object[] result : results) {
                JsonObjectBuilder job = Json.createObjectBuilder();
                job.add(MetricsUtil.ID, (int) result[0]);
                if(result[1]!=null) {
                    job.add(MetricsUtil.PID, (String) result[1]);
                }
                job.add(MetricsUtil.COUNT, (long) result[2]);
                jab.add(job);
            }
        } catch (javax.persistence.NoResultException nr) {
            // do nothing
        }
        return jab.build();
    }

    public JsonArray uniqueDownloadsTimeSeries(Dataverse d) {
        Query query = em.createNativeQuery("select distinct to_char(gb.responsetime, 'YYYY-MM') as date, ob.protocol || ':' || ob.authority || '/' || ob.identifier as pid, count(distinct email) "
                + " FROM guestbookresponse gb, DvObject ob"
                + " where ob.id = gb.dataset_id "
                + ((d == null) ? "" : " and ob.owner_id in (" + getCommaSeparatedIdStringForSubtree(d, "Dataverse") + ")\n")
                + "group by gb.dataset_id, ob.protocol, ob.authority, ob.identifier, to_char(gb.responsetime, 'YYYY-MM') order by to_char(gb.responsetime, 'YYYY-MM');");

        logger.log(Level.FINE, "Metric query: {0}", query);
        List<Object[]> results = query.getResultList();
        return MetricsUtil.timeSeriesByPIDToJson(results);

    }
    
    public JsonArray uniqueDatasetDownloads(String yyyymm, Dataverse d) {

    //select distinct count(distinct email),dataset_id, date_trunc('month', responsetime)  from guestbookresponse group by dataset_id, date_trunc('month',responsetime) order by dataset_id,date_trunc('month',responsetime);

        Query query = em.createNativeQuery("select ob.protocol || ':' || ob.authority || '/' || ob.identifier as pid, count(distinct email) "
                + " FROM guestbookresponse gb, DvObject ob"
                + " where ob.id = gb.dataset_id "
                + ((d == null) ? "" : " and ob.owner_id in (" + getCommaSeparatedIdStringForSubtree(d, "Dataverse") + ")\n")
                + " and date_trunc('month', responsetime) <=  to_date('" + yyyymm + "','YYYY-MM')\n"
                + "group by gb.dataset_id, ob.protocol, ob.authority, ob.identifier order by count(distinct email) desc;");
        JsonArrayBuilder jab = Json.createArrayBuilder();
        try {
            List<Object[]> results = query.getResultList();
            for (Object[] result : results) {
                JsonObjectBuilder job = Json.createObjectBuilder();
                job.add(MetricsUtil.PID, (String) result[0]);
                job.add(MetricsUtil.COUNT, (long) result[1]);
                jab.add(job);
            }

        } catch (javax.persistence.NoResultException nr) {
            // do nothing
        }
        return jab.build();

    }
    
    //MDC 
    
    
    public JsonArray mdcMetricTimeSeries(MetricType metricType, String country, Dataverse d) {
        Query query = em.createNativeQuery("SELECT distinct substring(monthyear from 1 for 7) as date, coalesce(sum(" + metricType.toString() + "),0) as count FROM DatasetMetrics\n"
                + ((d == null) ? "" : "WHERE dataset_id in ( " + getCommaSeparatedIdStringForSubtree(d, "Dataset") + ")\n")
                + ((country == null) ? "" : ((d == null) ? "WHERE " : "and ") + "countryCode = '" + country + "'")
                + " group by substring(monthyear from 1 for 7) order by substring(monthyear from 1 for 7);"
                );
        logger.log(Level.FINE, "Metric query: {0}", query);
        List<Object[]> results = query.getResultList();
        return MetricsUtil.timeSeriesToJson(results, true);
    }

    public JsonObject getMDCDatasetMetrics(MetricType metricType, String yyyymm, String country, Dataverse d) {
        String queryStr = "SELECT coalesce(sum(" + metricType.toString() + "),0) as count FROM DatasetMetrics\n"
                + ((d == null) ? "WHERE " : "WHERE dataset_id in ( " + getCommaSeparatedIdStringForSubtree(d, "Dataset") + ") and\n")
                + " monthYear <= '" + yyyymm + "' "
                + ((country == null) ? ";" : " and countryCode = '" + country + "';");
        logger.info("final query: " + queryStr);

        Query query = em.createNativeQuery(queryStr);
        BigDecimal sum = (BigDecimal) query.getSingleResult();
        // if(sum==null) {
        // sum = BigDecimal.ZERO;
        // }
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add(metricType.toString(), sum.longValue());
        return job.build();
    }

    /** Helper functions for metric caching */

    public String returnUnexpiredCacheDayBased(String metricName, String days, String dataLocation, Dataverse d) {
        Metric queriedMetric = getMetric(metricName, dataLocation, days, d);

        if (!doWeQueryAgainDayBased(queriedMetric)) {
            return queriedMetric.getValueJson();
        }
        return null;
    }

    public String returnUnexpiredCacheMonthly(String metricName, String yyyymm, String dataLocation, Dataverse d) {
        Metric queriedMetric = getMetric(metricName, dataLocation, yyyymm, d);

        if (!doWeQueryAgainMonthly(queriedMetric)) {
            return queriedMetric.getValueJson();
        }
        return null;
    }

    public String returnUnexpiredCacheAllTime(String metricName, String dataLocation, Dataverse d) {
        Metric queriedMetric = getMetric(metricName, dataLocation, null, d); // MAD: not passing a date

        if (!doWeQueryAgainAllTime(queriedMetric)) {
            return queriedMetric.getValueJson();
        }
        return null;
    }

    // For day based metrics we check to see if the metric has been pulled today
    public boolean doWeQueryAgainDayBased(Metric queriedMetric) {
        if (null == queriedMetric) { // never queried before
            return true;
        }

        LocalDate lastCalled = LocalDate.from(queriedMetric.getLastCalledDate().toInstant().atZone(ZoneId.systemDefault()));
        LocalDate todayDate = LocalDate.now(ZoneId.systemDefault());

        if (!lastCalled.equals(todayDate)) {
            return true;
        } else {
            return false;
        }
    }

    // This is for deciding whether to used a cached value on monthly queries
    //Assumes the metric passed in is sane (e.g. not run for past the current month, not a garbled date string, etc)
    public boolean doWeQueryAgainMonthly(Metric queriedMetric) {
        if (null == queriedMetric) { // never queried before
            return true;
        }

        String yyyymm = queriedMetric.getDateString();
        String thisMonthYYYYMM = MetricsUtil.getCurrentMonth();

        Date lastCalled = queriedMetric.getLastCalledDate();
        LocalDateTime ldt = LocalDateTime.ofInstant((new Date()).toInstant(), ZoneId.systemDefault());

        int minutesUntilNextQuery = systemConfig.getMetricsCacheTimeoutMinutes();

        if (yyyymm.equals(thisMonthYYYYMM)) { // if this month
            LocalDateTime ldtMinus = ldt.minusMinutes(minutesUntilNextQuery);
            Date todayMinus = Date.from(ldtMinus.atZone(ZoneId.systemDefault()).toInstant());

            // allow if today minus query wait is after last called time
            return (todayMinus.after(lastCalled));
        } else {
            String lastRunYYYYMM = yyyymmFormat.format(lastCalled);

            // if queried was last run during the month it was querying.
            // Allows requery of a past month to make it up to date.
            return (lastRunYYYYMM.equals(yyyymm));
        }
    }

    // This is for deciding whether to used a cached value over all time
    public boolean doWeQueryAgainAllTime(Metric queriedMetric) {
        if (null == queriedMetric) { // never queried before
            return true;
        }

        int minutesUntilNextQuery = systemConfig.getMetricsCacheTimeoutMinutes();
        Date lastCalled = queriedMetric.getLastCalledDate();
        LocalDateTime ldt = LocalDateTime.ofInstant((new Date()).toInstant(), ZoneId.systemDefault());

        LocalDateTime ldtMinus = ldt.minusMinutes(minutesUntilNextQuery);
        Date todayMinus = Date.from(ldtMinus.atZone(ZoneId.systemDefault()).toInstant());

        // allow if today minus query wait is after last called time
        return (todayMinus.after(lastCalled));
    }

    public Metric save(Metric newMetric) {
        Metric oldMetric = getMetric(newMetric.getName(), newMetric.getDataLocation(), newMetric.getDateString(), newMetric.getDataverse());

        if (oldMetric != null) {
            em.remove(oldMetric);
            em.flush();
        }
        em.persist(newMetric);
        return em.merge(newMetric);
    }

    // This works for date and day based metrics
    // It is ok to pass null for dataLocation and dayString
    public Metric getMetric(String name, String dataLocation, String dayString, Dataverse dataverse) {
        Query query = em.createQuery("select object(o) from Metric as o"
                + " where o.name = :name"
                + " and o.dataLocation" + (dataLocation == null ? " is null" : " = :dataLocation")
                + " and o.dayString" + (dayString == null ? " is null" : " = :dayString")
                + (dataverse == null ? " and o.dataverse is null" :  " and o.dataverse.id = :dataverseId")
                , Metric.class);
        query.setParameter("name", name);
        if(dataLocation != null){ query.setParameter("dataLocation", dataLocation);}
        if(dayString != null) {query.setParameter("dayString", dayString);}
        if(dataverse != null) {query.setParameter("dataverseId", dataverse.getId());}

        logger.log(Level.FINE, "getMetric query: {0}", query);

        Metric metric = null;
        try {
            metric = (Metric) query.getSingleResult();
        } catch (javax.persistence.NoResultException nr) {
            // do nothing
            logger.fine("No result");
        } catch (NonUniqueResultException nur) {
            //duplicates can happen when a new/requeried metric is called twice and saved twice before one can use the cache
            // this remove all but the 0th index one in that case
            for (int i = 1; i < query.getResultList().size(); i++) {
                Metric extraMetric = (Metric) query.getResultList().get(i);
                em.remove(extraMetric);
                em.flush();
            }
            metric = (Metric) query.getResultList().get(0);
        }
        logger.fine("returning: " + ((metric == null) ? "Null" : metric.getValueJson()));
        return metric;
    }

    // Modified from DANS
    // https://github.com/DANS-KNAW/dataverse/blob/dans-develop/src/main/java/edu/harvard/iq/dataverse/metrics/MetricsDansServiceBean.java

    /**
     * 
     * @param dvId - parent dataverse id
     * @param dtype - type of object to return 'Dataverse' or 'Dataset'
     * @return - list of objects of specified type included in the subtree (includes parent dataverse if dtype is 'Dataverse')
     */
    private String getCommaSeparatedIdStringForSubtree(Dataverse d, String dtype) {
        /* Currently limited to returning published items (non-null publicationdate)
         * To support queries of draft/other states, this method would have to be updated
         */
        // Start with parentID if returning a subtree of dataverses
        String idString = (dtype.equals("Dataverse")) ? (d.isReleased() ? Long.toString(d.getId()) : "null") : "null";

        String[] dvObjectIds = Arrays.stream(getChildrenIdsRecursively(d.getId(), dtype, DatasetVersion.VersionState.RELEASED).stream().mapToInt(i -> i).toArray())
                .mapToObj(String::valueOf).toArray(String[]::new);
        if (dvObjectIds.length != 0) {
            // Append parent if returning Dataverses
            idString = String.join(",", dvObjectIds) + ((dtype.equals("Dataverse")) ? ("," + idString) : "");
        }
        return idString;
    }

    private List<Integer> getChildrenIdsRecursively(Long dvId, String dtype, DatasetVersion.VersionState versionState) {
        
        //Intended to be called only with dvId != null
        String sql = "WITH RECURSIVE querytree AS (\n"
                + "     SELECT id, dtype, owner_id, publicationdate\n"
                + "     FROM dvobject\n"
                + "     WHERE id = " + dvId + "\n"
                + "     UNION ALL\n"
                + "     SELECT e.id, e.dtype, e.owner_id, e.publicationdate\n"
                + "     FROM dvobject e\n"
                + "     INNER JOIN querytree qtree ON qtree.id = e.owner_id\n"
                + ")\n"
                + "SELECT id\n"
                + "FROM querytree\n"
                + "where dtype='" + dtype + "' and owner_id is not null\n";
        //TODO: DEACCESSIONED datasets still have a publication date - should check versionstate explicitly?
        if (versionState != null) {
            switch (versionState) {
            case RELEASED:
                sql += "and publicationdate is not null\n";
                break;
            case DRAFT:
                sql += "and publicationdate is null\n";
                break;
            }
        }
        sql += ";";

        logger.fine("query  - (" + dvId + ") - getChildrenIdsRecursively: " + sql);
        return em.createNativeQuery(sql).getResultList();
    }

    public JsonObject getDataverseTree(Dataverse d, String yyyymm, DatasetVersion.VersionState state) {
        List<Object[]> results = getDataversesChildrenRecursively(d, yyyymm, state);
        HashMap<Long, JsonArray> subtrees = new HashMap<Long, JsonArray>();
        // Relying on the depth-first order
        // id, depth, alias, name, ownerid
        // (String)objs[2], (int)objs[0], (int) (long)objs[4], (int)objs[1],
        // (String)objs[3]));
        int currentDepth = Integer.MAX_VALUE;
        long currentOwnerId = -1;
        JsonArrayBuilder children = null;
        for (Object[] result : results) {
            int depth = (int) result[1];
            long ownerId = (long) result[4];
            long id = (int) result[0];
            JsonObjectBuilder node = Json.createObjectBuilder()
                    .add("id", id)
                    .add("ownerId", ownerId)
                    .add("alias", (String) result[2])
                    .add("depth", depth)
                    .add("name", (String) result[3]);
            if (ownerId != currentOwnerId) {
                // Add current array of children to map and start a new one
                if (children != null) {
                    subtrees.put(currentOwnerId, children.build());
                }
                children = Json.createArrayBuilder();
                currentOwnerId = ownerId;
            }
            if (subtrees.containsKey(id)) {
                node.add("children", subtrees.get(id));
                subtrees.remove(id);
            }
            children.add(node);
        }
        return (JsonObject) children.build().get(0);
    }

    private List<Object[]> getDataversesChildrenRecursively(Dataverse d, String yyyymm, DatasetVersion.VersionState versionState) {
        String sql = "WITH RECURSIVE querytree AS (\n"
                + "     SELECT id, dtype, owner_id, publicationdate, 0 as depth\n"
                + "     FROM dvobject\n"
                + "     WHERE id =" + ((d == null) ? "1" : d.getId() + " \n")
                + "     UNION ALL\n"
                + "     SELECT e.id, e.dtype, e.owner_id, e.publicationdate, depth+ 1\n"
                + "     FROM dvobject e\n"
                + "     INNER JOIN querytree qtree ON qtree.id = e.owner_id\n"
                + ")\n"
                + "SELECT qt.id, depth, dv.alias, dv.name, coalesce(qt.owner_id,0) as ownerId\n"
                + "FROM querytree qt, dataverse dv\n"
                + "where dtype='Dataverse'\n"
                + "and qt.id=dv.id\n";

        //TODO: DEACCESSIONED datasets still have a publication date - should check versionstate explicitly?
        if (versionState != null) {
            switch (versionState) {
            case RELEASED:
                sql += " and date_trunc('month', publicationdate) <=  to_date('" + yyyymm + "','YYYY-MM')\n";
                break;
            case DRAFT:
                sql += " and date_trunc('month', createdate) <=  to_date('" + yyyymm + "','YYYY-MM')\n";
                break;
            }
        }
        sql = sql + "order by depth desc, ownerId asc;";

        logger.fine("query  - getDataversesChildrenRecursively: " + sql);
        return em.createNativeQuery(sql).getResultList();
    }

}
