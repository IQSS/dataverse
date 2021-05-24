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
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author gdurand
 */
@ViewScoped
@Named
public class NavigationWrapper implements java.io.Serializable {
    
    private static final Logger logger = Logger.getLogger(NavigationWrapper.class.getName());    
    @Inject
    DataverseSession session;
    @Inject
    SettingsWrapper settingsWrapper;
    
    String redirectPage;


    public String getRedirectPage() {
        return !StringUtils.isEmpty(getPageFromContext()) ? "?redirectPage=" + getPageFromContext() : "";
    }
    
    // QDRCustom
    public String getShibLoginPath() {
        String QDRDataverseBaseURL = settingsWrapper.get(":QDRDataverseBaseURL");   
        String QDRDrupalSiteURL = settingsWrapper.get(":QDRDrupalSiteURL");
        //Example:
        //https://dev-aws.qdr.org/user/login?current_page=https%3A%2F%2Fdv.dev-aws.qdr.org%2Fshib.xhtml%3FredirectPage%3D%2Fdataset.xhtml%253FpersistentId%253Ddoi%253A10.33564%252FFK2LSJCQI%2526version%253DDRAFT
        String shibLoginPath;
        //Check cookies - if Drupal is logged in/cookies are set to trigger Dataverse passive login, don't try to login at Drupal
        ExternalContext context = FacesContext.getCurrentInstance().getExternalContext(); 
        Map<String, Object> cookies =  context.getRequestCookieMap();
        Cookie passive = (Cookie) cookies.get("_check_is_passive");
        try {
            if (passive==null || passive.getValue().equals("0")) {
              shibLoginPath = QDRDrupalSiteURL + "/user/login?current_page=" + URLEncoder.encode(QDRDataverseBaseURL, "UTF-8") + "%2Fshib.xhtml%3FredirectPage%3D" + URLEncoder.encode(getPageFromContext(), "UTF-8");
            } else {
              shibLoginPath = QDRDataverseBaseURL + "/shib.xhtml?redirectPage=" + getPageFromContext();  
            }
        } catch (UnsupportedEncodingException e) {
            //Shouldn't happen since we're just re-encoding something already successfully encoded once
            shibLoginPath = QDRDrupalSiteURL + "/user/login?current_page=" + QDRDataverseBaseURL;
            logger.severe("Unexpected Failure in getting shibLoginPath, baseURL = " + QDRDataverseBaseURL + ", redirectPage = " + getPageFromContext());
            e.printStackTrace();
        }
        
        /*String shibLoginPath = "/Shibboleth.sso/Login?target=".concat(QDRDataverseBaseURL).concat("/shib.xhtml");                
                
        if (!StringUtils.isEmpty(getRedirectPage())) {
           String redirectPageStr = getRedirectPage();
           redirectPageStr = redirectPageStr.replace("?redirectPage","%3FredirectPage");
           shibLoginPath = shibLoginPath.concat(redirectPageStr);
        }
        */
        
        return shibLoginPath;                        
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
            acceptableParameters.addAll(Arrays.asList("id", "alias", "version", "q", "ownerId", "persistentId", "versionId", "datasetId", "selectedFileIds", "mode", "dataverseId", "fileId", "datasetVersionId", "guestbookId", "selectTab"));

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
            // QDRCustom
            ExternalContext context = FacesContext.getCurrentInstance().getExternalContext(); 
            // Redirect user to Shibboleth login page
            try {
                context.redirect(getShibLoginPath());
                return "";
            } catch (IOException ex) {
                logger.info("Unable to redirect user to Shibboleth login page");
                return "";
            }
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
            Logger.getLogger(PermissionsWrapper.class.getName()).log(Level.SEVERE, null, ex);
        }
        context.responseComplete();
        return "";
    }    
       
    

}
