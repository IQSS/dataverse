/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

/**
 *
 * @author gdurand
 */
@ViewScoped
@Named
public class DataverseHeaderFragment implements java.io.Serializable {

    @EJB
    DataverseServiceBean dataverseService;

    @EJB
    SettingsServiceBean settingsService;

    @Inject
    DataverseSession dataverseSession;

    public List<Dataverse> getDataverses(Dataverse dataverse) {
        List dataverses = new ArrayList();
        if (dataverse != null) {
            dataverses.addAll(dataverse.getOwners());
            dataverses.add(dataverse);
        } else {
            dataverses.add(dataverseService.findRootDataverse());
        }
        return dataverses;
    }

    // @todo right now we just check on if published or if you are the creator; need full permission support
    public boolean hasVisibleChildren(Dataverse dataverse) {
        for (Dataverse dv : dataverseService.findByOwnerId(dataverse.getId())) {
            if (dv.isReleased() || dv.getCreator().equals(dataverseSession.getUser())) {
                return true;
            }
        }
        
        return false;

    }

    public TreeNode getDataverseTree(Dataverse dataverse) {
        if (dataverse == null) { // the primefaces component seems to call this with dataverse == null for some reason
            return null;
        }
        return getDataverseNode(dataverse, null, true);
    }

    private TreeNode getDataverseNode(Dataverse dataverse, TreeNode root, boolean expand) {
        // @todo right now we just check on if published or if you are the creator; need full permission support
        if (dataverse.isReleased() || dataverse.getCreator().equals(dataverseSession.getUser())) {
            TreeNode dataverseNode = new DefaultTreeNode(dataverse, root);
            dataverseNode.setExpanded(expand);
            List<Dataverse> childDataversesOfCurrentDataverse = dataverseService.findByOwnerId(dataverse.getId());
            for (Dataverse child : childDataversesOfCurrentDataverse) {
                getDataverseNode(child, dataverseNode, false);
            }
            return dataverseNode;
        }
        return null;
    }

    public String logout() {
        dataverseSession.setUser(null);
        return "/dataverse.xhtml?faces-redirect=true";
    }

    public boolean isSignupAllowed() {
        boolean safeDefaultIfKeyNotFound = false;
        return settingsService.isTrueForKey(SettingsServiceBean.Key.AllowSignUp, safeDefaultIfKeyNotFound);
    }

    public String getSignupUrl() {
        String nonNullDefaultIfKeyNotFound = "";
        String signUpUrl = settingsService.getValueForKey(SettingsServiceBean.Key.SignUpUrl, nonNullDefaultIfKeyNotFound);
        return signUpUrl;
    }
    
    // @todo consider creating a base bean, for now just make this static
    public  String getLoginRedirectPage() {
        return getRedirectPage();
    }
    
    // @todo consider creating a base bean, for now just make this static
    public static String getRedirectPage() {
        try {
            HttpServletRequest req = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
            StringBuilder redirectPage = new StringBuilder();
            redirectPage.append(req.getServletPath());           

            if (req.getParameterMap() != null) {
                StringBuilder queryString = new StringBuilder(); 
                for (Map.Entry<String, String[]> entry : ((Map<String, String[]>)req.getParameterMap()).entrySet()) {
                    String name = entry.getKey();
                    String value = entry.getValue()[0];
                    queryString.append(queryString.length() == 0 ? "?" : "&").append(name).append("=").append(value);                            
                }
                redirectPage.append(queryString);
            }            
            
            return "?redirectPage=" + URLEncoder.encode(redirectPage.toString(), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(DataverseHeaderFragment.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return "";
    }    

}
