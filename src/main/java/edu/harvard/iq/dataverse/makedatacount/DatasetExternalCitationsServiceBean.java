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
import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.ejb.Stateless;
import jakarta.inject.Named;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
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
public class DatasetExternalCitationsServiceBean implements java.io.Serializable {
    
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    protected EntityManager em;
    
    @EJB
    DatasetServiceBean datasetService;

  //Array of relationship types that are considered to be citations
  static ArrayList<String> inboundRelationships = new ArrayList<String>( 
          Arrays.asList(
          "cites",
          "references",
          "supplements"));
  static ArrayList<String> outboundRelationships = new ArrayList<String>( 
          Arrays.asList(
          "is-cited-by",
          "is-referenced-by",
          "is-supplemented-by"));
  
    public List<DatasetExternalCitations> parseCitations(JsonArray citations) {
        List<DatasetExternalCitations> datasetExternalCitations = new ArrayList<>();
        for (JsonValue citationValue : citations) {
            DatasetExternalCitations exCit = new DatasetExternalCitations();
            JsonObject citation = (JsonObject) citationValue;
            String subjectUri = citation.getJsonObject("attributes").getString("subj-id");
            
            String objectUri = citation.getJsonObject("attributes").getString("obj-id");
            String relationship = citation.getJsonObject("attributes").getString("relation-type-id");
            if (inboundRelationships.contains(relationship)) {
                Dataset localDs = null;
                if (objectUri.contains("doi")) {
                    String globalId = objectUri.replace("https://", "").replace("doi.org/", "doi:").toUpperCase().replace("DOI:", "doi:");
                    localDs = datasetService.findByGlobalId(globalId);
                    exCit.setDataset(localDs);
                }
                exCit.setCitedByUrl(subjectUri);
                
                if (localDs != null && !exCit.getCitedByUrl().isEmpty()) {
                    datasetExternalCitations.add(exCit);
                }
            }
            if (outboundRelationships.contains(relationship)) {
                Dataset localDs = null;
                if (subjectUri.contains("doi")) {
                    String globalId = subjectUri.replace("https://", "").replace("doi.org/", "doi:").toUpperCase().replace("DOI:", "doi:");
                    localDs = datasetService.findByGlobalId(globalId);
                    exCit.setDataset(localDs);
                }
                exCit.setCitedByUrl(objectUri);
                
                if (localDs != null && !exCit.getCitedByUrl().isEmpty()) {
                    datasetExternalCitations.add(exCit);
                }
            }
        }
        return datasetExternalCitations;
    }
    
    public DatasetExternalCitations save(DatasetExternalCitations datasetExternalCitations) {  
        //Replace existing if necessary
        Dataset dataset =  datasetExternalCitations.getDataset();
        String citedByUrl = datasetExternalCitations.getCitedByUrl();

        DatasetExternalCitations getExisting = getDatasetExternalCitationsByDatasetCitingPID(dataset, citedByUrl);
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
