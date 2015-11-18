/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.groups.Group;
import edu.harvard.iq.dataverse.authorization.groups.GroupServiceBean;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import static edu.harvard.iq.dataverse.util.JsfHelper.JH;
import edu.harvard.iq.dataverse.util.StringUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author gdurand
 */
@ViewScoped
@Named
public class DataverseHeaderFragment implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(DataverseHeaderFragment.class.getName());

    @EJB
    DataverseServiceBean dataverseService;

    @EJB
    SettingsServiceBean settingsService;

    @EJB
    GroupServiceBean groupService;

    @EJB
    PermissionServiceBean permissionService;

    @EJB
    SystemConfig systemConfig;

    @Inject
    DataverseSession dataverseSession;

    @EJB
    UserNotificationServiceBean userNotificationService;
    
    List<Breadcrumb> breadcrumbs = new ArrayList();

    private Long unreadNotificationCount = null;
    
    public List<Breadcrumb> getBreadcrumbs() {
        return breadcrumbs;
    }

    public void setBreadcrumbs(List<Breadcrumb> breadcrumbs) {
        this.breadcrumbs = breadcrumbs;
    }
    
    public void initBreadcrumbs(DvObject dvObject) {
            if (dvObject.getId() != null) {
                initBreadcrumbs(dvObject, null);
            } else {
                initBreadcrumbs(dvObject.getOwner(), dvObject instanceof Dataverse ? JH.localize("newDataverse") : 
                        dvObject instanceof Dataset ? JH.localize("newDataset") : null );
            }
    }
    
    public Long getUnreadNotificationCount(Long userId){
        
        if (userId == null){
            return new Long("0");
        }
        
        if (this.unreadNotificationCount != null){
            return this.unreadNotificationCount;
        }
        
        try{
            this.unreadNotificationCount = userNotificationService.getUnreadNotificationCountByUser(userId);
        }catch (Exception e){
            logger.warning("Error trying to retrieve unread notification count for user." + e.getMessage());
            this.unreadNotificationCount = new Long("0");
        }
        return this.unreadNotificationCount;
    }

    public void initBreadcrumbs(DvObject dvObject, String subPage) {
        breadcrumbs.clear();

        while (dvObject != null) {
            breadcrumbs.add(0, new Breadcrumb(dvObject.getDisplayName(), dvObject));
            dvObject = dvObject.getOwner();
        }        
        
        if (subPage != null) {
            breadcrumbs.add(new Breadcrumb(subPage, null));
        }
    }



    /* Old methods for breadcrumb and trees - currently disabled and deferred

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
     */
    public String logout() {
        dataverseSession.setUser(null);

        String redirectPage = getPageFromContext();
        try {
            redirectPage = URLDecoder.decode(redirectPage, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(LoginPage.class.getName()).log(Level.SEVERE, null, ex);
            redirectPage = "dataverse.xhtml&alias=" + dataverseService.findRootDataverse().getAlias();
        }

        if (StringUtils.isEmpty(redirectPage)) {
            redirectPage = "dataverse.xhtml&alias=" + dataverseService.findRootDataverse().getAlias();
        }

        logger.log(Level.INFO, "Sending user to = " + redirectPage);
        return redirectPage + (redirectPage.indexOf("?") == -1 ? "?" : "&") + "faces-redirect=true";
    }

    public boolean isSignupAllowed() {
        boolean safeDefaultIfKeyNotFound = false;
        return settingsService.isTrueForKey(SettingsServiceBean.Key.AllowSignUp, safeDefaultIfKeyNotFound);
    }

    public String getSignupUrl(String loginRedirect) {
        String nonNullDefaultIfKeyNotFound = "";
        String signUpUrl = settingsService.getValueForKey(SettingsServiceBean.Key.SignUpUrl, nonNullDefaultIfKeyNotFound);
        return signUpUrl + (signUpUrl.indexOf("?") == -1 ? loginRedirect : loginRedirect.replace("?", "&"));
    }

    public String getLoginRedirectPage() {
        return getRedirectPage();
    }

    // @todo consider creating a base bean, for now just make this static
    public static String getRedirectPage() {

        String redirectPage = getPageFromContext();
        if (!StringUtils.isEmpty(redirectPage)) {
            return "?redirectPage=" + redirectPage;
        }
        return "";
    }

    private static String getPageFromContext() {
        try {
            HttpServletRequest req = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
            StringBuilder redirectPage = new StringBuilder();
            redirectPage.append(req.getServletPath());

            // to regenerate the query string, we need to use the parameter map; however this can contain internal POST parameters
            // that we don't want, so we filter through a list of paramters we do allow
            // @todo verify what needs to be in this list of available parameters (for example do we want to repeat searches when you login?
            List acceptableParameters = new ArrayList();
            acceptableParameters.addAll(Arrays.asList("id", "alias", "version", "q", "ownerId", "persistentId", "versionId", "datasetId", "selectedFileIds", "mode"));

            if (req.getParameterMap() != null) {
                StringBuilder queryString = new StringBuilder();
                for (Map.Entry<String, String[]> entry : ((Map<String, String[]>) req.getParameterMap()).entrySet()) {
                    String name = entry.getKey();
                    if (acceptableParameters.contains(name)) {
                        String value = entry.getValue()[0];
                        queryString.append(queryString.length() == 0 ? "?" : "&").append(name).append("=").append(value);
                    }
                }
                redirectPage.append(queryString);
            }

            return URLEncoder.encode(redirectPage.toString(), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(DataverseHeaderFragment.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "";
    }

    // inner class used for breadcrumbs
    public static class Breadcrumb {

        private final String breadcrumbText;
        private final DvObject dvObject;

        public Breadcrumb(String breadcrumbText, DvObject dvObject) {
            this.breadcrumbText = breadcrumbText;            
            this.dvObject = dvObject;            

        }

        public String getBreadcrumbText() {
            return breadcrumbText;
        }

        public DvObject getDvObject() {
            return dvObject;
        }

    }

    public boolean isDebugShibboleth() {
        return systemConfig.isDebugEnabled();
    }

    public List<String> getGroups(User user) {
        List<String> groups = new ArrayList<>();
        Set<Group> groupsForUser = groupService.groupsFor(user, null);
        for (Group group : groupsForUser) {
            groups.add(group.getDisplayName() + " (" + group.getIdentifier() + ")");
        }
        return groups;
    }

    public List<String> getPermissions(User user, Dataverse dataverse) {
        List<String> permissions = new ArrayList<>();
        for (Permission permission : permissionService.permissionsFor(user, dataverse)) {
            permissions.add(permission.name());
        }
        return permissions;
    }
}
