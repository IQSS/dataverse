/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.groups.GroupServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import static edu.harvard.iq.dataverse.util.JsfHelper.JH;

import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
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
    
    @EJB
    DatasetVersionServiceBean datasetVersionService;
    
    @EJB
    DataFileServiceBean datafileService;

    @Inject
    DataverseSession dataverseSession;

    @Inject
    SettingsWrapper settingsWrapper;

    @Inject 
    NavigationWrapper navigationWrapper;    

    @EJB
    UserNotificationServiceBean userNotificationService;
    
    List<Breadcrumb> breadcrumbs = new ArrayList<>();

    private Long unreadNotificationCount = null;
    
    public List<Breadcrumb> getBreadcrumbs() {
        return breadcrumbs;
    }

    public void setBreadcrumbs(List<Breadcrumb> breadcrumbs) {
        this.breadcrumbs = breadcrumbs;
    }
    
    public void initBreadcrumbs(DvObject dvObject) {
            if (dvObject == null) {
                return;
            }
            if (dvObject.getId() != null) {
                initBreadcrumbs(dvObject, null);
            } else {
                initBreadcrumbs(dvObject.getOwner(), dvObject instanceof Dataverse ? BundleUtil.getStringFromBundle("newDataverse") : 
                        dvObject instanceof Dataset ? BundleUtil.getStringFromBundle("newDataset") : null );
            }
    }
    
    public void initBreadcrumbsForFileMetadata(FileMetadata fmd) {

        initBreadcrumbsForFileMetadata(fmd, null, null);
    }
    
    public void initBreadcrumbsForFileMetadata(DataFile datafile,  String subPage) {
       
        initBreadcrumbsForFileMetadata(null, datafile,  subPage);
    }
    

    public void initBreadcrumbsForFileMetadata(FileMetadata fmd, DataFile datafile,  String subPage) {
        if (fmd == null ){
            Dataset dataset = datafile.getOwner();
            Long getDatasetVersionID = dataset.getLatestVersion().getId();
            fmd = datafileService.findFileMetadataByDatasetVersionIdAndDataFileId(getDatasetVersionID, datafile.getId());
        }
        
        
        if (fmd == null) {
            return;
        }

        breadcrumbs.clear();
        
        String optionalUrlExtension = "&version=" + fmd.getDatasetVersion().getSemanticVersion();
        //First Add regular breadcrumb for the data file
        DvObject dvObject = fmd.getDataFile();
        breadcrumbs.add(0, new Breadcrumb(dvObject, dvObject.getDisplayName(), optionalUrlExtension));

        //Get the Dataset Owning the Datafile and add version to the breadcrumb       
        dvObject = dvObject.getOwner();

        breadcrumbs.add(0, new Breadcrumb(dvObject, dvObject.getDisplayName(), optionalUrlExtension));

        // now get Dataverse Owner of the dataset and proceed as usual
        dvObject = dvObject.getOwner();
        while (dvObject != null) {
            breadcrumbs.add(0, new Breadcrumb(dvObject, dvObject.getDisplayName()));
            dvObject = dvObject.getOwner();
        }
        
        if (subPage != null) {
            breadcrumbs.add(new Breadcrumb(subPage));
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
            breadcrumbs.add(0, new Breadcrumb(dvObject, dvObject.getDisplayName()));
            dvObject = dvObject.getOwner();
        }        
        
        if (subPage != null) {
            breadcrumbs.add(new Breadcrumb(subPage));
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
        dataverseSession.setStatusDismissed(false);

        String redirectPage = navigationWrapper.getPageFromContext();
        try {
            redirectPage = URLDecoder.decode(redirectPage, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(LoginPage.class.getName()).log(Level.SEVERE, null, ex);
            redirectPage = redirectToRoot();
        }

        if (StringUtils.isEmpty(redirectPage)) {
            redirectPage = redirectToRoot();
        }

        logger.log(Level.INFO, "Sending user to = " + redirectPage);
        return redirectPage + (!redirectPage.contains("?") ? "?" : "&") + "faces-redirect=true";
    }

    private Boolean signupAllowed = null;
    
    private String redirectToRoot(){
        return "dataverse.xhtml?alias=" + dataverseService.findRootDataverse().getAlias();
    }
    
    public boolean isSignupAllowed() {
        if (signupAllowed != null) {
            return signupAllowed;
        }
        boolean safeDefaultIfKeyNotFound = false;
        signupAllowed = settingsWrapper.isTrueForKey(SettingsServiceBean.Key.AllowSignUp, safeDefaultIfKeyNotFound);
        return signupAllowed;
    }

    public boolean isRootDataverseThemeDisabled(Dataverse dataverse) {
        if (dataverse == null) {
            return false;
        }
        if (dataverse.getOwner() == null) {
            // We're operating on the root dataverse.
            return settingsWrapper.isRootDataverseThemeDisabled();
        } else {
            return false;
        }
    }

    public String getSignupUrl(String loginRedirect) {
        String nonNullDefaultIfKeyNotFound = "";
        String signUpUrl = settingsWrapper.getValueForKey(SettingsServiceBean.Key.SignUpUrl, nonNullDefaultIfKeyNotFound);
        return signUpUrl + (!signUpUrl.contains("?") ? loginRedirect : loginRedirect.replace("?", "&"));
    }

    public String getLoginRedirectPage() {
        System.out.println("DEPRECATED call to getLoginRedirectPage method in DataverseHeaderfragment: " + navigationWrapper.getRedirectPage());
        return navigationWrapper.getRedirectPage();
    }

    public void addBreadcrumb (String url, String linkString){
        breadcrumbs.add(new Breadcrumb(url, linkString));
    }
    
    public void addBreadcrumb (String linkString){
        breadcrumbs.add(new Breadcrumb(linkString));
    }

    // inner class used for breadcrumbs
    public static class Breadcrumb {

        private final String breadcrumbText;
        private DvObject dvObject = null;
        private String url = null;
        private String optionalUrlExtension = null;

        public Breadcrumb( DvObject dvObject, String breadcrumbText, String optionalUrlExtension ) {
            this.breadcrumbText = breadcrumbText;
            this.dvObject = dvObject;
            this.optionalUrlExtension = optionalUrlExtension;
        }

        public Breadcrumb( DvObject dvObject, String breadcrumbText) {
            this.breadcrumbText = breadcrumbText;
            this.dvObject = dvObject;
        }

        public Breadcrumb( String url, String breadcrumbText) {
            this.breadcrumbText = breadcrumbText;
            this.url = url;
        }
        
        public Breadcrumb(String breadcrumbText){
            this.breadcrumbText = breadcrumbText;
        }

        public String getBreadcrumbText() {
            return breadcrumbText;
        }

        public DvObject getDvObject() {
            return dvObject;
        }

        public String getUrl() {
            return url;
        }
        
        public String getOptionalUrlExtension() {
            return optionalUrlExtension;
        }
    }
}