package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.api.DataTagsAPI;
import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.json.JsonObject;

/**
 *
 * @author Naomi
 */
@Named("dataTagsAPITestingBean")
@SessionScoped
public class DataTagsAPITestingBean implements Serializable {

    private String datasetName;
    

    @EJB
    DataTagsAPI dt;
    
    
    public String requestInterview() {
        String url = dt.requestInterview();
        Logger.getLogger(DataTagsAPITestingBean.class.getName()).info("Dataset name: " + datasetName);        
        try {
            FacesContext.getCurrentInstance().getExternalContext().redirect(url);
        } catch (IOException ex) {
            Logger.getLogger(DataTagsAPITestingBean.class.getName()).log(Level.SEVERE, null, ex);
        }
        return url;
    }
    
    public void setDatasetName(String name) {
        datasetName = name;
        dt.getContainer().setDatasetName(datasetName);
        dt.setCache(dt.getCallbackURL().substring(47), dt.getContainer());
    }
    
    public String getDatasetName() {
        if (!dt.getCache().isEmpty()) {
            return dt.getCache().get(dt.getCallbackURL().substring(47)).getDatasetName();
        } else {
            return "";
        }
    }
    
    
    public JsonObject getTags() {
       return dt.getCache().get(dt.getCallbackURL().substring(47)).getTag();
    }
    
    
}
