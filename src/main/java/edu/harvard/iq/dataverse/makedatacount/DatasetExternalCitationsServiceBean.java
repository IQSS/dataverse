/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.makedatacount;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import java.util.ArrayList;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

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
    
    
    public List<DatasetExternalCitations> parseCitations(JsonObject report) {
        List<DatasetExternalCitations> datasetExternalCitations = new ArrayList<>();
        JsonArray citations = report.getJsonArray("data");
        for (JsonValue citationValue : citations) {
            DatasetExternalCitations exCit = new DatasetExternalCitations();
            JsonObject citation = (JsonObject) citationValue;
            exCit.setCitedByUrl(citation.getJsonObject("attributes").getString("subj-id"));
           
            String localDatasetDOI = citation.getJsonObject("attributes").getString("obj-id");
            
            Dataset localDs = null;
            if (localDatasetDOI.contains("doi")) {
                String globalId = localDatasetDOI.replace("https://", "").replace("doi.org/", "doi:");
                localDs = datasetService.findByGlobalId(globalId);
                exCit.setDataset(localDs);
            }

            if (localDs != null) {
                datasetExternalCitations.add(exCit);
            }

        }
        return datasetExternalCitations;
    }
    
}
