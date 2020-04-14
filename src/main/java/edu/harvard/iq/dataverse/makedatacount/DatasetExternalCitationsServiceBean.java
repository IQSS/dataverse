/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.makedatacount;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.json.JsonArray;
import javax.json.JsonObject;
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
public class DatasetExternalCitationsServiceBean implements java.io.Serializable {
    
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    protected EntityManager em;
    
    @EJB
    DatasetServiceBean datasetService;
    
    public List<DatasetExternalCitations> parseCitations(JsonArray citations) {
        List<DatasetExternalCitations> datasetExternalCitations = new ArrayList<>();
        for (JsonValue citationValue : citations) {
            DatasetExternalCitations exCit = new DatasetExternalCitations();
            JsonObject citation = (JsonObject) citationValue;
            exCit.setCitedByUrl(citation.getJsonObject("attributes").getString("subj-id"));
           
            String localDatasetDOI = citation.getJsonObject("attributes").getString("obj-id");
            
            Dataset localDs = null;
            if (localDatasetDOI.contains("doi")) {
                String globalId = localDatasetDOI.replace("https://", "").replace("doi.org/", "doi:").toUpperCase().replace("DOI:", "doi:");
                localDs = datasetService.findByGlobalId(globalId);
                exCit.setDataset(localDs);
            }

            if (localDs != null && !exCit.getCitedByUrl().isEmpty() ) {
                datasetExternalCitations.add(exCit);
            }

        }
        return datasetExternalCitations;
    }
    
    public DatasetExternalCitations save(DatasetExternalCitations datasetExternalCitations) {  
        //Replace existing if necessary
        Dataset testDs =  datasetExternalCitations.getDataset();
        String testMonth = datasetExternalCitations.getCitedByUrl();

        DatasetExternalCitations getExisting = getDatasetExternalCitationsByDatasetCitingPID(testDs, testMonth);
        if (getExisting != null){
            em.remove(getExisting);
        }
        DatasetExternalCitations savedDatasetExternalCitations = em.merge(datasetExternalCitations);
        return savedDatasetExternalCitations;
    }
    
    private DatasetExternalCitations getDatasetExternalCitationsByDatasetCitingPID(Dataset dataset, String PID){
        DatasetExternalCitations dsExtCit = null;
        String queryStr = "SELECT d FROM DatasetExternalCitations d WHERE d.dataset.id = " + dataset.getId() + " and d.citedByUrl = '" + PID + "'";
        Query query = em.createQuery(queryStr);
        List resultList = query.getResultList();
        if (resultList.size() > 1) {
            throw new EJBException("More than one Dataset External Citation found in the dataset (id= " + dataset.getId() + "), with citedByUrl= " + PID + ".");
        }
        if (resultList.size() == 1) {
            dsExtCit = (DatasetExternalCitations) resultList.get(0);
            return dsExtCit;
        }
        return null;
    }
    
    public List<DatasetExternalCitations> getDatasetExternalCitationsByDataset(Dataset dataset) {
        List<DatasetExternalCitations> retVal = new ArrayList();
        String queryStr = "SELECT d FROM DatasetExternalCitations d WHERE d.dataset.id = " + dataset.getId();
        Query query = em.createQuery(queryStr);
        List<DatasetExternalCitations> result = query.getResultList();

        for (DatasetExternalCitations row : result) {
            DatasetExternalCitations dec = new DatasetExternalCitations();
            dec.setDataset(dataset);
            dec.setCitedByUrl(row.getCitedByUrl());
            retVal.add(dec);
        }
        
        return retVal;
    }
    
}
