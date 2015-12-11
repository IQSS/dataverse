/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.usage;

import edu.harvard.iq.dataverse.usage.Event.EventType;
import static edu.harvard.iq.dataverse.usage.UsageSearchQuery.DateHistogramInterval.*;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseHeaderFragment;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.DvObjectServiceBean;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroupServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

/**
 *
 * @author luopc
 */
@ViewScoped
@Named
public class UsagePage implements Serializable {

    private static final long serialVersionUID = -1718646164721462359L;
    
    private static final Logger logger = Logger.getLogger(UsagePage.class.getCanonicalName());

    public enum UsageType{VIEW,DOWNLOAD_FILE};
    
    private UsageType type;
    private Long id;
    private DvObject dvObject;

    @EJB
    DvObjectServiceBean dvObjectService;
    @EJB
    PermissionServiceBean permissionService;
    @EJB
    UsageSearchServiceBean usageSearchService;
    @EJB
    ExplicitGroupServiceBean explicitGroupService;
    @EJB
    DatasetServiceBean datasetService;
    @EJB
    AuthenticationServiceBean authenticationServiceBean;
    @EJB
    BuiltinUserServiceBean builtinUserService;
    
    //Events for view dataverse or dataset
    private UsageSearchQuery vdQuery;
    private UsageSearchResult vdEvents;

    //Events for file download
    private UsageSearchQuery fdQuery;
    private UsageSearchResult fdEvents;

    //download information for dataset
    private long selectedDataFileId;
    private List<FileMetadata> fileMetadatas;
    private Map<Long,FileMetadata> fileId2FileMetadata;

    public String init() {
        dvObject = dvObjectService.findDvObject(id);
        if(dvObject instanceof Dataverse || dvObject instanceof Dataset){
            if (!permissionService.on(dvObject).has(
                    dvObject instanceof Dataverse ? Permission.ManageDataversePermissions
                            : Permission.ManageDatasetPermissions)) {
                return "/loginpage.xhtml" + DataverseHeaderFragment.getRedirectPage();
            }
        }else{
            return "/404.xhtml";
        }
        switch(type){
            case VIEW:initViewDataverseOrDataset();break;
            case DOWNLOAD_FILE:initDownloadFile();break;
            default:
                return "/404.xhtml";
        }
        return "";
    }
    
    public void initViewDataverseOrDataset(){
        vdQuery = new UsageSearchQuery();
        vdQuery.setDateHistogramInterval(DAY);
        if (dvObject.isInstanceofDataverse()) {
            vdQuery.setEventTypes(Arrays.asList(EventType.VIEW_DATAVERSE));
            vdQuery.setDataverseIds(Arrays.asList(id));
        }
        if (dvObject.isInstanceofDataset()) {
            vdQuery.setEventTypes(Arrays.asList(EventType.VIEW_DATASET));
            vdQuery.setDatasetIds(Arrays.asList(id));
        }
        searchViewDvo(1);
    }
    
    public void searchViewDvo(int page) {
        vdQuery.setFrom((page - 1) * vdQuery.getSize());
        vdEvents = usageSearchService.search(vdQuery);
    }
    
    public void initDownloadFile(){       
        fdQuery = new UsageSearchQuery();
        if (dvObject.isInstanceofDataset()) {
            fileMetadatas = ((Dataset) dvObject).getLatestVersion().getFileMetadatas();
            fileId2FileMetadata = new HashMap<>();
            for (FileMetadata fileMetadata : fileMetadatas) {
                fileId2FileMetadata.put(fileMetadata.getDataFile().getId(), fileMetadata);
            }
            if(!fileMetadatas.isEmpty()){
                fdQuery.setDateHistogramInterval(DAY);
                fdQuery.setEventTypes(Arrays.asList(EventType.DOWNLOAD_FILE));
                searchFileDownload(1);
            }
        }
    }    
    
    public void searchFileDownload(int page){
        fdQuery.setFrom((page - 1)*fdQuery.getSize());
        if(selectedDataFileId == 0){
            fdQuery.setDatafileIds(getAllDataFileIds());
        }else{
            fdQuery.setDatafileIds(Arrays.asList(selectedDataFileId));
        }
        fdEvents = usageSearchService.search(fdQuery);
    }
    
    private List<Long> getAllDataFileIds(){
        List<Long> ids = new ArrayList<>();
        for(FileMetadata fileMetadata : fileMetadatas){
            ids.add(fileMetadata.getDataFile().getId());
        }
        return ids;
    }

    public UsageSearchQuery getVdQuery() {
        return vdQuery;
    }

    public void setVdQuery(UsageSearchQuery vdQuery) {
        this.vdQuery = vdQuery;
    }

    public UsageSearchResult getVdEvents() {
        return vdEvents;
    }

    public void setVdEvents(UsageSearchResult vdEvents) {
        this.vdEvents = vdEvents;
    }

    public UsageSearchQuery getFdQuery() {
        return fdQuery;
    }

    public void setFdQuery(UsageSearchQuery fdQuery) {
        this.fdQuery = fdQuery;
    }

    public UsageSearchResult getFdEvents() {
        return fdEvents;
    }

    public void setFdEvents(UsageSearchResult fdEvents) {
        this.fdEvents = fdEvents;
    }

    public long getSelectedDataFileId() {
        return selectedDataFileId;
    }

    public void setSelectedDataFileId(long selectedDataFileId) {
        this.selectedDataFileId = selectedDataFileId;
    }

    public List<FileMetadata> getFileMetadatas() {
        return fileMetadatas;
    }

    public void setFileMetadatas(List<FileMetadata> fileMetadatas) {
        this.fileMetadatas = fileMetadatas;
    }

    public Map<Long, FileMetadata> getFileId2FileMetadata() {
        return fileId2FileMetadata;
    }

    public void setFileId2FileMetadata(Map<Long, FileMetadata> fileId2FileMetadata) {
        this.fileId2FileMetadata = fileId2FileMetadata;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DvObject getDvObject() {
        return dvObject;
    }

    public void setDvObject(DvObject dvObject) {
        this.dvObject = dvObject;
    }

    public UsageType getType() {
        return type;
    }

    public void setType(UsageType type) {
        this.type = type;
    }
    
    public String getBreadcrumbTitle(){
        if(dvObject.isInstanceofDataverse()){
            if(type == UsageType.VIEW)
                return ResourceBundle.getBundle("Bundle", FacesContext.getCurrentInstance().getViewRoot().getLocale())
                        .getString("usage.dataverse.view");
        }else if(dvObject.isInstanceofDataset()){
            if(type == UsageType.VIEW)
                return ResourceBundle.getBundle("Bundle", FacesContext.getCurrentInstance().getViewRoot().getLocale())
                        .getString("usage.dataset.view");
            else if(type == UsageType.DOWNLOAD_FILE)
                return ResourceBundle.getBundle("Bundle", FacesContext.getCurrentInstance().getViewRoot().getLocale())
                        .getString("usage.datafile.download");
        }
        return "";
    }
}
