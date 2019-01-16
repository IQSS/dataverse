
package edu.harvard.iq.dataverse.makedatacount;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.Dataverse;
import java.io.StringReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.concurrent.Future;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

/**
 *
 * @author skraffmi
 */
@Named
@Stateless
public class DatasetMetricsServiceBean implements java.io.Serializable {
    
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
            return dsm;
        }
        return null;
    }
    
    public List<DatasetMetrics> parseSushiReport(JsonObject report){
        return parseSushiReport(report, null);
    }
    
    
    public List<DatasetMetrics> parseSushiReport(JsonObject report, Dataset dataset) {
        List<DatasetMetrics> datasetMetricsAll = new ArrayList<>();
        JsonArray reportDatasets = report.getJsonArray("report_datasets");
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
            } else {
                System.out.print("Does Not Contain  dataset-id");
            }
            if (dataset != null){
                ds = dataset;
            } else {
                if (globalId != null){
                    ds = datasetService.findByGlobalId(globalId);
                }
            }
            if (obj.containsKey("performance")) {
                JsonArray performance = obj.getJsonArray("performance");
                for (JsonObject perfObj : performance.getValuesAs(JsonObject.class)) {
                    String monthYear = "";
                    JsonObject period = perfObj.getJsonObject("period");
                    monthYear = period.getString("begin-date");
                    JsonArray instanceArray = perfObj.getJsonArray("instance");
                    for (JsonObject instObj : instanceArray.getValuesAs(JsonObject.class)) {
                        if (instObj.getString("metric-type").equals("total-dataset-investigations")) { 
                            List<String[]> totalInvestigations = new ArrayList<>();
                            if (instObj.containsKey("country-counts")) {
                                JsonObject countryCountObj = instObj.getJsonObject("country-counts");
                                totalInvestigations = getCountryCountArray(countryCountObj);                               
                            }
                            List<DatasetMetrics> datasetMetricsTotal = new ArrayList<>();
                            if(!totalInvestigations.isEmpty()){
                               for(String[] investigation: totalInvestigations){
                                   DatasetMetrics dm = new DatasetMetrics();
                                   dm.setDataset(ds);
                                   dm.setCountryCode(investigation[0]);
                                   dm.setViewsTotal(new Long(investigation[1]));
                                   dm.setMonth(monthYear);
                                   datasetMetricsTotal.add(dm);
                               }
                            }
                            datasetMetricsDataset= addUpdateMetrics(datasetMetricsDataset, datasetMetricsTotal , "TotalViews");
                        }
                        if (instObj.getString("metric-type").equals("unique-dataset-investigations")) { //unique-dataset-investigations
                            List<String[]> uniqueInvestigations = new ArrayList<>();
                            if (instObj.containsKey("country-counts")) {
                                JsonObject countryCountObj = instObj.getJsonObject("country-counts");
                                uniqueInvestigations = getCountryCountArray(countryCountObj);                               
                            }
                            List<DatasetMetrics> datasetMetricsUnique = new ArrayList<>();
                            if(!uniqueInvestigations.isEmpty()){
                               for(String[] investigation: uniqueInvestigations){
                                   DatasetMetrics dm = new DatasetMetrics();
                                   dm.setDataset(ds);
                                   dm.setCountryCode(investigation[0]);
                                   dm.setViewsUnique(new Long(investigation[1]));
                                   dm.setMonth(monthYear);
                                   datasetMetricsUnique.add(dm);
                               }
                            }                           
                           datasetMetricsDataset= addUpdateMetrics(datasetMetricsDataset, datasetMetricsUnique , "UniqueViews");
                        }
                        if (instObj.getString("metric-type").equals("total-dataset-requests")) { //unique-dataset-investigations
                            List<String[]> totalRequests = new ArrayList<>();
                            if (instObj.containsKey("country-counts")) {
                                JsonObject countryCountObj = instObj.getJsonObject("country-counts");
                                totalRequests = getCountryCountArray(countryCountObj);                               
                            }
                            List<DatasetMetrics> datasetMetricsRequestsTotal = new ArrayList<>();
                            if(!totalRequests.isEmpty()){
                               for(String[] investigation: totalRequests){
                                   DatasetMetrics dm = new DatasetMetrics();
                                   dm.setDataset(ds);
                                   dm.setCountryCode(investigation[0]);
                                   dm.setDownloadsTotal(new Long(investigation[1]));
                                   dm.setMonth(monthYear);
                                   datasetMetricsRequestsTotal.add(dm);
                               }
                            }                           
                           datasetMetricsDataset= addUpdateMetrics(datasetMetricsDataset, datasetMetricsRequestsTotal , "TotalRequests");
                        }
                        if (instObj.getString("metric-type").equals("unique-dataset-requests")) { //unique-dataset-investigations
                            List<String[]> uniqueRequests = new ArrayList<>();
                            if (instObj.containsKey("country-counts")) {
                                JsonObject countryCountObj = instObj.getJsonObject("country-counts");
                                uniqueRequests = getCountryCountArray(countryCountObj);                               
                            }
                            List<DatasetMetrics> datasetMetricsRequestsTotal = new ArrayList<>();
                            if(!uniqueRequests.isEmpty()){
                               for(String[] investigation: uniqueRequests){
                                   DatasetMetrics dm = new DatasetMetrics();
                                   dm.setDataset(ds);
                                   dm.setCountryCode(investigation[0]);
                                   dm.setDownloadsUnique(new Long(investigation[1]));
                                   dm.setMonth(monthYear);
                                   datasetMetricsRequestsTotal.add(dm);
                               }
                            }                           
                           datasetMetricsDataset= addUpdateMetrics(datasetMetricsDataset, datasetMetricsRequestsTotal , "UniqueRequests");
                        }
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
    
    private List<DatasetMetrics> addUpdateMetrics(List<DatasetMetrics> currentList, List<DatasetMetrics> compareList, String countField){
        
        List<DatasetMetrics> toAdd = new ArrayList();
        
        for (DatasetMetrics testMetric : compareList) {
            
            boolean add = true;
            ListIterator<DatasetMetrics> iterator = currentList.listIterator();
            while (iterator.hasNext()) {
                DatasetMetrics next = iterator.next();
                if (next.getCountryCode().equals(testMetric.getCountryCode())) {
                    //Replace element
       
                    if (countField.equals("TotalViews")){
                       next.setViewsTotal(testMetric.getViewsTotal());
                    }
                    if (countField.equals("UniqueViews")){
                       next.setViewsUnique(testMetric.getViewsUnique());
                    }
                    
                    if (countField.equals("TotalRequests")){
                       next.setDownloadsTotal(testMetric.getDownloadsTotal());
                    }
                    
                    if (countField.equals("UniqueRequests")){
                       next.setDownloadsUnique(testMetric.getDownloadsUnique());
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
