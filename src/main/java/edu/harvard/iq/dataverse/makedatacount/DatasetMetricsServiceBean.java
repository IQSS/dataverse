
package edu.harvard.iq.dataverse.makedatacount;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.logging.Logger;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.ejb.Stateless;
import jakarta.inject.Named;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 *
 * @author skraffmi
 */
@Named
@Stateless
public class DatasetMetricsServiceBean implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(DatasetMetricsServiceBean.class.getCanonicalName());
    
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    protected EntityManager em;
    
    @EJB
    DatasetServiceBean datasetService;
    
    public DatasetMetrics getDatasetMetricsByDatasetMonthCountry(Dataset dataset, String monthYear, String country) {
        DatasetMetrics dsm = null;
        String queryStr = "SELECT d FROM DatasetMetrics d WHERE d.dataset.id = " + dataset.getId() + " and d.monthYear = '" + monthYear + "' " + " and d.countryCode = '" + country + "' ";
        Query query = em.createQuery(queryStr);
        List resultList = query.getResultList();
        if (resultList.size() > 1) {
            throw new EJBException("More than one Dataset Metric found in the dataset (id= " + dataset.getId() + "), with monthYear= " + monthYear + " and Country code = " + country  + ".");
        }
        if (resultList.size() == 1) {
            dsm = (DatasetMetrics) resultList.get(0);
            dsm.setViewsTotal(dsm.getViewsTotalRegular() + dsm.getViewsTotalMachine());
            dsm.setViewsUnique(dsm.getViewsUniqueRegular() + dsm.getViewsUniqueMachine());
            dsm.setDownloadsTotal(dsm.getDownloadsTotalRegular() + dsm.getDownloadsTotalMachine());
            dsm.setDownloadsUnique(dsm.getDownloadsUniqueRegular() + dsm.getDownloadsUniqueMachine());
            return dsm;
        }
        return null;
    }
    
    public DatasetMetrics getMetrics(Dataset dataset) {
        String nullMonthYear = null;
        String nullCountry = null;
        return getDatasetMetricsByDatasetForDisplay(dataset, nullMonthYear, nullCountry);
    }

    public DatasetMetrics getDatasetMetricsByDatasetForDisplay(Dataset dataset, String monthYear, String country) {

        Long dataset_id = dataset.getId();

        String whereClause = " where dataset_id = " + dataset_id.toString() + " ";

        if (monthYear != null) {
            whereClause += "and monthYear = '" + monthYear + "' ";
        }
        if (country != null) {
            whereClause += "and countryCode = '" + country + "' ";
        }

        Query query = em.createNativeQuery(""
                + "select sum(viewstotalregular), sum(viewsuniqueregular), sum(downloadstotalregular), "
                + "sum(downloadsuniqueregular),sum(viewstotalmachine), sum(viewsuniquemachine), sum(downloadstotalmachine), sum(downloadsuniquemachine)  "
                + "from datasetmetrics \n"
                + whereClause
                + ";"
        );

        Object[] row = (Object[]) query.getSingleResult();

        DatasetMetrics dm = new DatasetMetrics();
        dm.setDataset(dataset);
        dm.setCountryCode(country);
        dm.setMonth(monthYear);
        dm.setViewsTotalRegular(row[0] == null ? 0 : ((BigDecimal) row[0]).longValue());
        dm.setViewsUniqueRegular( row[1] == null ? 0 : ((BigDecimal) row[1]).longValue());
        dm.setDownloadsTotalRegular(row[2] == null ? 0 : ((BigDecimal) row[2]).longValue());
        dm.setDownloadsUniqueRegular(row[3] == null ? 0 : ((BigDecimal) row[3]).longValue());
        dm.setViewsTotalMachine(row[4] == null ? 0 : ((BigDecimal) row[4]).longValue());
        dm.setViewsUniqueMachine(row[5] == null ? 0 : ((BigDecimal) row[5]).longValue());
        dm.setDownloadsTotalMachine(row[6] == null ? 0 : ((BigDecimal) row[6]).longValue());
        dm.setDownloadsUniqueMachine(row[7] == null ? 0 : ((BigDecimal) row[7]).longValue());
        dm.setViewsTotal(dm.getViewsTotalRegular() + dm.getViewsTotalMachine());
        dm.setViewsUnique(dm.getViewsUniqueRegular() + dm.getViewsUniqueMachine());
        dm.setDownloadsTotal(dm.getDownloadsTotalRegular() + dm.getDownloadsTotalMachine());
        dm.setDownloadsUnique(dm.getDownloadsUniqueRegular() + dm.getDownloadsUniqueMachine());

        return dm;

    }
        
    public List<DatasetMetrics> parseSushiReport(JsonObject report){
        return parseSushiReport(report, null);
    }
    
    
    public List<DatasetMetrics> parseSushiReport(JsonObject report, Dataset dataset) {
        List<DatasetMetrics> datasetMetricsAll = new ArrayList<>();
        //Current counter-processor v 0.1.04+ format
        JsonArray reportDatasets = report.getJsonArray("report-datasets");
        if(reportDatasets==null) {
            //Try counter-processor v 0.0.1 name
            reportDatasets = report.getJsonArray("report_datasets");
        }
        for (JsonValue reportDataset : reportDatasets) {
            List<DatasetMetrics> datasetMetricsDataset = new ArrayList<>();
            String globalId = null;
            Dataset ds = null;
            StringReader rdr = new StringReader(reportDataset.toString());
            JsonReader jrdr = Json.createReader(rdr);
            JsonObject obj = jrdr.readObject();
            String jsonGlobalId = "";
            String globalIdType = "";
            if (obj.containsKey("dataset-id")) {
                JsonArray dsIdArray = obj.getJsonArray("dataset-id");
                JsonObject idObj = dsIdArray.getJsonObject(0);
                jsonGlobalId = idObj.getString("value");
                globalIdType = idObj.getString("type");
                globalId = globalIdType + ":" + jsonGlobalId;
            }
            if (dataset != null) {
                ds = dataset;
            } else {
                if (globalId != null) {
                    ds = datasetService.findByGlobalId(globalId);
                }
                if (ds == null) {
                    continue;
                }
            }
            if (obj.containsKey("performance")) {
                JsonArray performance = obj.getJsonArray("performance");
                for (JsonObject perfObj : performance.getValuesAs(JsonObject.class)) {
                    String monthYear = "";
                    JsonObject period = perfObj.getJsonObject("period");
                    monthYear = period.getString("begin-date");
                    JsonArray instanceArray = perfObj.getJsonArray("instance");
                    //"access-method": "regular", 
                    for (JsonObject instObj : instanceArray.getValuesAs(JsonObject.class)) {
                        List<DatasetMetrics> datasetMetricsList = new ArrayList<>();
                        String metricType = instObj.getString("metric-type");
                        String accessMethod = instObj.getString("access-method");
                        Long totalCount = new Long(instObj.getInt("count"));
                        List<String[]> countArray = new ArrayList<>();
                        if (instObj.containsKey("country-counts")) {
                            JsonObject countryCountObj = instObj.getJsonObject("country-counts");
                            countArray = getCountryCountArray(countryCountObj);
                        }

                        for (String[] row : countArray) {
                            DatasetMetrics dm = new DatasetMetrics();
                            dm.initCounts();
                            dm.setDataset(ds);
                            dm.setCountryCode(row[0]);
                            dm.setMonth(monthYear);
                            Long count = new Long(row[1]);
                            dm = loadMetrics(dm, count, accessMethod, metricType);
                            datasetMetricsList.add(dm);
                            totalCount -= count;
                        }

                        if (totalCount.intValue() > 0) {
                            DatasetMetrics ncDm = addNoCountryMetric(ds, accessMethod, metricType, totalCount, monthYear);
                            datasetMetricsList.add(ncDm);
                        }
                        datasetMetricsDataset = addUpdateMetrics(datasetMetricsDataset, datasetMetricsList, metricType, accessMethod);

                    }
                }
            }
            datasetMetricsAll.addAll(datasetMetricsDataset);
        }
        return datasetMetricsAll;
    }
    
    private List<String[]> getCountryCountArray(JsonObject countryCountObj) {
        List<String[]> retList = new ArrayList<>();
        Set<String> keyValuePair = countryCountObj.keySet();
        for (String key : keyValuePair) {
            Integer value = countryCountObj.getInt(key);
            String countryCode = key;
            String[] datasetContributor = new String[]{countryCode, value.toString()};
            retList.add(datasetContributor);
        }
        return retList;
    }
    
    private DatasetMetrics loadMetrics(DatasetMetrics dmIn, Long count, String accessMethod, String metricType) {

        if (accessMethod.equals("regular")) {
            switch (metricType) {
                case "total-dataset-investigations":
                    dmIn.setViewsTotalRegular(count);
                    break;
                case "unique-dataset-investigations":
                    dmIn.setViewsUniqueRegular(count);
                    break;
                case "total-dataset-requests":
                    dmIn.setDownloadsTotalRegular(count);
                    break;
                case "unique-dataset-requests":
                    dmIn.setDownloadsUniqueRegular(count);
                    break;
            }
        } else {
            switch (metricType) {
                case "total-dataset-investigations":
                    dmIn.setViewsTotalMachine(count);
                    break;
                case "unique-dataset-investigations":
                    dmIn.setViewsUniqueMachine(count);
                    break;
                case "total-dataset-requests":
                    dmIn.setDownloadsTotalMachine(count);
                    break;
                case "unique-dataset-requests":
                    dmIn.setDownloadsUniqueMachine(count);
                    break;
            }
        }
        return dmIn;
    }
    
    private DatasetMetrics addNoCountryMetric(Dataset ds, String accessMethod, String metricType, Long remaining, String monthYear) {
        DatasetMetrics dm = new DatasetMetrics();
        dm.initCounts();
        dm.setDataset(ds);
        dm.setCountryCode("");
        dm.setMonth(monthYear);
        dm = loadMetrics(dm, remaining, accessMethod,  metricType ); 
        return dm;
    }
    
    private List<DatasetMetrics> addUpdateMetrics(List<DatasetMetrics> currentList, List<DatasetMetrics> compareList, String countField, String accessMethod){
        
        List<DatasetMetrics> toAdd = new ArrayList();
        
        for (DatasetMetrics testMetric : compareList) {
            
            boolean add = true;
            ListIterator<DatasetMetrics> iterator = currentList.listIterator();
            while (iterator.hasNext()) {
                DatasetMetrics next = iterator.next();
                if (next.getCountryCode().equals(testMetric.getCountryCode())) {
                    //Replace element      
                    if (countField.equals("total-dataset-investigations")){
                        if(accessMethod.equals("regular")){
                            next.setViewsTotalRegular(testMetric.getViewsTotalRegular());
                        } else {
                            next.setViewsTotalMachine(testMetric.getViewsTotalMachine());
                        }                      
                    }
                    if (countField.equals("unique-dataset-investigations")){
                        if(accessMethod.equals("regular")){
                            next.setViewsUniqueRegular(testMetric.getViewsUniqueRegular());
                        } else {
                            next.setViewsUniqueMachine(testMetric.getViewsUniqueMachine());
                        }
                    }                    
                    if (countField.equals("total-dataset-requests")){
                        if(accessMethod.equals("regular")){
                            next.setDownloadsTotalRegular(testMetric.getDownloadsTotalRegular());
                        } else {
                            next.setDownloadsTotalMachine(testMetric.getDownloadsTotalMachine());
                        }
                    }                    
                    if (countField.equals("unique-dataset-requests")){
                        if(accessMethod.equals("regular")){
                            next.setDownloadsUniqueRegular(testMetric.getDownloadsUniqueRegular());
                        } else {
                            next.setDownloadsUniqueMachine(testMetric.getDownloadsUniqueMachine());
                        }
                    }

                    iterator.set(next);
                    add = false;
                }
            }
            if(add){
               toAdd.add(testMetric);
            }
        }
        
        if(!toAdd.isEmpty()){
            currentList.addAll(toAdd);
        }
        
        return currentList;
    }
    
    public DatasetMetrics save(DatasetMetrics datasetMetrics) {  
        //Replace existing if necessary
        if(datasetMetrics.getDataset() == null){
            return null;
        }
        Dataset testDs =  datasetMetrics.getDataset();
        String testMonth = datasetMetrics.getMonthYear();
        String testCountry = datasetMetrics.getCountryCode();
        DatasetMetrics getExisting = getDatasetMetricsByDatasetMonthCountry(testDs, testMonth, testCountry);
        if (getExisting != null){
            em.remove(getExisting);
        }
        DatasetMetrics savedDatasetMetrics = em.merge(datasetMetrics);
        return savedDatasetMetrics;
    }
    
}
