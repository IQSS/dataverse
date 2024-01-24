/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author gdurand
 */
@ViewScoped
@Named
public class NavigationWrapper implements java.io.Serializable {
    
    @Inject
    DataverseSession session;
    
    String redirectPage;


    public String getRedirectPage() {
        return !StringUtils.isEmpty(getPageFromContext()) ? "?redirectPage=" + getPageFromContext() : "";
    }

    public String getPageFromContext() {
        if (redirectPage == null) {
            StringBuilder redirectBuilder = new StringBuilder();        

            HttpServletRequest req = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
            redirectBuilder.append(req.getServletPath());

            // to regenerate the query string, we need to use the parameter map; however this can contain internal POST parameters
            // that we don't want, so we filter through a list of paramters we do allow
            // @todo verify what needs to be in this list of available parameters (for example do we want to repeat searches when you login?
            List<String> acceptableParameters = new ArrayList<>();
            acceptableParameters.addAll(Arrays.asList("id", "alias", "version", "q", "ownerId", "persistentId", "versionId", "datasetId", "selectedFileIds", "mode", "dataverseId", "fileId", "datasetVersionId", "guestbookId"));

            if (req.getParameterMap() != null) {
                StringBuilder queryString = new StringBuilder();
                for (Map.Entry<String, String[]> entry : ((Map<String, String[]>) req.getParameterMap()).entrySet()) {
                    String name = entry.getKey();
                    if (acceptableParameters.contains(name)) {
                        String value = entry.getValue()[0];
                        queryString.append(queryString.length() == 0 ? "?" : "&").append(name).append("=").append(value);
                    }
                }
                redirectBuilder.append(queryString);
            }

            try {
                redirectPage = URLEncoder.encode(redirectBuilder.toString(), "UTF-8");

            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(DataverseHeaderFragment.class.getName()).log(Level.SEVERE, null, ex);
                redirectPage = "";
            }
        }
        
        return redirectPage;
    }  
    
    
    
     public String notAuthorized(){
        if (!session.getUser().isAuthenticated()){
            return "/loginpage.xhtml" + getRedirectPage();
        } else {
            return sendError(HttpServletResponse.SC_FORBIDDEN);
        }        
    }
    
    public String notFound() {
        return sendError(HttpServletResponse.SC_NOT_FOUND);
    }
    
    private String sendError(int errorCode) {
        FacesContext context = FacesContext.getCurrentInstance();
        try {
            context.getExternalContext().responseSendError(errorCode,null);
        } catch (IOException ex) {
            //Logger.getLogger(PermissionsWrapper.class.getName()).log(Level.SEVERE, null, ex);
            Logger.getLogger(NavigationWrapper.class.getName()).fine("Caught exception in sendError(): "+ex.getMessage());
        }
        context.responseComplete();
        return "";
    }    
       
    

}
