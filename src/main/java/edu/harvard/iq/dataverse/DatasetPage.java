package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.provenance.ProvPopupFragmentBean;
import edu.harvard.iq.dataverse.api.AbstractApiBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.PrivateUrlUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.branding.BrandingUtil;
import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import edu.harvard.iq.dataverse.dataaccess.ImageThumbConverter;
import edu.harvard.iq.dataverse.dataaccess.SwiftAccessIO;
import edu.harvard.iq.dataverse.datacapturemodule.DataCaptureModuleUtil;
import edu.harvard.iq.dataverse.datacapturemodule.ScriptRequestResponse;
import edu.harvard.iq.dataverse.dataset.DatasetThumbnail;
import edu.harvard.iq.dataverse.dataset.DatasetUtil;
import edu.harvard.iq.dataverse.datavariable.VariableServiceBean;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.CreatePrivateUrlCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CuratePublishedDatasetVersionCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeaccessionDatasetVersionCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteDatasetVersionCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeletePrivateUrlCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DestroyDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.GetPrivateUrlCommand;
import edu.harvard.iq.dataverse.engine.command.impl.LinkDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.PublishDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.PublishDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetVersionCommand;
import edu.harvard.iq.dataverse.export.ExportException;
import edu.harvard.iq.dataverse.export.ExportService;
import edu.harvard.iq.dataverse.export.spi.Exporter;
import edu.harvard.iq.dataverse.ingest.IngestRequest;
import edu.harvard.iq.dataverse.ingest.IngestServiceBean;
import edu.harvard.iq.dataverse.metadataimport.ForeignMetadataImportServiceBean;
import edu.harvard.iq.dataverse.privateurl.PrivateUrl;
import edu.harvard.iq.dataverse.privateurl.PrivateUrlServiceBean;
import edu.harvard.iq.dataverse.privateurl.PrivateUrlUtil;
import edu.harvard.iq.dataverse.search.SearchFilesServiceBean;
import edu.harvard.iq.dataverse.search.SortBy;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.ArchiverUtil;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.FileSortFieldAndOrder;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.JsfHelper;
import static edu.harvard.iq.dataverse.util.JsfHelper.JH;
import static edu.harvard.iq.dataverse.util.StringUtil.isEmpty;

import edu.harvard.iq.dataverse.util.StringUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.workflows.WorkflowComment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Collection;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.file.UploadedFile;
import javax.validation.ConstraintViolation;
import org.apache.commons.httpclient.HttpClient;
//import org.primefaces.context.RequestContext;
import java.util.Arrays;
import java.util.HashSet;
import javax.faces.model.SelectItem;
import java.util.logging.Level;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.engine.command.impl.AbstractSubmitToArchiveCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateNewDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.GetLatestPublishedDatasetVersionCommand;
import edu.harvard.iq.dataverse.engine.command.impl.RequestRsyncScriptCommand;
import edu.harvard.iq.dataverse.engine.command.impl.PublishDatasetResult;
import edu.harvard.iq.dataverse.engine.command.impl.RestrictFileCommand;
import edu.harvard.iq.dataverse.engine.command.impl.ReturnDatasetToAuthorCommand;
import edu.harvard.iq.dataverse.engine.command.impl.SubmitDatasetForReviewCommand;
import edu.harvard.iq.dataverse.externaltools.ExternalTool;
import edu.harvard.iq.dataverse.externaltools.ExternalToolServiceBean;
import edu.harvard.iq.dataverse.export.SchemaDotOrgExporter;
import edu.harvard.iq.dataverse.externaltools.ExternalToolHandler;
import edu.harvard.iq.dataverse.makedatacount.MakeDataCountLoggingServiceBean;
import edu.harvard.iq.dataverse.makedatacount.MakeDataCountLoggingServiceBean.MakeDataCountEntry;
import java.util.Collections;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;

import javax.faces.event.AjaxBehaviorEvent;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.io.IOUtils;

import org.primefaces.component.tabview.TabView;
import org.primefaces.event.CloseEvent;
import org.primefaces.event.TabChangeEvent;
import org.primefaces.event.data.PageEvent;

import edu.harvard.iq.dataverse.search.FacetLabel;
import edu.harvard.iq.dataverse.search.SearchConstants;
import edu.harvard.iq.dataverse.search.SearchFields;
import edu.harvard.iq.dataverse.search.SearchServiceBean;
import edu.harvard.iq.dataverse.search.SearchUtil;
import edu.harvard.iq.dataverse.search.SolrClientService;
import java.util.Comparator;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.primefaces.PrimeFaces;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

/**
 *
 * @author gdurand
 */
@ViewScoped
@Named("DatasetPage")
public class DatasetPage implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(DatasetPage.class.getCanonicalName());

    public enum EditMode {

        CREATE, INFO, FILE, METADATA, LICENSE
    };

    public enum DisplayMode {

        INIT, SAVE
    };
    

    @EJB
    DatasetServiceBean datasetService;
    @EJB
    DatasetVersionServiceBean datasetVersionService;
    @EJB
    DataFileServiceBean datafileService;
    @EJB
    PermissionServiceBean permissionService;
    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    DatasetFieldServiceBean fieldService;
    @EJB
    VariableServiceBean variableService;
    @EJB
    IngestServiceBean ingestService;
    @EJB
    ForeignMetadataImportServiceBean metadataImportService;
    @EJB
    EjbDataverseEngine commandEngine;
    @Inject
    DataverseSession session;
    @EJB
    UserNotificationServiceBean userNotificationService;
    @EJB
    BuiltinUserServiceBean builtinUserService;
    @EJB
    DataverseFieldTypeInputLevelServiceBean dataverseFieldTypeInputLevelService;
    @EJB
    SettingsServiceBean settingsService;
    @EJB
    SearchServiceBean searchService;
    @EJB
    AuthenticationServiceBean authService;
    @EJB
    SystemConfig systemConfig;
    @EJB
    GuestbookResponseServiceBean guestbookResponseService;
    @EJB
    FileDownloadServiceBean fileDownloadService;
    @EJB
    DataverseLinkingServiceBean dvLinkingService;
    @EJB
    DatasetLinkingServiceBean dsLinkingService;
    @EJB
    SearchFilesServiceBean searchFilesService;
    @EJB
    DataverseRoleServiceBean dataverseRoleService;
    @EJB
    PrivateUrlServiceBean privateUrlService;
    @EJB
    ExternalToolServiceBean externalToolService;
    @EJB
    SolrClientService solrClientService;
    @Inject
    DataverseRequestServiceBean dvRequestService;
    @Inject
    DatasetVersionUI datasetVersionUI;
    @Inject
    PermissionsWrapper permissionsWrapper;
    @Inject
    FileDownloadHelper fileDownloadHelper;
    @Inject
    ThumbnailServiceWrapper thumbnailServiceWrapper;
    @Inject
    SettingsWrapper settingsWrapper; 
    @Inject 
    ProvPopupFragmentBean provPopupFragmentBean;
    @Inject
    MakeDataCountLoggingServiceBean mdcLogService;
    @Inject DataverseHeaderFragment dataverseHeaderFragment;

    private Dataset dataset = new Dataset();
    
    private Long id = null;    
    private EditMode editMode;
    private boolean bulkFileDeleteInProgress = false;

    private Long ownerId;
    private Long versionId;
    private int selectedTabIndex;
    private List<DataFile> newFiles = new ArrayList<>();
    private List<DataFile> uploadedFiles = new ArrayList<>();
    private MutableBoolean uploadInProgress = new MutableBoolean(false);

    private DatasetVersion workingVersion;
    private DatasetVersion clone;
    private int releaseRadio = 1;
    private int deaccessionRadio = 0;
    private int deaccessionReasonRadio = 0;
    private String datasetNextMajorVersion = "1.0";
    private String datasetNextMinorVersion = "";
    private String dropBoxSelection = "";
    private String deaccessionReasonText = "";
    private String displayCitation;
    private String deaccessionForwardURLFor = "";
    private String showVersionList = "false";
    private List<Template> dataverseTemplates = new ArrayList<>();
    private Template defaultTemplate;
    private Template selectedTemplate;
    /**
     * In the file listing, the page the user is on. This is zero-indexed so if
     * the user clicks page 2 in the UI, this will be 1.
     */
    private int filePaginatorPage;
    private int rowsPerPage;

    private String persistentId;
    private String version;
    private String protocol = "";
    private String authority = "";
    private String customFields="";

    private boolean noDVsAtAll = false;

    private boolean noDVsRemaining = false;
    
    private boolean stateChanged = false;

    private Long linkingDataverseId;
    private List<SelectItem> linkingDVSelectItems;
    private Dataverse linkingDataverse;
    private Dataverse selectedHostDataverse;

    public Dataverse getSelectedHostDataverse() {
        return selectedHostDataverse;
    }

    public void setSelectedHostDataverse(Dataverse selectedHostDataverse) {
        this.selectedHostDataverse = selectedHostDataverse;
    }
    
    // Version tab lists
    private List<DatasetVersion> versionTabList = new ArrayList<>();
    private List<DatasetVersion> versionTabListForPostLoad = new ArrayList<>();

    
    // Used to store results of permissions checks
    private final Map<String, Boolean> datasetPermissionMap = new HashMap<>(); // { Permission human_name : Boolean }
    

    
    private DataFile selectedDownloadFile;

    private Long maxFileUploadSizeInBytes = null;
    
    private String dataverseSiteUrl = ""; 
    
    private boolean removeUnusedTags;
    
    private Boolean hasRsyncScript = false;
    
    private Boolean hasTabular = false;

    /**
     * If the dataset version has at least one tabular file. The "hasTabular"
     * boolean is for the dataset level ("has ever had a tabular file") but
     * sometimes you want to know about the current version ("no tabular files
     * currently"). Like all files, tabular files can be deleted.
     */
    private boolean versionHasTabular = false;
    
    private boolean showIngestSuccess;

    public boolean isShowIngestSuccess() {
        return showIngestSuccess;
    }

    public void setShowIngestSuccess(boolean showIngestSuccess) {
        this.showIngestSuccess = showIngestSuccess;
    }
        
    // TODO: Consider renaming "configureTools" to "fileConfigureTools".
    List<ExternalTool> configureTools = new ArrayList<>();
    // TODO: Consider renaming "exploreTools" to "fileExploreTools".
    List<ExternalTool> exploreTools = new ArrayList<>();
    // TODO: Consider renaming "configureToolsByFileId" to "fileConfigureToolsByFileId".
    Map<Long, List<ExternalTool>> configureToolsByFileId = new HashMap<>();
    // TODO: Consider renaming "exploreToolsByFileId" to "fileExploreToolsByFileId".
    Map<Long, List<ExternalTool>> exploreToolsByFileId = new HashMap<>();
    // TODO: Consider renaming "previewToolsByFileId" to "file:PreviewToolsByFileId".
    Map<Long, List<ExternalTool>> previewToolsByFileId = new HashMap<>();
    // TODO: Consider renaming "previewTools" to "filePreviewTools".
    List<ExternalTool> previewTools = new ArrayList<>();
    private List<ExternalTool> datasetExploreTools;
    
    public Boolean isHasRsyncScript() {
        return hasRsyncScript;
    }

    public void setHasRsyncScript(Boolean hasRsyncScript) {
        this.hasRsyncScript = hasRsyncScript;
    }
    
    /**
     * The contents of the script.
     */
    private String rsyncScript = "";

    public String getRsyncScript() {
        return rsyncScript;
    }

    public void setRsyncScript(String rsyncScript) {
        this.rsyncScript = rsyncScript;
    }

    private String rsyncScriptFilename;

    public String getRsyncScriptFilename() {
        return rsyncScriptFilename;
    }

    private String thumbnailString = null; 

    // This is the Dataset-level thumbnail; 
    // it's either the thumbnail of the designated datafile, 
    // or scaled down uploaded "logo" file, or randomly selected
    // image datafile from this dataset. 
    public String getThumbnailString() {
        // This method gets called 30 (!) times, just to load the page!
        // - so let's cache that string the first time it's called. 
            
        if (thumbnailString != null) {
            if ("".equals(thumbnailString)) {
                return null;
            } 
            return thumbnailString;
        }

        if (!readOnly) {
            DatasetThumbnail datasetThumbnail = dataset.getDatasetThumbnail(ImageThumbConverter.DEFAULT_DATASETLOGO_SIZE);
            if (datasetThumbnail == null) {
                thumbnailString = "";
                return null;
            }

            if (datasetThumbnail.isFromDataFile()) {
                if (!datasetThumbnail.getDataFile().equals(dataset.getThumbnailFile())) {
                    datasetService.assignDatasetThumbnailByNativeQuery(dataset, datasetThumbnail.getDataFile());
                    // refresh the dataset:
                    dataset = datasetService.find(dataset.getId());
                }
            }

            thumbnailString = datasetThumbnail.getBase64image();
        } else {
            thumbnailString = thumbnailServiceWrapper.getDatasetCardImageAsBase64Url(dataset, 
                    workingVersion.getId(),
                    !workingVersion.isDraft(),
                    ImageThumbConverter.DEFAULT_DATASETLOGO_SIZE);
            if (thumbnailString == null) {
                thumbnailString = "";
                return null;
            }
            
            
        }
        return thumbnailString;
    }

    public void setThumbnailString(String thumbnailString) {
        //Dummy method
    }

    public boolean isRemoveUnusedTags() {
        return removeUnusedTags;
    }

    public void setRemoveUnusedTags(boolean removeUnusedTags) {
        this.removeUnusedTags = removeUnusedTags;
    }

    private List<FileMetadata> fileMetadatas;
    private String fileSortField;
    private String fileSortOrder;

    private LazyFileMetadataDataModel lazyModel;

    public LazyFileMetadataDataModel getLazyModel() {
        return lazyModel;
    }

    public void setLazyModel(LazyFileMetadataDataModel lazyModel) {
        this.lazyModel = lazyModel;
    }
    
    public List<Entry<String,String>> getCartList() {
        if (session.getUser() instanceof AuthenticatedUser) {
            return ((AuthenticatedUser) session.getUser()).getCart().getContents();
        }
        return null;
    }
    
    public boolean checkCartForItem(String title, String persistentId) {
        if (session.getUser() instanceof AuthenticatedUser) {
            return ((AuthenticatedUser) session.getUser()).getCart().checkCartForItem(title, persistentId);
        }
        return false;
    }

    public void addItemtoCart(String title, String persistentId) throws Exception{
        if (canComputeAllFiles(true)) {
            if (session.getUser() instanceof AuthenticatedUser) {
                AuthenticatedUser authUser = (AuthenticatedUser) session.getUser();
                try {
                    authUser.getCart().addItem(title, persistentId);
                    JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("dataset.compute.computeBatch.success"));
                } catch (Exception ex){
                    JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("dataset.compute.computeBatch.failure"));
                }
            }
        }
    }
    
    public void removeCartItem(String title, String persistentId) throws Exception {
        if (session.getUser() instanceof AuthenticatedUser) {
            AuthenticatedUser authUser = (AuthenticatedUser) session.getUser();
            try {
                authUser.getCart().removeItem(title, persistentId);
                JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("dataset.compute.computeBatch.success"));
            } catch (Exception ex){
                JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("dataset.compute.computeBatch.failure"));
            }
        }
    }
    
    public void clearCart() throws Exception {
        if (session.getUser() instanceof AuthenticatedUser) {
            AuthenticatedUser authUser = (AuthenticatedUser) session.getUser();
            try {
                authUser.getCart().clear();
                JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("dataset.compute.computeBatch.success"));
            } catch (Exception ex){
                JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("dataset.compute.computeBatch.failure"));
            }
        }
    }
    
    public boolean isCartEmpty() {
        if (session.getUser() instanceof AuthenticatedUser) {
            AuthenticatedUser authUser = (AuthenticatedUser) session.getUser();
            return authUser.getCart().getContents().isEmpty();
        }
        return true;
    }
    
        
    public String getCartComputeUrl() {
        if (session.getUser() instanceof AuthenticatedUser) {
            AuthenticatedUser authUser = (AuthenticatedUser) session.getUser();
            String url = settingsWrapper.getValueForKey(SettingsServiceBean.Key.ComputeBaseUrl);
            if (url == null) {
                return "";
            }
            // url indicates that you are computing with multiple datasets
            url += "/multiparty?";
            List<Entry<String,String>> contents = authUser.getCart().getContents();
            for (Entry<String,String> entry : contents) {
                String persistentIdUrl = entry.getValue();
                url +=  persistentIdUrl + "&";
            }
            return url.substring(0, url.length() - 1);
        }
        return "";
    }

    private String fileLabelSearchTerm;

    public String getFileLabelSearchTerm() {
        return fileLabelSearchTerm;
    }

    public void setFileLabelSearchTerm(String fileLabelSearchTerm) {
        if (fileLabelSearchTerm != null) {
            this.fileLabelSearchTerm = fileLabelSearchTerm.trim();
        } else {
            this.fileLabelSearchTerm="";
        }
    }
    
    private String fileTypeFacet; 
    
    public String getFileTypeFacet() {
        return fileTypeFacet;
    }

    public void setFileTypeFacet(String fileTypeFacet) {
        if (fileTypeFacet != null) {
            this.fileTypeFacet = fileTypeFacet.trim();
        } 
    }
    
    private String fileAccessFacet; 
    
    public String getFileAccessFacet() {
        return fileAccessFacet;
    }

    public void setFileAccessFacet(String fileAccessFacet) {
        if (fileAccessFacet != null) {
            this.fileAccessFacet = fileAccessFacet.trim();
        } 
    }
    
    private String fileTagsFacet; 
    
    public String getFileTagsFacet() {
        return fileTagsFacet;
    }

    public void setFileTagsFacet(String fileTagsFacet) {
        if (fileTagsFacet != null) {
            this.fileTagsFacet = fileTagsFacet.trim();
        } 
    }
    
    private List<FileMetadata> fileMetadatasSearch;
    
    public List<FileMetadata> getFileMetadatasSearch() {
        return fileMetadatasSearch;
    }
    
    public void setFileMetadatasSearch(List<FileMetadata> fileMetadatasSearch) {
        this.fileMetadatasSearch = fileMetadatasSearch;
    }
    
    public void updateFileSearch(){  
        logger.fine("updating file search list");
        this.fileMetadatasSearch = selectFileMetadatasForDisplay();
        
    }
    
        private Long numberOfFilesToShow = (long) 25;

    public Long getNumberOfFilesToShow() {
        return numberOfFilesToShow;
    }

    public void setNumberOfFilesToShow(Long numberOfFilesToShow) {
        this.numberOfFilesToShow = numberOfFilesToShow;
    }
    
    public void showAll(){
        setNumberOfFilesToShow(new Long(fileMetadatasSearch.size()));
    }
    
    private List<FileMetadata> selectFileMetadatasForDisplay() {
        Set<Long> searchResultsIdSet = null;

        if (isIndexedVersion()) {
            // We run the search even if no search term and/or facets are 
            // specified - to generate the facet labels list:
            searchResultsIdSet = getFileIdsInVersionFromSolr(workingVersion.getId(), this.fileLabelSearchTerm);
            // But, if no search terms were specified, we can immediately return the full 
            // list of the files in the version: 
            if (StringUtil.isEmpty(fileLabelSearchTerm)
                    && StringUtil.isEmpty(fileTypeFacet)
                    && StringUtil.isEmpty(fileAccessFacet)
                    && StringUtil.isEmpty(fileTagsFacet)) {
                if ((StringUtil.isEmpty(fileSortField) || fileSortField.equals("name")) && StringUtil.isEmpty(fileSortOrder)) {
                    return workingVersion.getFileMetadatasSorted();
                } else {
                    searchResultsIdSet = null; 
                }
            }

        } else {
            // No, this is not an indexed version. 
            // If the search term was specified, we'll run a search in the db;
            // if not - return the full list of files in the version. 
            // (no facets without solr!)
            if (StringUtil.isEmpty(this.fileLabelSearchTerm)) {
                if ((StringUtil.isEmpty(fileSortField) || fileSortField.equals("name")) && StringUtil.isEmpty(fileSortOrder)) {
                    return workingVersion.getFileMetadatasSorted();
                }
            } else {
                searchResultsIdSet = getFileIdsInVersionFromDb(workingVersion.getId(), this.fileLabelSearchTerm);
            }
        }

        List<FileMetadata> retList = new ArrayList<>();

        for (FileMetadata fileMetadata : workingVersion.getFileMetadatasSorted()) {
            if (searchResultsIdSet == null || searchResultsIdSet.contains(fileMetadata.getDataFile().getId())) {
                retList.add(fileMetadata);
            }
        }

        if ((StringUtil.isEmpty(fileSortOrder) && !("name".equals(fileSortField))) 
                || ("desc".equals(fileSortOrder) || !("name".equals(fileSortField)))) {
            sortFileMetadatas(retList);
            
        }
        
        return retList;     
    }
    
    private void sortFileMetadatas(List<FileMetadata> fileList) {
        if ("name".equals(fileSortField) && "desc".equals(fileSortOrder)) {
            Collections.sort(fileList, compareByLabelZtoA);
        } else if ("date".equals(fileSortField)) {
            if ("desc".equals(fileSortOrder)) {
                Collections.sort(fileList, compareByOldest);
            } else {
                Collections.sort(fileList, compareByNewest);
            }
        } else if ("type".equals(fileSortField)) {
            Collections.sort(fileList, compareByType);
        } else if ("size".equals(fileSortField)) {
            Collections.sort(fileList, compareBySize);
        }
    }
    
    private Boolean isIndexedVersion = null; 
    
    public boolean isIndexedVersion() {
        if (isIndexedVersion != null) {
            return isIndexedVersion;
        }
        // The version is SUPPOSED to be indexed if it's the latest published version, or a 
        // draft. So if none of the above is true, we return false right away:
        
        if (!(workingVersion.isDraft() || isThisLatestReleasedVersion())) {
            return isIndexedVersion = false; 
        }
        
        // ... but if it is the latest published version or a draft, we want to test 
        // and confirm that this version *has* actually been indexed and is searchable   
        // (and that solr is actually up and running!), by running a quick solr search:
        return isIndexedVersion = isThisVersionSearchable();
    }
    
    /**
     * Finds the list of numeric datafile ids in the Version specified, by running
     * a database query.
     * 
     * @param datasetVersionId numeric version id
     * @param pattern string keyword
     * @return set of numeric ids
     * 
     */
    
    public Set<Long> getFileIdsInVersionFromDb(Long datasetVersionId, String pattern) {
        logger.fine("searching for file ids, in the database");
        List<Long> searchResultsIdList = datafileService.findDataFileIdsByDatasetVersionIdLabelSearchTerm(datasetVersionId, pattern, "", "");
        
        Set<Long> ret = new HashSet<>();
        for (Long id : searchResultsIdList) {
            ret.add(id);
        }
        
        return ret;
    }
    
    
    private Map<String, List<FacetLabel>> facetLabelsMap; 
    
    public Map<String, List<FacetLabel>> getFacetLabelsMap() {
        return facetLabelsMap;
    }
    
    public List<FacetLabel> getFileTypeFacetLabels() {
        if (facetLabelsMap != null) {
            return facetLabelsMap.get("fileTypeGroupFacet");
        }
        return null;
    }
    
    public List<FacetLabel> getFileAccessFacetLabels() {
        if (facetLabelsMap != null) {
            return facetLabelsMap.get("fileAccess");
        }
        return null;
    }
    
    public List<FacetLabel> getFileTagsFacetLabels() {
        if (facetLabelsMap != null) {
            return facetLabelsMap.get("fileTag");
        }
        return null;
    }
    
    /**
     * Verifies that solr is running and that the version is indexed and searchable
     * @return boolean
     */
    public boolean isThisVersionSearchable() {
        SolrQuery solrQuery = new SolrQuery();
        
        solrQuery.setQuery(SearchUtil.constructQuery(SearchFields.ENTITY_ID, workingVersion.getDataset().getId().toString()));
        
        solrQuery.addFilterQuery(SearchUtil.constructQuery(SearchFields.TYPE, SearchConstants.DATASETS));
        solrQuery.addFilterQuery(SearchUtil.constructQuery(SearchFields.DATASET_VERSION_ID, workingVersion.getId().toString()));
        
        logger.fine("Solr query (testing if searchable): " + solrQuery);
                
        QueryResponse queryResponse = null;
        
        try {
            queryResponse = solrClientService.getSolrClient().query(solrQuery);
        } catch (Exception ex) {
            logger.fine("Solr exception: " + ex.getLocalizedMessage());
            // solr maybe down/some error may have occurred... 
            return false; 
        }
        
        SolrDocumentList docs = queryResponse.getResults();
        Iterator<SolrDocument> iter = docs.iterator();
        
        // there should be only 1 result, really... 
        while (iter.hasNext()) {
            SolrDocument solrDocument = iter.next();
            Long entityid = (Long) solrDocument.getFieldValue(SearchFields.ENTITY_ID);
            logger.fine("solr result id: "+entityid);
            if (entityid.equals(workingVersion.getDataset().getId())) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Finds the list of numeric datafile ids in the Version specified, by running
     * a direct solr search query.
     * 
     * @param datasetVersionId numeric version id
     * @param pattern string keyword
     * @return set of numeric ids
     * 
     */
    public Set<Long> getFileIdsInVersionFromSolr(Long datasetVersionId, String pattern) {
        logger.fine("searching for file ids, in solr");
        
        SolrQuery solrQuery = new SolrQuery();
        
        List<String> queryStrings = new ArrayList<>();
        
        // Main query: 
        if (!StringUtil.isEmpty(pattern)) {
            // searching on the file name ("label") and description:
            queryStrings.add(SearchUtil.constructQuery(SearchFields.FILE_NAME, pattern + "*"));
            queryStrings.add(SearchUtil.constructQuery(SearchFields.FILE_DESCRIPTION, pattern + "*"));
            queryStrings.add(SearchUtil.constructQuery(SearchFields.FILE_TAG_SEARCHABLE, pattern + "*"));

            solrQuery.setQuery(SearchUtil.constructQuery(queryStrings, false));
        } else {
            // ... or "everything", if no search pattern is supplied: 
            // (presumably, one or more facet fields is supplied, below)
            solrQuery.setQuery("*");
        }
        
        
        // ask for facets: 
        
        solrQuery.setParam("facet", "true");
        /**
         * @todo: do we need facet.query?
         */
        solrQuery.setParam("facet.query", "*");
        
        solrQuery.addFacetField(SearchFields.FILE_TYPE);
        solrQuery.addFacetField(SearchFields.ACCESS);
        solrQuery.addFacetField(SearchFields.FILE_TAG);
        
        
        // Extra filter queries from the facets, if specified: 
        
        if (!StringUtil.isEmpty(fileTypeFacet)) {
            solrQuery.addFilterQuery(SearchFields.FILE_TYPE + ":" + fileTypeFacet);
        } 
        
        if (!StringUtil.isEmpty(fileAccessFacet)) {
            solrQuery.addFilterQuery(SearchFields.ACCESS + ":" + fileAccessFacet);
        }
        
        if (!StringUtil.isEmpty(fileTagsFacet)) {
            solrQuery.addFilterQuery(SearchFields.FILE_TAG + ":" + fileTagsFacet);
        }        
        
        // Additional filter queries, to restrict the results to files in this version only:
        
        solrQuery.addFilterQuery(SearchFields.TYPE + ":" + SearchConstants.FILES);
        if (!workingVersion.isDraft()) {
            solrQuery.addFilterQuery(SearchFields.DATASET_VERSION_ID + ":" + datasetVersionId);
        } else {
            // To avoid indexing duplicate solr documents, we don't index ALL of the files
            // in a draft version - only the new files added to the draft and/or the
            // files for which the metadata have been changed. 
            // So, in order to find all the files in the draft version, we can't just 
            // run a query with the dataset version id, like with the published version, 
            // above. Instead we are searching for all the indexed files in the dataset, 
            // except the ones indexed with the "fileDeleted" flag - that indicates that 
            // they are no longer in the draft:
            solrQuery.addFilterQuery(SearchFields.PARENT_ID + ":" + workingVersion.getDataset().getId());
            // Note that we don't want to use the query "fileDeleted: false" - that 
            // would only find the documents in which the fileDeleted boolean field 
            // is actually present, AND set to false. Instead we are searching with 
            // "!(fileDeleted: true)" - that will find ALL the records, except for 
            // the ones where the value is explicitly set to true.
            solrQuery.addFilterQuery("!(" + SearchFields.FILE_DELETED + ":" + true + ")");
            
        }

        // Unlimited number of search results: 
        // (but we are searching within one dataset(version), so it should be manageable)
        solrQuery.setRows(Integer.MAX_VALUE);

        logger.fine("Solr query (file search): " + solrQuery);
                
        QueryResponse queryResponse = null;
        boolean fileDeletedFlagNotIndexed = false; 
        Set<Long> resultIds = new HashSet<>();
        
        try {
            queryResponse = solrClientService.getSolrClient().query(solrQuery);
        } catch (HttpSolrClient.RemoteSolrException ex) {
            logger.fine("Remote Solr Exception: " + ex.getLocalizedMessage());
            String msg = ex.getLocalizedMessage(); 
            if (msg.contains(SearchFields.FILE_DELETED)) {
                fileDeletedFlagNotIndexed = true; 
            }
        } catch (Exception ex) {
            logger.warning("Solr exception: " + ex.getLocalizedMessage());
            return resultIds; 
        }
        
        if (fileDeletedFlagNotIndexed) {
            // try again, without the flag:
            solrQuery.removeFilterQuery("!(" + SearchFields.FILE_DELETED + ":" + true + ")");
            logger.fine("Solr query (trying again): " + solrQuery);

            try {
                queryResponse = solrClientService.getSolrClient().query(solrQuery);
            } catch (Exception ex) {
                logger.warning("Caught a Solr exception (again!): " + ex.getLocalizedMessage());
                return resultIds;
            }
        }
        
        // Process the facets: 
        
        facetLabelsMap = new HashMap<>();
        
        for (FacetField facetField : queryResponse.getFacetFields()) {
            List<FacetLabel> facetLabelList = new ArrayList<>();

            int count = 0;
            
            for (FacetField.Count facetFieldCount : facetField.getValues()) {
                logger.fine("facet field value: " + facetField.getName() + " " + facetFieldCount.getName() + " (" + facetFieldCount.getCount() + ")");
                if (facetFieldCount.getCount() > 0) {
                    FacetLabel facetLabel = new FacetLabel(facetFieldCount.getName(), facetFieldCount.getCount());
                    // quote facets arguments, just in case:
                    facetLabel.setFilterQuery(facetField.getName() + ":\"" + facetFieldCount.getName() + "\"");
                    facetLabelList.add(facetLabel);
                    count += facetFieldCount.getCount();
                }
            }
            
            if (count > 0) {
                facetLabelsMap.put(facetField.getName(), facetLabelList);
            }            
        }
        
        SolrDocumentList docs = queryResponse.getResults();
        Iterator<SolrDocument> iter = docs.iterator();
        
        while (iter.hasNext()) {
            SolrDocument solrDocument = iter.next();
            Long entityid = (Long) solrDocument.getFieldValue(SearchFields.ENTITY_ID);
            logger.fine("solr result id: "+entityid);
            resultIds.add(entityid);
        }
        
        return resultIds;
    }
    
    /*
        Save the setting locally so db isn't hit repeatedly
    
        This may be "null", signifying unlimited download size
    */
    public Long getMaxFileUploadSizeInBytes(){
        return this.maxFileUploadSizeInBytes;
    }
    
    public boolean isUnlimitedUploadFileSize(){
        
        if (this.maxFileUploadSizeInBytes == null){
            return true;
        }
        return false;
    }

    public String getDataverseSiteUrl() {
        return this.dataverseSiteUrl;
    }
    
    public void setDataverseSiteUrl(String dataverseSiteUrl) {
        this.dataverseSiteUrl = dataverseSiteUrl;
    }
    
    public DataFile getInitialDataFile() {
        if (workingVersion.getFileMetadatas() != null && workingVersion.getFileMetadatas().size() > 0) {
            return workingVersion.getFileMetadatas().get(0).getDataFile();
        }
        return null;
    }
    
    public SwiftAccessIO getSwiftObject() {
        try {
            StorageIO<DataFile> storageIO = getInitialDataFile() == null ? null : getInitialDataFile().getStorageIO();
            if (storageIO != null && storageIO instanceof SwiftAccessIO) {
                return (SwiftAccessIO)storageIO;
            } else {
                logger.fine("DatasetPage: Failed to cast storageIO as SwiftAccessIO (most likely because storageIO is a FileAccessIO)");
            } 
        } catch (IOException e) {
            logger.fine("DatasetPage: Failed to get storageIO");

        }
        return null;
    }

    public String getSwiftContainerName() throws IOException {
        SwiftAccessIO swiftObject = getSwiftObject();
        try {
            swiftObject.open();
            return swiftObject.getSwiftContainerName();
        } catch (Exception e){
            logger.info("DatasetPage: Failed to open swift object");
        }
        
        return "";
        
    }
    
    public void setSwiftContainerName(String name){
        
    }
    //This function applies to an entire dataset
    private boolean isSwiftStorage() {
        //containers without datafiles will not be stored in swift storage
        if (getInitialDataFile() != null){
            for (FileMetadata fmd : workingVersion.getFileMetadatas()) {
                //if any of the datafiles are stored in swift
                if (fmd.getDataFile().getStorageIdentifier().startsWith("swift://")) {
                    return true;
                }
            }
        }
        return false;
    }
    
    //This function applies to a single datafile
    private boolean isSwiftStorage(FileMetadata metadata){
        if (metadata.getDataFile().getStorageIdentifier().startsWith("swift://")) {
            return true;
        }
        return false;
    }
    
    
    private Boolean showComputeButtonForDataset = null;
    //This function applies to an entire dataset
    public boolean showComputeButton() {
        if (showComputeButtonForDataset != null) {
            return showComputeButtonForDataset;
        }
        
        if (isSwiftStorage() && (settingsService.getValueForKey(SettingsServiceBean.Key.ComputeBaseUrl) != null)) {
            showComputeButtonForDataset = true;
        } else {
            showComputeButtonForDataset = false;
        }
        return showComputeButtonForDataset;
    }
    
    private Map<Long, Boolean> showComputeButtonForFile = new HashMap<>();
    //this function applies to a single datafile
    public boolean showComputeButton(FileMetadata metadata) {
        Long fileId = metadata.getDataFile().getId();
        
        if (fileId == null) {
            return false;
        }
        
        if (showComputeButtonForFile.containsKey(fileId)) {
            return showComputeButtonForFile.get(fileId);
        }
        
        boolean result = isSwiftStorage(metadata) 
                && settingsService.getValueForKey(SettingsServiceBean.Key.ComputeBaseUrl) != null;
        
        showComputeButtonForFile.put(fileId, result);
        return result;
    }

    public boolean canComputeAllFiles(boolean isCartCompute){
        for (FileMetadata fmd : workingVersion.getFileMetadatas()) {
             if (!fileDownloadHelper.canDownloadFile(fmd)) {
                 //RequestContext requestContext = RequestContext.getCurrentInstance();
                 PrimeFaces.current().executeScript("PF('computeInvalid').show()");
                 return false;
             }
        }
        if (!isCartCompute) {
            try {
                FacesContext.getCurrentInstance().getExternalContext().redirect(getComputeUrl());
             } catch (IOException ioex) {
                 logger.warning("Failed to issue a redirect.");
             }
        }
        return true;
    }
    
    public boolean canDownloadFiles(){
        //returns true if the page user has permission to download at least one file
        for (FileMetadata fmd : workingVersion.getFileMetadatas()) {
             if (fileDownloadHelper.canDownloadFile(fmd)) {
                 return true;
             }
        }
        return false;
    }
    
    /*
    in getComputeUrl(), we are sending the container/dataset name and the exipiry and signature 
    for the temporary url of only ONE datafile within the dataset. This is because in the 
    ceph version of swift, we are only able to generate the temporary url for a single object 
    within a container.
    Ideally, we want a temporary url for an entire container/dataset, so perhaps this could instead
    be handled on the compute environment end. 
    Additionally, we have to think about the implications this could have with dataset versioning, 
    since we currently store all files (even from old versions) in the same container.
    --SF
    */
    public String getComputeUrl() throws IOException {

        return settingsWrapper.getValueForKey(SettingsServiceBean.Key.ComputeBaseUrl) + "?" + this.getPersistentId();
            //WHEN we are able to get a temp url for a dataset
            //return settingsWrapper.getValueForKey(SettingsServiceBean.Key.ComputeBaseUrl) + "?containerName=" + swiftObject.getSwiftContainerName() + "&temp_url_sig=" + swiftObject.getTempUrlSignature() + "&temp_url_expires=" + swiftObject.getTempUrlExpiry();

        
    }
    
    //For a single file
    public String getComputeUrl(FileMetadata metadata) {
        SwiftAccessIO swiftObject = null;
        try {
            StorageIO<DataFile> storageIO = metadata.getDataFile().getStorageIO();
            if (storageIO != null && storageIO instanceof SwiftAccessIO) {
                swiftObject = (SwiftAccessIO)storageIO;
                swiftObject.open();
            }

        } catch (IOException e) {
            logger.info("DatasetPage: Failed to get storageIO");
        }
        if (settingsWrapper.isTrueForKey(SettingsServiceBean.Key.PublicInstall, false)) {
            return settingsWrapper.getValueForKey(SettingsServiceBean.Key.ComputeBaseUrl) + "?" + this.getPersistentId() + "=" + swiftObject.getSwiftFileName();
        }
        
        return settingsWrapper.getValueForKey(SettingsServiceBean.Key.ComputeBaseUrl) + "?" + this.getPersistentId() + "=" + swiftObject.getSwiftFileName() + "&temp_url_sig=" + swiftObject.getTempUrlSignature() + "&temp_url_expires=" + swiftObject.getTempUrlExpiry();

    }
    
    public String getCloudEnvironmentName() {
        return settingsWrapper.getValueForKey(SettingsServiceBean.Key.CloudEnvironmentName);
    }
    
    public DataFile getSelectedDownloadFile() {
        return selectedDownloadFile;
    }

    public void setSelectedDownloadFile(DataFile selectedDownloadFile) {
        this.selectedDownloadFile = selectedDownloadFile;
    }
    
    public List<DataFile> getNewFiles() {
        return newFiles;
    }
    
    public void setNewFiles(List<DataFile> newFiles) {
        this.newFiles = newFiles;
    }
    
    public List<DataFile> getUploadedFiles() {
        return uploadedFiles;
    }
    
    public void setUploadedFiles(List<DataFile> uploadedFiles) {
        this.uploadedFiles = uploadedFiles;
    }
    
    public MutableBoolean getUploadInProgress() {
        return uploadInProgress;
    }
    
    public void setUploadInProgress(MutableBoolean inProgress) {
        this.uploadInProgress = inProgress;
    }
    
    public Dataverse getLinkingDataverse() {
        return linkingDataverse;
    }

    public void setLinkingDataverse(Dataverse linkingDataverse) {
        this.linkingDataverse = linkingDataverse;
    }

    public List<SelectItem> getLinkingDVSelectItems() {
        return linkingDVSelectItems;
    }

    public void setLinkingDVSelectItems(List<SelectItem> linkingDVSelectItems) {
        this.linkingDVSelectItems = linkingDVSelectItems;
    }

    public Long getLinkingDataverseId() {
        return linkingDataverseId;
    }

    public void setLinkingDataverseId(Long linkingDataverseId) {
        this.linkingDataverseId = linkingDataverseId;
    }


    
    public void updateReleasedVersions(){
        
        setReleasedVersionTabList(resetReleasedVersionTabList());
        
    }
    
    public void clickDeaccessionDataset(){
        setReleasedVersionTabList(resetReleasedVersionTabList());
        setRenderDeaccessionPopup(true);
    }
    
    private boolean renderDeaccessionPopup = false;

    public boolean isRenderDeaccessionPopup() {
        return renderDeaccessionPopup;
    }

    public void setRenderDeaccessionPopup(boolean renderDeaccessionPopup) {
        this.renderDeaccessionPopup = renderDeaccessionPopup;
    }
    
    public void updateSelectedLinkingDV(ValueChangeEvent event) {
        linkingDataverseId = (Long) event.getNewValue();
    }

    public boolean isNoDVsAtAll() {
        return noDVsAtAll;
    }

    public void setNoDVsAtAll(boolean noDVsAtAll) {
        this.noDVsAtAll = noDVsAtAll;
    }

    public boolean isNoDVsRemaining() {
        return noDVsRemaining;
    }
    

    private Map<Long, String> datafileThumbnailsMap = new HashMap<>();

    public boolean isThumbnailAvailable(FileMetadata fileMetadata) {
        
        // new and optimized logic: 
        // - check download permission here (should be cached - so it's free!)
        // - only then check if the thumbnail is available/exists.
        // then cache the results!
        
        Long dataFileId = fileMetadata.getDataFile().getId();
        
        if (datafileThumbnailsMap.containsKey(dataFileId)) {
            return !"".equals(datafileThumbnailsMap.get(dataFileId));
        }
        
        if (!FileUtil.isThumbnailSupported(fileMetadata.getDataFile())) {
            datafileThumbnailsMap.put(dataFileId, "");
            return false;
        }
        
        if (!this.fileDownloadHelper.canDownloadFile(fileMetadata)) {
            datafileThumbnailsMap.put(dataFileId, "");
            return false;
        }
     
        
        
        String thumbnailAsBase64 = ImageThumbConverter.getImageThumbnailAsBase64(fileMetadata.getDataFile(), ImageThumbConverter.DEFAULT_THUMBNAIL_SIZE);
        
        
        //if (datafileService.isThumbnailAvailable(fileMetadata.getDataFile())) {
        if (!StringUtil.isEmpty(thumbnailAsBase64)) {
            datafileThumbnailsMap.put(dataFileId, thumbnailAsBase64);
            return true;
        } 
        
        datafileThumbnailsMap.put(dataFileId, "");
        return false;
        
    }
    
    public String getDataFileThumbnailAsBase64(FileMetadata fileMetadata) {
        return datafileThumbnailsMap.get(fileMetadata.getDataFile().getId());
    }
    
    // Another convenience method - to cache Update Permission on the dataset: 
    public boolean canUpdateDataset() {
        return permissionsWrapper.canUpdateDataset(dvRequestService.getDataverseRequest(), this.dataset);
    }
    public boolean canPublishDataverse() {
        return permissionsWrapper.canIssuePublishDataverseCommand(dataset.getOwner());
    }
    
    public boolean canPublishDataset(){
        return permissionsWrapper.canIssuePublishDatasetCommand(dataset);
    }

    public boolean canViewUnpublishedDataset() {
        return permissionsWrapper.canViewUnpublishedDataset( dvRequestService.getDataverseRequest(), dataset);
    }
        
    /* 
     * 4.2.1 optimization. 
     * HOWEVER, this doesn't appear to be saving us anything! 
     * i.e., it's just as cheap to use session.getUser().isAuthenticated() 
     * every time; it doesn't do any new db lookups. 
    */
    public boolean isSessionUserAuthenticated() {
        return session.getUser().isAuthenticated();
    }
    
    /**
     * For use in the Dataset page
     * @return 
     */
    public boolean isSuperUser(){
        
        if (!this.isSessionUserAuthenticated()){
            return false;
        }
        
        if (this.session.getUser().isSuperuser()){
            return true;
        }
        return false;
    }
    /**
     * Check Dataset related permissions
     * 
     * @param permissionToCheck
     * @return 
     */
    public boolean doesSessionUserHaveDataSetPermission(Permission permissionToCheck){
        if (permissionToCheck == null){
            return false;
        }
               
        String permName = permissionToCheck.getHumanName();
       
        // Has this check already been done? 
        // 
        if (this.datasetPermissionMap.containsKey(permName)){
            // Yes, return previous answer
            return this.datasetPermissionMap.get(permName);
        }
        
        // Check the permission
        //
        boolean hasPermission = this.permissionService.userOn(this.session.getUser(), this.dataset).has(permissionToCheck);

        // Save the permission
        this.datasetPermissionMap.put(permName, hasPermission);
        
        // return true/false
        return hasPermission;
    }
    
    public void setNoDVsRemaining(boolean noDVsRemaining) {
        this.noDVsRemaining = noDVsRemaining;
    }

    private GuestbookResponse guestbookResponse;
    private Guestbook selectedGuestbook;

    public GuestbookResponse getGuestbookResponse() {
        return guestbookResponse;
    }

    public void setGuestbookResponse(GuestbookResponse guestbookResponse) {
        this.guestbookResponse = guestbookResponse;
    }

    public Guestbook getSelectedGuestbook() {
        return selectedGuestbook;
    }

    public void setSelectedGuestbook(Guestbook selectedGuestbook) {
        this.selectedGuestbook = selectedGuestbook;
    }

    public void viewSelectedGuestbook(Guestbook selectedGuestbook) {
        this.selectedGuestbook = selectedGuestbook;
    }

    public void reset() {
        dataset.setGuestbook(null);
    }

    public int getFilePaginatorPage() {
        return filePaginatorPage;
    }

    public void setFilePaginatorPage(int filePaginatorPage) {
        this.filePaginatorPage = filePaginatorPage;
    }
    
    
    public int getRowsPerPage() {
        return rowsPerPage;
    }

    public void setRowsPerPage(int rowsPerPage) {
        this.rowsPerPage = rowsPerPage;
    }

    public String getGlobalId() {
        return persistentId;
    }
        
    public String getPersistentId() {
        return persistentId;
    }

    public void setPersistentId(String persistentId) {
        this.persistentId = persistentId;
    }
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }    

    public String getShowVersionList() {
        return showVersionList;
    }

    public void setShowVersionList(String showVersionList) {
        this.showVersionList = showVersionList;
    }

    public String getShowOtherText() {
        return showOtherText;
    }

    public void setShowOtherText(String showOtherText) {
        this.showOtherText = showOtherText;
    }
    private String showOtherText = "false";

    public String getDeaccessionForwardURLFor() {
        return deaccessionForwardURLFor;
    }

    public void setDeaccessionForwardURLFor(String deaccessionForwardURLFor) {
        this.deaccessionForwardURLFor = deaccessionForwardURLFor;
    }
    private DatasetVersionDifference datasetVersionDifference;

    public String getDeaccessionReasonText() {
        return deaccessionReasonText;
    }

    public void setDeaccessionReasonText(String deaccessionReasonText) {
        this.deaccessionReasonText = deaccessionReasonText;
    }

    public String getDisplayCitation() {
        //displayCitation = dataset.getCitation(false, workingVersion);
        return displayCitation;
    }

    public void setDisplayCitation(String displayCitation) {
        this.displayCitation = displayCitation;
    }

    public String getDropBoxSelection() {
        return dropBoxSelection;
    }

    public String getDropBoxKey() {
        // Site-specific DropBox application registration key is configured 
        // via a JVM option under glassfish.
        //if (true)return "some-test-key";  // for debugging

        String configuredDropBoxKey = System.getProperty("dataverse.dropbox.key");
        if (configuredDropBoxKey != null) {
            return configuredDropBoxKey;
        }
        return "";
    }

    public void setDropBoxSelection(String dropBoxSelection) {
        this.dropBoxSelection = dropBoxSelection;
    }

    public Dataset getDataset() {
        return dataset;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    public DatasetVersion getWorkingVersion() {
        return workingVersion;
    }
    
    public Long getId() { return this.id; }
    public void setId(Long id) { this.id = id; }    

    public EditMode getEditMode() {
        return editMode;
    }

    public void setEditMode(EditMode editMode) {
        this.editMode = editMode;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }
    
    public Long getVersionId() {
        return versionId;
    }

    public void setVersionId(Long versionId) {
        this.versionId = versionId;
    }

    public int getSelectedTabIndex() {
        return selectedTabIndex;
    }

    public void setSelectedTabIndex(int selectedTabIndex) {
        this.selectedTabIndex = selectedTabIndex;
    }

    public int getReleaseRadio() {
        return releaseRadio;
    }

    public void setReleaseRadio(int releaseRadio) {
        this.releaseRadio = releaseRadio;
    }

    public String getDatasetNextMajorVersion() {
        return datasetNextMajorVersion;
    }

    public void setDatasetNextMajorVersion(String datasetNextMajorVersion) {
        this.datasetNextMajorVersion = datasetNextMajorVersion;
    }

    public String getDatasetNextMinorVersion() {
        return datasetNextMinorVersion;
    }

    public void setDatasetNextMinorVersion(String datasetNextMinorVersion) {
        this.datasetNextMinorVersion = datasetNextMinorVersion;
    }

    public int getDeaccessionReasonRadio() {
        return deaccessionReasonRadio;
    }

    public void setDeaccessionReasonRadio(int deaccessionReasonRadio) {
        this.deaccessionReasonRadio = deaccessionReasonRadio;
    }

    public int getDeaccessionRadio() {
        return deaccessionRadio;
    }

    public void setDeaccessionRadio(int deaccessionRadio) {
        this.deaccessionRadio = deaccessionRadio;
    }

    public List<Template> getDataverseTemplates() {
        return dataverseTemplates;
    }

    public void setDataverseTemplates(List<Template> dataverseTemplates) {
        this.dataverseTemplates = dataverseTemplates;
    }

    public Template getDefaultTemplate() {
        return defaultTemplate;
    }

    public void setDefaultTemplate(Template defaultTemplate) {
        this.defaultTemplate = defaultTemplate;
    }

    public Template getSelectedTemplate() {
        return selectedTemplate;
    }

    public void setSelectedTemplate(Template selectedTemplate) {
        this.selectedTemplate = selectedTemplate;
    }
    
    public void updateSelectedTemplate(ValueChangeEvent event) {
        
        selectedTemplate = (Template) event.getNewValue();
        if (selectedTemplate != null) {
            //then create new working version from the selected template
            workingVersion.updateDefaultValuesFromTemplate(selectedTemplate); 
            updateDatasetFieldInputLevels();
        } else { 
            workingVersion.initDefaultValues();
            updateDatasetFieldInputLevels();
        }
        resetVersionUI();
    }

    /*
    // Original
    private void updateDatasetFieldInputLevels() {
        Long dvIdForInputLevel = ownerId;

        if (!dataverseService.find(ownerId).isMetadataBlockRoot()) {
            dvIdForInputLevel = dataverseService.find(ownerId).getMetadataRootId();
        }
        for (DatasetField dsf : workingVersion.getFlatDatasetFields()) {
            DataverseFieldTypeInputLevel dsfIl = dataverseFieldTypeInputLevelService.findByDataverseIdDatasetFieldTypeId(dvIdForInputLevel, dsf.getDatasetFieldType().getId());
            if (dsfIl != null) {
                dsf.setInclude(dsfIl.isInclude());
            } else {
                dsf.setInclude(true);
            }
        }
    }*/

    /***
     *
     * Note: Updated to retrieve DataverseFieldTypeInputLevel objects in single query
     *
     */    
     private void updateDatasetFieldInputLevels() {
         Long dvIdForInputLevel = ownerId;
        
         // OPTIMIZATION (?): replaced "dataverseService.find(ownerId)" with 
         // simply dataset.getOwner()... saves us a few lookups.
         // TODO: could there possibly be any reason we want to look this 
         // dataverse up by the id here?? -- L.A. 4.2.1
         if (!dataset.getOwner().isMetadataBlockRoot()) {
             dvIdForInputLevel = dataset.getOwner().getMetadataRootId();
         }        
        
        /* ---------------------------------------------------------
            Map to hold DatasetFields  
            Format:  {  DatasetFieldType.id : DatasetField }
         --------------------------------------------------------- */
        // Initialize Map
        Map<Long, DatasetField> mapDatasetFields = new HashMap<>();   

        // Populate Map
         for (DatasetField dsf : workingVersion.getFlatDatasetFields()) {
            if (dsf.getDatasetFieldType().getId() != null){
                mapDatasetFields.put(dsf.getDatasetFieldType().getId(), dsf);
            }
        }      
        
        /* ---------------------------------------------------------
            Retrieve List of DataverseFieldTypeInputLevel objects
            Use the DatasetFieldType id's which are the Map's keys
         --------------------------------------------------------- */
        List<Long> idList = new ArrayList<>(mapDatasetFields.keySet());
        List<DataverseFieldTypeInputLevel> dsFieldTypeInputLevels = dataverseFieldTypeInputLevelService.findByDataverseIdAndDatasetFieldTypeIdList(dvIdForInputLevel, idList);
        
        /* ---------------------------------------------------------
            Iterate through List of DataverseFieldTypeInputLevel objects
            Call "setInclude" on its related DatasetField object
         --------------------------------------------------------- */
        for (DataverseFieldTypeInputLevel oneDSFieldTypeInputLevel : dsFieldTypeInputLevels){
            
            if (oneDSFieldTypeInputLevel != null) {
                // Is the DatasetField in the hash?    hash format: {  DatasetFieldType.id : DatasetField }
                DatasetField dsf = mapDatasetFields.get(oneDSFieldTypeInputLevel.getDatasetFieldType().getId());  
                if (dsf != null){
                    // Yes, call "setInclude"
                    dsf.setInclude(oneDSFieldTypeInputLevel.isInclude());
                    // remove from hash                
                    mapDatasetFields.remove(oneDSFieldTypeInputLevel.getDatasetFieldType().getId());    
                }
            }
        }  // end: updateDatasetFieldInputLevels
        
        /* ---------------------------------------------------------
            Iterate through any DatasetField objects remaining in the hash
            Call "setInclude(true) on each one
         --------------------------------------------------------- */
        for ( DatasetField dsf  : mapDatasetFields.values()) {
               if (dsf != null){
                   dsf.setInclude(true);
               }
        }
     }

    public void handleChange() {
        logger.fine("handle change");
        logger.fine("new value " + selectedTemplate.getId());
    }

    public void handleChangeButton() {

    }

    public boolean isShapefileType(FileMetadata fm) {
        if (fm == null) {
            return false;
        }
        if (fm.getDataFile() == null) {
            return false;
        }

        return fm.getDataFile().isShapefileType();
    }

    private void msg(String s){
        // System.out.println(s);
    }

    private List<FileMetadata> displayFileMetadata;

    public List<FileMetadata> getDisplayFileMetadata() {
        return displayFileMetadata;
    }

    public void setDisplayFileMetadata(List<FileMetadata> displayFileMetadata) {
        this.displayFileMetadata = displayFileMetadata;
    }
    
    private boolean readOnly = true; 
    
    public String init() {
        return init(true);
    }
    
    public String initCitation() {
        return init(false);
    }     
    
    public void updateOwnerDataverse() {
        if (selectedHostDataverse != null && selectedHostDataverse.getId() != null) {
            ownerId = selectedHostDataverse.getId();
            dataset.setOwner(selectedHostDataverse);
            logger.info("New host dataverse id: "+ownerId);
            // discard the dataset already created
            //If a global ID was already assigned, as is true for direct upload, keep it (if files were already uploaded, they are at the path corresponding to the existing global id)
            GlobalId gid = dataset.getGlobalId();
            dataset = new Dataset();
            if(gid!=null) {
            	dataset.setGlobalId(gid);
            }
         
            // initiate from scratch: (isolate the creation of a new dataset in its own method?)
            init(true);
            // rebuild the bred crumbs display:
            dataverseHeaderFragment.initBreadcrumbs(dataset);
        }       
    }
    
    public boolean rsyncUploadSupported() {

        return settingsWrapper.isRsyncUpload() && DatasetUtil.isAppropriateStorageDriver(dataset);
    }
    
    private String init(boolean initFull) {
  
        //System.out.println("_YE_OLDE_QUERY_COUNTER_");  // for debug purposes
        setDataverseSiteUrl(systemConfig.getDataverseSiteUrl());

        guestbookResponse = new GuestbookResponse();
        
        String nonNullDefaultIfKeyNotFound = "";
        protocol = settingsWrapper.getValueForKey(SettingsServiceBean.Key.Protocol, nonNullDefaultIfKeyNotFound);
        authority = settingsWrapper.getValueForKey(SettingsServiceBean.Key.Authority, nonNullDefaultIfKeyNotFound);
        if (this.getId() != null || versionId != null || persistentId != null) { // view mode for a dataset

            DatasetVersionServiceBean.RetrieveDatasetVersionResponse retrieveDatasetVersionResponse = null;

            // ---------------------------------------
            // Set the workingVersion and Dataset
            // ---------------------------------------           
            if (persistentId != null) {
                logger.fine("initializing DatasetPage with persistent ID " + persistentId);
                // Set Working Version and Dataset by PersistentID
                dataset = datasetService.findByGlobalId(persistentId);
                if (dataset == null) {
                    logger.warning("No such dataset: "+persistentId);
                    return permissionsWrapper.notFound();
                }
                logger.fine("retrieved dataset, id="+dataset.getId());
                
                retrieveDatasetVersionResponse = datasetVersionService.selectRequestedVersion(dataset.getVersions(), version);
                //retrieveDatasetVersionResponse = datasetVersionService.retrieveDatasetVersionByPersistentId(persistentId, version);
                this.workingVersion = retrieveDatasetVersionResponse.getDatasetVersion();
                logger.fine("retrieved version: id: " + workingVersion.getId() + ", state: " + this.workingVersion.getVersionState());

            } else if (this.getId() != null) {
                // Set Working Version and Dataset by Datasaet Id and Version
                dataset = datasetService.find(this.getId());
                if (dataset == null) {
                    logger.warning("No such dataset: "+dataset);
                    return permissionsWrapper.notFound();
                }
                //retrieveDatasetVersionResponse = datasetVersionService.retrieveDatasetVersionById(dataset.getId(), version);
                retrieveDatasetVersionResponse = datasetVersionService.selectRequestedVersion(dataset.getVersions(), version);
                this.workingVersion = retrieveDatasetVersionResponse.getDatasetVersion();
                logger.info("retreived version: id: " + workingVersion.getId() + ", state: " + this.workingVersion.getVersionState());

            } else if (versionId != null) {
                // TODO: 4.2.1 - this method is broken as of now!
                // Set Working Version and Dataset by DatasaetVersion Id
                //retrieveDatasetVersionResponse = datasetVersionService.retrieveDatasetVersionByVersionId(versionId);

            }
            this.maxFileUploadSizeInBytes = systemConfig.getMaxFileUploadSizeForStore(dataset.getEffectiveStorageDriverId());


            if (retrieveDatasetVersionResponse == null) {
                return permissionsWrapper.notFound();
            }

            
            //this.dataset = this.workingVersion.getDataset();

            // end: Set the workingVersion and Dataset
            // ---------------------------------------
            // Is the DatasetVersion or Dataset null?
            //
            if (workingVersion == null || this.dataset == null) {
                return permissionsWrapper.notFound();
            }

            // Is the Dataset harvested?
            
            if (dataset.isHarvested()) {
                // if so, we'll simply forward to the remote URL for the original
                // source of this harvested dataset:
                String originalSourceURL = dataset.getRemoteArchiveURL();
                if (originalSourceURL != null && !originalSourceURL.equals("")) {
                    logger.fine("redirecting to "+originalSourceURL);
                    try {
                        FacesContext.getCurrentInstance().getExternalContext().redirect(originalSourceURL);
                    } catch (IOException ioex) {
                        // must be a bad URL...
                        // we don't need to do anything special here - we'll redirect
                        // to the local 404 page, below.
                        logger.warning("failed to issue a redirect to "+originalSourceURL);
                    }
                    return originalSourceURL;
                }

                return permissionsWrapper.notFound();
            }
              
            // Check permisisons           
            if (!(workingVersion.isReleased() || workingVersion.isDeaccessioned()) && !this.canViewUnpublishedDataset()) {
                return permissionsWrapper.notAuthorized();
            }

            if (!retrieveDatasetVersionResponse.wasRequestedVersionRetrieved()) {
                //msg("checkit " + retrieveDatasetVersionResponse.getDifferentVersionMessage());
                JsfHelper.addWarningMessage(retrieveDatasetVersionResponse.getDifferentVersionMessage());//BundleUtil.getStringFromBundle("dataset.message.metadataSuccess"));
            }
            
            // init the citation
            displayCitation = dataset.getCitation(true, workingVersion);
            
            if(workingVersion.isPublished()) {
                MakeDataCountEntry entry = new MakeDataCountEntry(FacesContext.getCurrentInstance(), dvRequestService, workingVersion);
                mdcLogService.logEntry(entry);
            }
            displayWorkflowComments();
            
            
            if (initFull) {
                // init the list of FileMetadatas
                if (workingVersion.isDraft() && canUpdateDataset()) {
                    readOnly = false;
                } else {
                    // an attempt to retreive both the filemetadatas and datafiles early on, so that 
                    // we don't have to do so later (possibly, many more times than necessary):
                    datafileService.findFileMetadataOptimizedExperimental(dataset);
                }
                
                // This will default to all the files in the version, if the search term
                // parameter hasn't been specified yet:
                fileMetadatasSearch = selectFileMetadatasForDisplay();

                ownerId = dataset.getOwner().getId();
                datasetNextMajorVersion = this.dataset.getNextMajorVersionString();
                datasetNextMinorVersion = this.dataset.getNextMinorVersionString();
                datasetVersionUI = datasetVersionUI.initDatasetVersionUI(workingVersion, false);
                updateDatasetFieldInputLevels();

                setExistReleasedVersion(resetExistRealeaseVersion());
                //moving setVersionTabList to tab change event
                //setVersionTabList(resetVersionTabList());
                //setReleasedVersionTabList(resetReleasedVersionTabList());
                //SEK - lazymodel may be needed for datascroller in future release
                // lazyModel = new LazyFileMetadataDataModel(workingVersion.getId(), datafileService );
                this.guestbookResponse = guestbookResponseService.initGuestbookResponseForFragment(workingVersion, null, session);
                logger.fine("Checking if rsync support is enabled.");
                if (DataCaptureModuleUtil.rsyncSupportEnabled(settingsWrapper.getValueForKey(SettingsServiceBean.Key.UploadMethods))
                        && dataset.getFiles().isEmpty()) { //only check for rsync if no files exist
                    try {
                        ScriptRequestResponse scriptRequestResponse = commandEngine.submit(new RequestRsyncScriptCommand(dvRequestService.getDataverseRequest(), dataset));
                        logger.fine("script: " + scriptRequestResponse.getScript());
                        if (scriptRequestResponse.getScript() != null && !scriptRequestResponse.getScript().isEmpty()) {
                            setHasRsyncScript(true);
                            setRsyncScript(scriptRequestResponse.getScript());
                            rsyncScriptFilename = "upload-" + workingVersion.getDataset().getIdentifier() + ".bash";
                            rsyncScriptFilename = rsyncScriptFilename.replace("/", "_");
                        } else {
                            setHasRsyncScript(false);
                        }
                    } catch (RuntimeException ex) {
                        logger.warning("Problem getting rsync script: " + ex.getLocalizedMessage());
                    } catch (CommandException cex) {
                        logger.warning("Problem getting rsync script (Command Exception): " + cex.getLocalizedMessage());
                    }
                }

            }                       
        } else if (ownerId != null) {
            // create mode for a new child dataset
            readOnly = false; 
            editMode = EditMode.CREATE;
            selectedHostDataverse = dataverseService.find(ownerId);
            dataset.setOwner(selectedHostDataverse);
            dataset.setProtocol(protocol);
            dataset.setAuthority(authority);

            if (dataset.getOwner() == null) {
                return permissionsWrapper.notFound();
            } else if (!permissionService.on(dataset.getOwner()).has(Permission.AddDataset)) {
                return permissionsWrapper.notAuthorized(); 
            }
            //Wait until the create command before actually getting an identifier, except if we're using directUpload  
        	//Need to assign an identifier prior to calls to requestDirectUploadUrl if direct upload is used.
            if ( isEmpty(dataset.getIdentifier()) && systemConfig.directUploadEnabled(dataset) ) {
            	CommandContext ctxt = commandEngine.getContext();
            	GlobalIdServiceBean idServiceBean = GlobalIdServiceBean.getBean(ctxt);
                dataset.setIdentifier(ctxt.datasets().generateDatasetIdentifier(dataset, idServiceBean));
            }                        
            dataverseTemplates.addAll(dataverseService.find(ownerId).getTemplates());
            if (!dataverseService.find(ownerId).isTemplateRoot()) {
                for (Template templateTest: dataverseService.find(ownerId).getParentTemplates()){
                   if(!dataverseTemplates.contains(templateTest)){
                       dataverseTemplates.add(templateTest);
                   }
                }               
            }
            Collections.sort(dataverseTemplates, (Template t1, Template t2) -> t1.getName().compareToIgnoreCase(t2.getName()));

            defaultTemplate = dataverseService.find(ownerId).getDefaultTemplate();
            if (defaultTemplate != null) {
                selectedTemplate = defaultTemplate;
                for (Template testT : dataverseTemplates) {
                    if (defaultTemplate.getId().equals(testT.getId())) {
                        selectedTemplate = testT;
                    }
                }
                workingVersion = dataset.getEditVersion(selectedTemplate, null);
                updateDatasetFieldInputLevels();
            } else {
                workingVersion = dataset.getCreateVersion();
                updateDatasetFieldInputLevels();
            }
            
            if (settingsWrapper.isTrueForKey(SettingsServiceBean.Key.PublicInstall, false)){
                JH.addMessage(FacesMessage.SEVERITY_WARN, BundleUtil.getStringFromBundle("dataset.message.label.fileAccess"), 
                        BundleUtil.getStringFromBundle("dataset.message.publicInstall"));
            }

            resetVersionUI();

            // FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Add New Dataset", " - Enter metadata to create the dataset's citation. You can add more metadata about this dataset after it's created."));
        } else {
            return permissionsWrapper.notFound();
        }
        try {
            privateUrl = commandEngine.submit(new GetPrivateUrlCommand(dvRequestService.getDataverseRequest(), dataset));
            if (privateUrl != null) {
                JH.addMessage(FacesMessage.SEVERITY_INFO, BundleUtil.getStringFromBundle("dataset.privateurl.header"), 
                        BundleUtil.getStringFromBundle("dataset.privateurl.infoMessageAuthor", Arrays.asList(getPrivateUrlLink(privateUrl))));
            }
        } catch (CommandException ex) {
            // No big deal. The user simply doesn't have access to create or delete a Private URL.
        }
        if (session.getUser() instanceof PrivateUrlUser) {
            PrivateUrlUser privateUrlUser = (PrivateUrlUser) session.getUser();
            if (dataset != null && dataset.getId().equals(privateUrlUser.getDatasetId())) {
                JH.addMessage(FacesMessage.SEVERITY_WARN, BundleUtil.getStringFromBundle("dataset.privateurl.header"), 
                        BundleUtil.getStringFromBundle("dataset.privateurl.infoMessageReviewer"));
            }
        }
                
        displayLockInfo(dataset);
            
        for (FileMetadata fmd : workingVersion.getFileMetadatas()) {
            if (fmd.getDataFile().isTabularData()) {
                versionHasTabular = true;
                break;
            }
        }
        for(DataFile f : dataset.getFiles()) {
            // TODO: Consider uncommenting this optimization.
//            if (versionHasTabular) {
//                hasTabular = true;
//                break;
//            }
            if(f.isTabularData()) {
                hasTabular = true;
                break;
            }
        }
        //Show ingest success message if refresh forces a page reload after ingest success
        //This is needed to display the explore buttons (the fileDownloadHelper needs to be reloaded via page 
        if (showIngestSuccess) {
            JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("dataset.unlocked.ingest.message"));
        }
        
        configureTools = externalToolService.findFileToolsByType(ExternalTool.Type.CONFIGURE);
        exploreTools = externalToolService.findFileToolsByType(ExternalTool.Type.EXPLORE);
        previewTools = externalToolService.findFileToolsByType(ExternalTool.Type.PREVIEW);
        datasetExploreTools = externalToolService.findDatasetToolsByType(ExternalTool.Type.EXPLORE);
        rowsPerPage = 10;
      
        
        
        return null;
    }
    
    private void displayWorkflowComments() {
        List<WorkflowComment> comments = workingVersion.getWorkflowComments();
        for (WorkflowComment wfc : comments) {
            if (wfc.isToBeShown() && wfc.getDatasetVersion().equals(workingVersion)
                    && wfc.getAuthenticatedUser().equals(session.getUser())) {
                if (wfc.getType() == WorkflowComment.Type.WORKFLOW_SUCCESS) {
                    JsfHelper.addSuccessMessage(wfc.getMessage());

                } else if (wfc.getType() == WorkflowComment.Type.WORKFLOW_FAILURE) {
                    JsfHelper.addWarningMessage(wfc.getMessage());
                }
                datasetService.markWorkflowCommentAsRead(wfc);
            }
        }
    }

    private void displayLockInfo(Dataset dataset) {
        // Various info messages, when the dataset is locked (for various reasons):
        if (dataset.isLocked() && canUpdateDataset()) {
            if (dataset.isLockedFor(DatasetLock.Reason.Workflow)) {
                JH.addMessage(FacesMessage.SEVERITY_WARN, BundleUtil.getStringFromBundle("dataset.locked.message"),
                        BundleUtil.getStringFromBundle("dataset.locked.message.details"));
            }
            if (dataset.isLockedFor(DatasetLock.Reason.InReview)) {
                JH.addMessage(FacesMessage.SEVERITY_WARN, BundleUtil.getStringFromBundle("dataset.locked.inReview.message"),
                        BundleUtil.getStringFromBundle("dataset.inreview.infoMessage"));
            }
            if (dataset.isLockedFor(DatasetLock.Reason.DcmUpload)) {
                JH.addMessage(FacesMessage.SEVERITY_WARN, BundleUtil.getStringFromBundle("file.rsyncUpload.inProgressMessage.summary"),
                        BundleUtil.getStringFromBundle("file.rsyncUpload.inProgressMessage.details"));
                lockedDueToDcmUpload = true;
            }
            //This is a hack to remove dataset locks for File PID registration if 
            //the dataset is released
            //in testing we had cases where datasets with 1000 files were remaining locked after being published successfully
            /*if(dataset.getLatestVersion().isReleased() && dataset.isLockedFor(DatasetLock.Reason.finalizePublication)){
                datasetService.removeDatasetLocks(dataset.getId(), DatasetLock.Reason.finalizePublication);
            }*/
            if (dataset.isLockedFor(DatasetLock.Reason.finalizePublication)) {
                // "finalizePublication" lock is used to lock the dataset while 
                // the FinalizeDatasetPublicationCommand is running asynchronously. 
                // the tasks currently performed by the command are the  pid registration 
                // for files and (or) physical file validation (either or both 
                // of these two can be disabled via database settings). More 
                // such asynchronous processing tasks may be added in the future. 
                JH.addMessage(FacesMessage.SEVERITY_WARN, BundleUtil.getStringFromBundle("dataset.publish.workflow.message"),
                        BundleUtil.getStringFromBundle("dataset.pidRegister.workflow.inprogress"));
            }
            if (dataset.isLockedFor(DatasetLock.Reason.FileValidationFailed)) {
                // the dataset is locked, because one or more datafiles in it 
                // failed validation during an attempt to publish it. 
                JH.addMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("dataset.publish.file.validation.error.message"),
                        BundleUtil.getStringFromBundle("dataset.publish.file.validation.error.contactSupport"));
            } 
            if (dataset.isLockedFor(DatasetLock.Reason.EditInProgress)) {
                JH.addMessage(FacesMessage.SEVERITY_WARN, BundleUtil.getStringFromBundle("dataset.locked.editInProgress.message"),
                        BundleUtil.getStringFromBundle("dataset.locked.editInProgress.message.details", Arrays.asList(BrandingUtil.getSupportTeamName(null))));
            }
        }
        
        if (dataset.isLockedFor(DatasetLock.Reason.Ingest)) {
            JH.addMessage(FacesMessage.SEVERITY_WARN, BundleUtil.getStringFromBundle("dataset.locked.message"),
                    BundleUtil.getStringFromBundle("dataset.locked.ingest.message"));
            lockedDueToIngestVar = true;
        }

        // With DataCite, we try to reserve the DOI when the dataset is created. Sometimes this
        // fails because DataCite is down. We show the message below to set expectations that the
        // "Publish" button won't work until the DOI has been reserved using the "Reserve PID" API.
        if (settingsWrapper.isDataCiteInstallation() && dataset.getGlobalIdCreateTime() == null && editMode != EditMode.CREATE) {
            JH.addMessage(FacesMessage.SEVERITY_WARN, BundleUtil.getStringFromBundle("dataset.locked.pidNotReserved.message"),
                    BundleUtil.getStringFromBundle("dataset.locked.pidNotReserved.message.details"));
        }

    }
    
    private Boolean fileTreeViewRequired = null; 
    
    public boolean isFileTreeViewRequired() {
        if (fileTreeViewRequired == null) {
            fileTreeViewRequired = workingVersion.getFileMetadatas().size() > 1 
                    && datafileService.isFoldersMetadataPresentInVersion(workingVersion);
        }
        return fileTreeViewRequired; 
    }
    
    public enum FileDisplayStyle {

        TABLE, TREE
    };
    
    private FileDisplayStyle fileDisplayMode = FileDisplayStyle.TABLE; 
    
    public String getFileDisplayMode() {
        return fileDisplayMode.equals(FileDisplayStyle.TABLE) ? "Table" : "Tree";
    }
    
    public void setFileDisplayMode(String fileDisplayMode) {
        if ("Table".equals(fileDisplayMode)) {
            this.fileDisplayMode = FileDisplayStyle.TABLE;
        } else {
            this.fileDisplayMode = FileDisplayStyle.TREE;
        } 
    }
    
    public boolean isFileDisplayTable() {
        return fileDisplayMode == FileDisplayStyle.TABLE;
    }
    
    public void toggleFileDisplayMode() {
        if (fileDisplayMode == FileDisplayStyle.TABLE) {
            fileDisplayMode = FileDisplayStyle.TREE;
        } else {
            fileDisplayMode = FileDisplayStyle.TABLE;
        }
    }
    public boolean isFileDisplayTree() {
        return fileDisplayMode == FileDisplayStyle.TREE;
    }
    
    private TreeNode filesTreeRoot = null; 
    
    public TreeNode getFilesTreeRoot() {
        if (filesTreeRoot == null) {
            initFilesTree();
        }
        return filesTreeRoot;
    }
    
    private void initFilesTree() {
        filesTreeRoot = createFolderTreeNode("root", null);
        TreeNode currentNode = filesTreeRoot;
        // this is a temporary map, that we keep while we are building 
        // the tree - in order to have direct access to the ancestor tree
        // nodes that have already been created:
        Map<String, TreeNode> folderMap = new HashMap<>();
        boolean expandFolders = true; 
        
        for ( FileMetadata fileMetadata :workingVersion.getFileMetadatasSortedByLabelAndFolder()) {
            String folder = fileMetadata.getDirectoryLabel();
            
            logger.fine("current folder: "+folder+"; current label: "+fileMetadata.getLabel());
            
            if (StringUtil.isEmpty(folder)) {
                filesTreeRoot.getChildren().add(createFileTreeNode(fileMetadata, filesTreeRoot));
            } else {
                if (folderMap.containsKey(folder)) {
                    // We have already created this node; and since all the FileMetadatas 
                    // are sorted by folder-then-label, it is safe to assume this is 
                    // still the "current node":
                    currentNode.getChildren().add(createFileTreeNode(fileMetadata, currentNode));
                } else {
                    // no node for this folder yet - need to create!

                    String[] subfolders = folder.split("/");
                    int level = 0;
                    currentNode = filesTreeRoot;

                    while (level < subfolders.length) {
                        String folderPath = subfolders[0];
                        for (int i = 1; i < level + 1; i++) {
                            folderPath = folderPath.concat("/").concat(subfolders[i]);
                        }

                        if (folderMap.containsKey(folderPath)) {
                            // jump directly to that ancestor folder node:
                            currentNode = folderMap.get(folderPath);
                        } else {
                            // create a new folder node:
                            currentNode = createFolderTreeNode(subfolders[level], currentNode);
                            folderMap.put(folderPath, currentNode);
                            // all the folders, except for the top-level root node 
                            // are collapsed by default:
                            currentNode.setExpanded(expandFolders);

                        }
                        level++;
                    }
                    currentNode.getChildren().add(createFileTreeNode(fileMetadata, currentNode));
                    // As soon as we reach the first folder containing files, we want
                    // to have all the other folders collapsed by default:
                    if (expandFolders) {
                        expandFolders = false; 
                    }
                }
            }
        }
        
        folderMap = null; 

    }
    
    private DefaultTreeNode createFolderTreeNode(String name, TreeNode parent) {
        // For a tree node representing a folder, we use its name, as a String, 
        // as the node data payload. (meaning, in the xhtml the folder name can
        // be shown as simply "#{node}". 
        // If we ever want to have more information shown for folders in the 
        // tree view (for example, we could show the number of files and sub-folders
        // in each folder next to the name), we will have to define a custom class 
        // and use it instead of the string in the DefaultTreeNode constructor
        // below:
        DefaultTreeNode folderNode = new DefaultTreeNode(name, parent);
        return folderNode; 
    }
    
    private DefaultTreeNode createFileTreeNode(FileMetadata fileMetadata, TreeNode parent) {
        // For a tree node representing a DataFile, we pack the entire FileMetadata 
        // object into the node, as its "data" payload. 
        // Note that we are using a custom node type ("customFileNode"), defined 
        // in the page xhtml.
        // If we ever want to have customized nodes that display different types 
        // of information for different types of files (tab. files would be a 
        // natural case), more custom nodes could be defined.
        
        DefaultTreeNode fileNode = new DefaultTreeNode("customFileNode", fileMetadata, parent);         
        
        return fileNode; 
    }
    
    public boolean isHasTabular() {
        return hasTabular;
    }

    public boolean isVersionHasTabular() {
        return versionHasTabular;
    }

    public boolean isReadOnly() {
        return readOnly; 
    }

    private void resetVersionUI() {
        
        datasetVersionUI = datasetVersionUI.initDatasetVersionUI(workingVersion, true);
        if (isSessionUserAuthenticated()) {
            AuthenticatedUser au = (AuthenticatedUser) session.getUser();

            //On create set pre-populated fields
            for (DatasetField dsf : dataset.getEditVersion().getDatasetFields()) {
                if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.depositor) && dsf.isEmpty()) {
                    dsf.getDatasetFieldValues().get(0).setValue(au.getLastName() + ", " + au.getFirstName());
                }
                if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.dateOfDeposit) && dsf.isEmpty()) {
                    dsf.getDatasetFieldValues().get(0).setValue(new SimpleDateFormat("yyyy-MM-dd").format(new Timestamp(new Date().getTime())));
                }

                if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.datasetContact) && dsf.isEmpty()) {
                    for (DatasetFieldCompoundValue contactValue : dsf.getDatasetFieldCompoundValues()) {
                        for (DatasetField subField : contactValue.getChildDatasetFields()) {
                            if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.datasetContactName)) {
                                subField.getDatasetFieldValues().get(0).setValue(au.getLastName() + ", " + au.getFirstName());
                            }
                            if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.datasetContactAffiliation)) {
                                subField.getDatasetFieldValues().get(0).setValue(au.getAffiliation());
                            }
                            if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.datasetContactEmail)) {
                                subField.getDatasetFieldValues().get(0).setValue(au.getEmail());
                            }
                        }
                    }
                }

                String creatorOrcidId = au.getOrcidId();
                if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.author) && dsf.isEmpty()) {
                    for (DatasetFieldCompoundValue authorValue : dsf.getDatasetFieldCompoundValues()) {
                        for (DatasetField subField : authorValue.getChildDatasetFields()) {
                            if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.authorName)) {
                                subField.getDatasetFieldValues().get(0).setValue(au.getLastName() + ", " + au.getFirstName());
                            }
                            if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.authorAffiliation)) {
                                subField.getDatasetFieldValues().get(0).setValue(au.getAffiliation());
                            }
                            if (creatorOrcidId != null) {
                                if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.authorIdValue)) {
                                    subField.getDatasetFieldValues().get(0).setValue(creatorOrcidId);
                                }
                                if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.authorIdType)) {  
                                   DatasetFieldType authorIdTypeDatasetField = fieldService.findByName(DatasetFieldConstant.authorIdType);
                                   subField.setSingleControlledVocabularyValue(fieldService.findControlledVocabularyValueByDatasetFieldTypeAndStrValue(authorIdTypeDatasetField, "ORCID", true));
                                }                                
                            }
                        }
                    }
                }
            }
        }
    }
    
    
    private void refreshSelectedFiles(List<FileMetadata> filesToRefresh){
        if (readOnly) {
            dataset = datasetService.find(dataset.getId());
        }
        String termsOfAccess = workingVersion.getTermsOfUseAndAccess().getTermsOfAccess();
        boolean requestAccess = workingVersion.getTermsOfUseAndAccess().isFileAccessRequest();
        workingVersion = dataset.getEditVersion();
        workingVersion.getTermsOfUseAndAccess().setTermsOfAccess(termsOfAccess);
        workingVersion.getTermsOfUseAndAccess().setFileAccessRequest(requestAccess);
        List <FileMetadata> newSelectedFiles = new ArrayList<>();
        for (FileMetadata fmd : filesToRefresh){
            for (FileMetadata fmdn: workingVersion.getFileMetadatas()){
                if (fmd.getDataFile().equals(fmdn.getDataFile())){
                    newSelectedFiles.add(fmdn);
                }
            }
        }
        
        filesToRefresh.clear();
        for (FileMetadata fmdn : newSelectedFiles ){
            filesToRefresh.add(fmdn);
        }
        readOnly = false;
    }

    private Integer chunkSize = 25;

    public Integer getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(Integer chunkSize) {
        this.chunkSize = chunkSize;
    }
    
    public void viewAllButtonPress(){
        setChunkSize(fileMetadatasSearch.size());
    }
    
     private int activeTabIndex;

    public int getActiveTabIndex() {
        return activeTabIndex;
    }

    public void setActiveTabIndex(int activeTabIndex) {
        this.activeTabIndex = activeTabIndex;
    }
    
    public void tabChanged(TabChangeEvent event) {
        TabView tv = (TabView) event.getComponent();
        this.activeTabIndex = tv.getActiveIndex();
        if (this.activeTabIndex == 3) {
            setVersionTabList(resetVersionTabList());
            setReleasedVersionTabList(resetReleasedVersionTabList());
        } else {
            releasedVersionTabList = new ArrayList<>();
            versionTabList = new ArrayList<>();
            if(this.activeTabIndex == 0) {
                 init();
            }          
        }
    }

    public void edit(EditMode editMode) {
        this.editMode = editMode;
        if (this.readOnly) {
            dataset = datasetService.find(dataset.getId());
        }
        workingVersion = dataset.getEditVersion();
        clone = workingVersion.cloneDatasetVersion();
        if (editMode == EditMode.INFO) {
            // ?
        } else if (editMode == EditMode.FILE) {
            // JH.addMessage(FacesMessage.SEVERITY_INFO, BundleUtil.getStringFromBundle("dataset.message.editFiles"));
            // FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Upload + Edit Dataset Files", " - You can drag and drop your files from your desktop, directly into the upload widget."));
        } else if (editMode.equals(EditMode.METADATA)) {
            datasetVersionUI = datasetVersionUI.initDatasetVersionUI(workingVersion, true);
            updateDatasetFieldInputLevels();
            JH.addMessage(FacesMessage.SEVERITY_INFO, BundleUtil.getStringFromBundle("dataset.message.editMetadata.label"), BundleUtil.getStringFromBundle("dataset.message.editMetadata.message"));
            //FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Edit Dataset Metadata", " - Add more metadata about your dataset to help others easily find it."));
        } else if (editMode.equals(EditMode.LICENSE)){
            JH.addMessage(FacesMessage.SEVERITY_INFO, BundleUtil.getStringFromBundle("dataset.message.editTerms.label"), BundleUtil.getStringFromBundle("dataset.message.editTerms.message"));
            //FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Edit Dataset License and Terms", " - Update your dataset's license and terms of use."));
        }
        this.readOnly = false;
    }

    public String sendBackToContributor() {
        try {
            //FIXME - Get Return Comment from sendBackToContributor popup
            Command<Dataset> cmd = new ReturnDatasetToAuthorCommand(dvRequestService.getDataverseRequest(), dataset, "");
            dataset = commandEngine.submit(cmd);
            JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("dataset.reject.success"));
        } catch (CommandException ex) {
            String message = ex.getMessage();
            logger.log(Level.SEVERE, "sendBackToContributor: {0}", message);
            JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("dataset.reject.failure", Collections.singletonList(message)));
        }
        
        /* 
         The notifications below are redundant, since the ReturnDatasetToAuthorCommand
         sends them already. - L.A. Sep. 7 2017
         
        List<AuthenticatedUser> authUsers = permissionService.getUsersWithPermissionOn(Permission.PublishDataset, dataset);
        List<AuthenticatedUser> editUsers = permissionService.getUsersWithPermissionOn(Permission.EditDataset, dataset);

        editUsers.removeAll(authUsers);
        new HashSet<>(editUsers).forEach( au -> 
            userNotificationService.sendNotification(au, new Timestamp(new Date().getTime()), 
                                                     UserNotification.Type.RETURNEDDS, dataset.getLatestVersion().getId())
        );
        */

        //FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, "DatasetSubmitted", "This dataset has been sent back to the contributor.");
        //FacesContext.getCurrentInstance().addMessage(null, message);
        return  returnToLatestVersion();
    }

    public String submitDataset() {
        try {
            Command<Dataset> cmd = new SubmitDatasetForReviewCommand( dvRequestService.getDataverseRequest(), dataset);
            dataset = commandEngine.submit(cmd);
            //JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("dataset.submit.success"));
        } catch (CommandException ex) {
            String message = ex.getMessage();
            logger.log(Level.SEVERE, "submitDataset: {0}", message);
            JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("dataset.submit.failure", Collections.singletonList(message)));
        }
        return returnToLatestVersion();
    }
    
    public String releaseParentDVAndDataset(){
        releaseParentDV();
        return releaseDataset(false);
    }

    public String releaseDataset() {
        if(!dataset.getOwner().isReleased()){
            releaseParentDV();
        }       
        if(publishDatasetPopup()|| publishBothPopup() || !dataset.getLatestVersion().isMinorUpdate()){
            return releaseDataset(false);
        }        
        switch (releaseRadio) {
            case 1:
                return releaseDataset(true);
            case 2:
                return releaseDataset(false);
            case 3:
                return updateCurrentVersion();
            default:
                return "Invalid Choice";
        }
    }
    
    private void releaseParentDV(){
        if (session.getUser() instanceof AuthenticatedUser) {
            PublishDataverseCommand cmd = new PublishDataverseCommand(dvRequestService.getDataverseRequest(), dataset.getOwner());
            try {
                commandEngine.submit(cmd);
                JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("dataverse.publish.success"));

            } catch (CommandException ex) {
                logger.log(Level.SEVERE, "Unexpected Exception calling  publish dataverse command", ex);
                JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("dataverse.publish.failure"));

            }
        } else {
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, BundleUtil.getStringFromBundle("dataverse.notreleased")  ,BundleUtil.getStringFromBundle( "dataverse.release.authenticatedUsersOnly"));
            FacesContext.getCurrentInstance().addMessage(null, message);
        }
        
    }

    public String deaccessionVersions() {
        Command<DatasetVersion> cmd;
        try {
            if (selectedDeaccessionVersions == null) {
                for (DatasetVersion dv : this.dataset.getVersions()) {
                    if (dv.isReleased()) {
                        DatasetVersion deaccession = datasetVersionService.find(dv.getId());
                        cmd = new DeaccessionDatasetVersionCommand(dvRequestService.getDataverseRequest(), setDatasetVersionDeaccessionReasonAndURL(deaccession), true);
                        DatasetVersion datasetv = commandEngine.submit(cmd);
                    }
                }
            } else {
                for (DatasetVersion dv : selectedDeaccessionVersions) {
                    DatasetVersion deaccession = datasetVersionService.find(dv.getId());
                    cmd = new DeaccessionDatasetVersionCommand(dvRequestService.getDataverseRequest(), setDatasetVersionDeaccessionReasonAndURL(deaccession), false);
                    DatasetVersion datasetv = commandEngine.submit(cmd);
                }
            }
        } catch (CommandException ex) {
            logger.severe(ex.getMessage());
            JH.addMessage(FacesMessage.SEVERITY_FATAL, BundleUtil.getStringFromBundle("dataset.message.deaccessionFailure"));
        }
        setRenderDeaccessionPopup(false);
        JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("datasetVersion.message.deaccessionSuccess"));
        return returnToDatasetOnly();
    }

    private DatasetVersion setDatasetVersionDeaccessionReasonAndURL(DatasetVersion dvIn) {
        int deaccessionReasonCode = getDeaccessionReasonRadio();
        String deacessionReasonDetail = getDeaccessionReasonText() != null ? ( getDeaccessionReasonText()).trim() : "";
        switch (deaccessionReasonCode) {
            case 1:
                dvIn.setVersionNote(BundleUtil.getStringFromBundle("file.deaccessionDialog.reason.selectItem.identifiable") );
                break;
            case 2:
                dvIn.setVersionNote(BundleUtil.getStringFromBundle("file.deaccessionDialog.reason.selectItem.beRetracted") );
                break;
            case 3:
                dvIn.setVersionNote(BundleUtil.getStringFromBundle("file.deaccessionDialog.reason.selectItem.beTransferred") );
                break;
            case 4:
                dvIn.setVersionNote(BundleUtil.getStringFromBundle("file.deaccessionDialog.reason.selectItem.IRB"));
                break;
            case 5:
                dvIn.setVersionNote(BundleUtil.getStringFromBundle("file.deaccessionDialog.reason.selectItem.legalIssue"));
                break;
            case 6:
                dvIn.setVersionNote(BundleUtil.getStringFromBundle("file.deaccessionDialog.reason.selectItem.notValid"));
                break;
            case 7:
                break;
        }
        if (!deacessionReasonDetail.isEmpty()){
            if (!StringUtil.isEmpty(dvIn.getVersionNote())){
                dvIn.setVersionNote(dvIn.getVersionNote() + " " + deacessionReasonDetail);
            } else {
                dvIn.setVersionNote(deacessionReasonDetail);
            }
        }
        
        dvIn.setArchiveNote(getDeaccessionForwardURLFor());
        return dvIn;
    }

    private String releaseDataset(boolean minor) {
        if (session.getUser() instanceof AuthenticatedUser) {
            try {
                final PublishDatasetResult result = commandEngine.submit(
                    new PublishDatasetCommand(dataset, dvRequestService.getDataverseRequest(), minor)
                );
                dataset = result.getDataset();
                // Sucessfully executing PublishDatasetCommand does not guarantee that the dataset 
                // has been published. If a publishing workflow is configured, this may have sent the 
                // dataset into a workflow limbo, potentially waiting for a third party system to complete 
                // the process. So it may be premature to show the "success" message at this point. 
                
                if ( result.isCompleted() ) {
                    JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("dataset.message.publishSuccess"));
                } else {
                    JH.addMessage(FacesMessage.SEVERITY_WARN, BundleUtil.getStringFromBundle("dataset.locked.message"), BundleUtil.getStringFromBundle("dataset.locked.message.details"));
                }
                
            } catch (CommandException ex) {
                Dataset testDs = datasetService.find(dataset.getId());
                if (testDs != null && !testDs.isLockedFor(DatasetLock.Reason.FileValidationFailed)) {
                    // If the dataset could not be published because it has failed 
                    // physical file validation, the messaging will be handled via
                    // the lock info system. 
                    JsfHelper.addErrorMessage(ex.getLocalizedMessage());
                }
                logger.severe(ex.getMessage());
            }
            
        } else {
            JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("dataset.message.only.authenticatedUsers"));
        }
        return returnToDraftVersion();
    }

    @Deprecated
    public String registerDataset() {
        try {
            UpdateDatasetVersionCommand cmd = new UpdateDatasetVersionCommand(dataset, dvRequestService.getDataverseRequest());
            cmd.setValidateLenient(true); 
            dataset = commandEngine.submit(cmd);
        } catch (CommandException ex) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN,BundleUtil.getStringFromBundle( "dataset.registration.failed"), " - " + ex.toString()));
            logger.severe(ex.getMessage());
        }
        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, BundleUtil.getStringFromBundle("dataset.registered"), BundleUtil.getStringFromBundle("dataset.registered.msg"));
        FacesContext.getCurrentInstance().addMessage(null, message);
        return returnToDatasetOnly();
    }
    
    public String updateCurrentVersion() {
        /*
         * Note: The code here mirrors that in the
         * edu.harvard.iq.dataverse.api.Datasets:publishDataset method (case
         * "updatecurrent"). Any changes to the core logic (i.e. beyond updating the
         * messaging about results) should be applied to the code there as well.
         */
        String errorMsg = null;
        String successMsg = BundleUtil.getStringFromBundle("datasetversion.update.success");
        try {
            CuratePublishedDatasetVersionCommand cmd = new CuratePublishedDatasetVersionCommand(dataset, dvRequestService.getDataverseRequest());
            dataset = commandEngine.submit(cmd);
            // If configured, and currently published version is archived, try to update archive copy as well
            DatasetVersion updateVersion = dataset.getLatestVersion();
            if (updateVersion.getArchivalCopyLocation() != null) {
                String className = settingsService.get(SettingsServiceBean.Key.ArchiverClassName.toString());
                AbstractSubmitToArchiveCommand archiveCommand = ArchiverUtil.createSubmitToArchiveCommand(className, dvRequestService.getDataverseRequest(), updateVersion);
                if (archiveCommand != null) {
                    // Delete the record of any existing copy since it is now out of date/incorrect
                    updateVersion.setArchivalCopyLocation(null);
                    /*
                     * Then try to generate and submit an archival copy. Note that running this
                     * command within the CuratePublishedDatasetVersionCommand was causing an error:
                     * "The attribute [id] of class
                     * [edu.harvard.iq.dataverse.DatasetFieldCompoundValue] is mapped to a primary
                     * key column in the database. Updates are not allowed." To avoid that, and to
                     * simplify reporting back to the GUI whether this optional step succeeded, I've
                     * pulled this out as a separate submit().
                     */
                    try {
                        updateVersion = commandEngine.submit(archiveCommand);
                        if (updateVersion.getArchivalCopyLocation() != null) {
                            successMsg = BundleUtil.getStringFromBundle("datasetversion.update.archive.success");
                        } else {
                            errorMsg = BundleUtil.getStringFromBundle("datasetversion.update.archive.failure");
                        }
                    } catch (CommandException ex) {
                        errorMsg = BundleUtil.getStringFromBundle("datasetversion.update.archive.failure") + " - " + ex.toString();
                        logger.severe(ex.getMessage());
                    }
                }
            }
        } catch (CommandException ex) {
            errorMsg = BundleUtil.getStringFromBundle("datasetversion.update.failure") + " - " + ex.toString();
            logger.severe(ex.getMessage());
        }
        if (errorMsg != null) {
            JsfHelper.addErrorMessage(errorMsg);
        } else {
            JsfHelper.addSuccessMessage(successMsg);
        }
        return returnToDatasetOnly();
    }


    public void refresh(ActionEvent e) {
        refresh();
    }
        
    public String refresh() {
        logger.fine("refreshing");

        //dataset = datasetService.find(dataset.getId());
        dataset = null;

        logger.fine("refreshing working version");

        DatasetVersionServiceBean.RetrieveDatasetVersionResponse retrieveDatasetVersionResponse = null;

        if (persistentId != null) {
            //retrieveDatasetVersionResponse = datasetVersionService.retrieveDatasetVersionByPersistentId(persistentId, version);
            dataset = datasetService.findByGlobalId(persistentId);
            retrieveDatasetVersionResponse = datasetVersionService.selectRequestedVersion(dataset.getVersions(), version);
        } else if (versionId != null) {
            retrieveDatasetVersionResponse = datasetVersionService.retrieveDatasetVersionByVersionId(versionId);
        } else if (dataset.getId() != null) {
            //retrieveDatasetVersionResponse = datasetVersionService.retrieveDatasetVersionById(dataset.getId(), version);
            dataset = datasetService.find(dataset.getId());
            retrieveDatasetVersionResponse = datasetVersionService.selectRequestedVersion(dataset.getVersions(), version);
        }

        if (retrieveDatasetVersionResponse == null) {
            // TODO: 
            // should probably redirect to the 404 page, if we can't find 
            // this version anymore. 
            // -- L.A. 4.2.3 
            return "";
        }
        this.workingVersion = retrieveDatasetVersionResponse.getDatasetVersion();

        if (this.workingVersion == null) {
            // TODO: 
            // same as the above

            return "";
        }

        if (dataset == null) {
            // this would be the case if we were retrieving the version by 
            // the versionId, above.
            this.dataset = this.workingVersion.getDataset();
        }

        if (readOnly) {
            datafileService.findFileMetadataOptimizedExperimental(dataset);
        }
        
        fileMetadatasSearch = selectFileMetadatasForDisplay();

        displayCitation = dataset.getCitation(true, workingVersion);
        stateChanged = false;

        if (lockedDueToIngestVar != null && lockedDueToIngestVar) {
            //we need to add a redirect here to disply the explore buttons as needed
            //as well as the ingest success message
            //SEK 12/20/2019 - since we are ingesting a file we know that there is a current draft version
            lockedDueToIngestVar = null;
            if (canViewUnpublishedDataset()) {
                return "/dataset.xhtml?persistentId=" + dataset.getGlobalIdString() + "&showIngestSuccess=true&version=DRAFT&faces-redirect=true";
            } else {
                return "/dataset.xhtml?persistentId=" + dataset.getGlobalIdString() + "&showIngestSuccess=true&faces-redirect=true";
            }
        }

        displayWorkflowComments();
        
        return "";
    }
    
    public String deleteDataset() {

        DestroyDatasetCommand cmd;
        boolean deleteCommandSuccess = false;
        Map<Long,String> deleteStorageLocations = datafileService.getPhysicalFilesToDelete(dataset); 
        
        try {
            cmd = new DestroyDatasetCommand(dataset, dvRequestService.getDataverseRequest());
            commandEngine.submit(cmd);
            deleteCommandSuccess = true;
            /* - need to figure out what to do 
             Update notification in Delete Dataset Method
             for (UserNotification und : userNotificationService.findByDvObject(dataset.getId())){
             userNotificationService.delete(und);
             } */
        } catch (CommandException ex) {
            JH.addMessage(FacesMessage.SEVERITY_FATAL, BundleUtil.getStringFromBundle("dataset.message.deleteFailure"));
            logger.severe(ex.getMessage());
        }
        
        if (deleteCommandSuccess) {
            datafileService.finalizeFileDeletes(deleteStorageLocations);
            JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("dataset.message.deleteSuccess"));
        }
        
        return "/dataverse.xhtml?alias=" + dataset.getOwner().getAlias() + "&faces-redirect=true";
    }
    
    public String editFileMetadata(){
        // If there are no files selected, return an empty string - which 
        // means, do nothing, don't redirect anywhere, stay on this page. 
        // The dialogue telling the user to select at least one file will 
        // be shown to them by an onclick javascript method attached to the 
        // filemetadata edit button on the page.
        // -- L.A. 4.2.1
        if (this.selectedFiles == null || this.selectedFiles.size() < 1) {
            return "";
        } 
        return "/editdatafiles.xhtml?selectedFileIds=" + getSelectedFilesIdsString() + "&datasetId=" + dataset.getId() +"&faces-redirect=true";
    }

    public String deleteDatasetVersion() {
        DeleteDatasetVersionCommand cmd;
        try {
            cmd = new DeleteDatasetVersionCommand(dvRequestService.getDataverseRequest(), dataset);
            commandEngine.submit(cmd);
            JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("datasetVersion.message.deleteSuccess"));
        } catch (CommandException ex) {
            JH.addMessage(FacesMessage.SEVERITY_FATAL, BundleUtil.getStringFromBundle("dataset.message.deleteFailure"));
            logger.severe(ex.getMessage());
        }

        return returnToDatasetOnly();
    }

    private List<FileMetadata> selectedFiles = new ArrayList<>();

    public List<FileMetadata> getSelectedFiles() {
        return selectedFiles;
    }

    public void setSelectedFiles(List<FileMetadata> selectedFiles) {
        this.selectedFiles = selectedFiles;
    }
    
    private Dataverse selectedDataverseForLinking;

    public Dataverse getSelectedDataverseForLinking() {
        return selectedDataverseForLinking;
    }

    public void setSelectedDataverseForLinking(Dataverse sdvfl) {
        this.selectedDataverseForLinking = sdvfl;
    }
    
    private List<FileMetadata> selectedRestrictedFiles; // = new ArrayList<>();

    public List<FileMetadata> getSelectedRestrictedFiles() {
        return selectedRestrictedFiles;
    }

    public void setSelectedRestrictedFiles(List<FileMetadata> selectedRestrictedFiles) {
        this.selectedRestrictedFiles = selectedRestrictedFiles;
    }
    
    private List<FileMetadata> selectedUnrestrictedFiles; // = new ArrayList<>();

    public List<FileMetadata> getSelectedUnrestrictedFiles() {
        return selectedUnrestrictedFiles;
    }

    public void setSelectedUnrestrictedFiles(List<FileMetadata> selectedUnrestrictedFiles) {
        this.selectedUnrestrictedFiles = selectedUnrestrictedFiles;
    }
    
    private List<FileMetadata> selectedDownloadableFiles;

    public List<FileMetadata> getSelectedDownloadableFiles() {
        return selectedDownloadableFiles;
    }

    public void setSelectedDownloadableFiles(List<FileMetadata> selectedDownloadableFiles) {
        this.selectedDownloadableFiles = selectedDownloadableFiles;
    }
    
    private List<FileMetadata> selectedNonDownloadableFiles;

    public List<FileMetadata> getSelectedNonDownloadableFiles() {
        return selectedNonDownloadableFiles;
    }

    public void setSelectedNonDownloadableFiles(List<FileMetadata> selectedNonDownloadableFiles) {
        this.selectedNonDownloadableFiles = selectedNonDownloadableFiles;
    }

    private List<FileMetadata> selectedNonDownloadallableFiles;

    public List<FileMetadata> getSelectedNonDownloadallableFiles() {
        return selectedNonDownloadallableFiles;
    }

    public void setSelectedNonDownloadallableFiles(List<FileMetadata> selectedNonDownloadallableFiles) {
        this.selectedNonDownloadallableFiles = selectedNonDownloadallableFiles;
    }

    public String getSizeOfDataset() {
        boolean original = false;
        return DatasetUtil.getDownloadSize(workingVersion, original);
    }

    public String getSizeOfDatasetOrig() {
        boolean original = true;
        return DatasetUtil.getDownloadSize(workingVersion, original);
    }

    public void validateAllFilesForDownloadArchival() {
        boolean guestbookRequired = isDownloadPopupRequired();
        boolean downloadOriginal = false;
        validateFilesForDownloadAll(guestbookRequired, downloadOriginal);
    }

    /**
     * Can result in "requested optional service" error. For non-tabular files
     * it's safer to use validateAllFilesForDownloadArchival.
     */
    public void validateAllFilesForDownloadOriginal() {
        boolean guestbookRequired = isDownloadPopupRequired();
        boolean downloadOriginal = true;
        validateFilesForDownloadAll(guestbookRequired, downloadOriginal);
    }

    public void validateFilesForDownload(boolean guestbookRequired, boolean downloadOriginal){
        setSelectedDownloadableFiles(new ArrayList<>());
        setSelectedNonDownloadableFiles(new ArrayList<>());
        
        if (this.selectedFiles.isEmpty()) {
            //RequestContext requestContext = RequestContext.getCurrentInstance();
            PrimeFaces.current().executeScript("PF('selectFilesForDownload').show()");
            return;
        }
        for (FileMetadata fmd : this.selectedFiles){
            if(this.fileDownloadHelper.canDownloadFile(fmd)){
                getSelectedDownloadableFiles().add(fmd);
            } else {
                getSelectedNonDownloadableFiles().add(fmd);
            }
        }
        
        // If some of the files were restricted and we had to drop them off the 
        // list, and NONE of the files are left on the downloadable list
        // - we show them a "you're out of luck" popup: 
        if(getSelectedDownloadableFiles().isEmpty() && !getSelectedNonDownloadableFiles().isEmpty()){
            //RequestContext requestContext = RequestContext.getCurrentInstance();
            PrimeFaces.current().executeScript("PF('downloadInvalid').show()");
            return;
        }
        
        // Note that the GuestbookResponse object may still have information from 
        // the last download action performed by the user. For example, it may 
        // still have the non-null Datafile in it, if the user has just downloaded
        // a single file; or it may still have the format set to "original" - 
        // even if that's not what they are trying to do now. 
        // So make sure to reset these values:
        guestbookResponse.setDataFile(null);
        guestbookResponse.setSelectedFileIds(getSelectedDownloadableFilesIdsString());
        if (downloadOriginal) {
            guestbookResponse.setFileFormat("original");
        } else {
            guestbookResponse.setFileFormat("");
        }
        guestbookResponse.setDownloadtype("Download");
        
        // If we have a bunch of files that we can download, AND there were no files 
        // that we had to take off the list, because of permissions - we can 
        // either send the user directly to the download API (if no guestbook/terms
        // popup is required), or send them to the download popup:
        if(!getSelectedDownloadableFiles().isEmpty() && getSelectedNonDownloadableFiles().isEmpty()){
            if (guestbookRequired){
                openDownloadPopupForMultipleFileDownload();
            } else {
                startMultipleFileDownload();
            }
            return;
        } 
        
        // ... and if some files were restricted, but some are downloadable, 
        // we are showing them this "you are somewhat in luck" popup; that will 
        // then direct them to the download, or popup, as needed:
        if(!getSelectedDownloadableFiles().isEmpty() && !getSelectedNonDownloadableFiles().isEmpty()){
            //RequestContext requestContext = RequestContext.getCurrentInstance();
            PrimeFaces.current().executeScript("PF('downloadMixed').show()");
        }       

    }

    /**
     * This method borrows heavily from validateFilesForDownload but does not
     * use the selectedFiles field.
     */
    public void validateFilesForDownloadAll(boolean guestbookRequired, boolean downloadOriginal) {
        setSelectedNonDownloadallableFiles(new ArrayList<>());
        List<FileMetadata> downloadableFiles = new ArrayList<>();
        for (FileMetadata fmd : workingVersion.getFileMetadatas()) {
            if (this.fileDownloadHelper.canDownloadFile(fmd)) {
                downloadableFiles.add(fmd);
            } else {
                getSelectedNonDownloadallableFiles().add(fmd);
            }
        }

        // If some of the files were restricted and we had to drop them off the
        // list, and NONE of the files are left on the downloadable list
        // - we show them a "you're out of luck" popup:
        if (downloadableFiles.isEmpty() && !getSelectedNonDownloadallableFiles().isEmpty()) {
            //RequestContext requestContext = RequestContext.getCurrentInstance();
            PrimeFaces.current().executeScript("PF('downloadInvalid').show()");
            return;
        }

        // Note that the GuestbookResponse object may still have information from
        // the last download action performed by the user. For example, it may
        // still have the non-null Datafile in it, if the user has just downloaded
        // a single file; or it may still have the format set to "original" -
        // even if that's not what they are trying to do now.
        // So make sure to reset these values:
        guestbookResponse.setDataFile(null);
        // Inline getSelectedDownloadableFilesIdsString() that doesn't use selectedDownloadableFiles
        String downloadIdString = "";
        for (FileMetadata fmd : downloadableFiles) {
            if (!StringUtil.isEmpty(downloadIdString)) {
                downloadIdString += ",";
            }
            downloadIdString += fmd.getDataFile().getId();
        }
        guestbookResponse.setSelectedFileIds(downloadIdString);
        if (downloadOriginal) {
            guestbookResponse.setFileFormat("original");
        } else {
            guestbookResponse.setFileFormat("");
        }
        guestbookResponse.setDownloadtype("Download");

        // If we have a bunch of files that we can download, AND there were no files
        // that we had to take off the list, because of permissions - we can
        // either send the user directly to the download API (if no guestbook/terms
        // popup is required), or send them to the download popup:
        if (!downloadableFiles.isEmpty() && getSelectedNonDownloadallableFiles().isEmpty()) {
            if (guestbookRequired) {
                openDownloadPopupForDownloadAll();
            } else {
                startMultipleFileDownload();
            }
            return;
        }

        // ... and if some files were restricted, but some are downloadable,
        // we are showing them this "you are somewhat in luck" popup; that will
        // then direct them to the download, or popup, as needed:
        if (!downloadableFiles.isEmpty() && !getSelectedNonDownloadallableFiles().isEmpty()) {
            //RequestContext requestContext = RequestContext.getCurrentInstance();
            PrimeFaces.current().executeScript("PF('downloadAllMixed').show()");
        }

    }

    private boolean selectAllFiles;

    public boolean isSelectAllFiles() {
        return selectAllFiles;
    }

    public void setSelectAllFiles(boolean selectAllFiles) {
        this.selectAllFiles = selectAllFiles;
    }

    public void toggleAllSelected(){
        //This is here so that if the user selects all on the dataset page
        // s/he will get all files on download
        this.selectAllFiles = !this.selectAllFiles;
    }
    

    // helper Method
    public String getSelectedFilesIdsString() {        
        String downloadIdString = "";
        for (FileMetadata fmd : this.selectedFiles){
            if (!StringUtil.isEmpty(downloadIdString)) {
                downloadIdString += ",";
            }
            downloadIdString += fmd.getDataFile().getId();
        }
        return downloadIdString;     
    }
    
    // helper Method
    public String getSelectedDownloadableFilesIdsString() {        
        String downloadIdString = "";
        for (FileMetadata fmd : this.selectedDownloadableFiles){
            if (!StringUtil.isEmpty(downloadIdString)) {
                downloadIdString += ",";
            }
            downloadIdString += fmd.getDataFile().getId();
        }
        return downloadIdString;     
    }
    
    
    public void updateFileCounts(){
        setSelectedUnrestrictedFiles(new ArrayList<>());
        setSelectedRestrictedFiles(new ArrayList<>());
        setTabularDataSelected(false);
        for (FileMetadata fmd : this.selectedFiles){
            if(fmd.isRestricted()){
                getSelectedRestrictedFiles().add(fmd);
            } else {
                getSelectedUnrestrictedFiles().add(fmd);
            }
            if(fmd.getDataFile().isTabularData()){
                setTabularDataSelected(true);
            }
        }
    }
    

    private List<String> getSuccessMessageArguments() {
        List<String> arguments = new ArrayList<>();
        String dataverseString = "";
        arguments.add(StringEscapeUtils.escapeHtml(dataset.getDisplayName()));
        dataverseString += " <a href=\"/dataverse/" + selectedDataverseForLinking.getAlias() + "\">" + StringEscapeUtils.escapeHtml(selectedDataverseForLinking.getDisplayName()) + "</a>";
        arguments.add(dataverseString);
        return arguments;
    }
    
        
    public void saveLinkingDataverses(ActionEvent evt) {

        if (saveLink(selectedDataverseForLinking)) {
            JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("dataset.message.linkSuccess", getSuccessMessageArguments()));
        } else {
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, BundleUtil.getStringFromBundle("dataset.notlinked"), linkingDataverseErrorMessage);
            FacesContext.getCurrentInstance().addMessage(null, message);
        }

    }
    
    private String linkingDataverseErrorMessage = "";


    public String getLinkingDataverseErrorMessage() {
        return linkingDataverseErrorMessage;
    }

    public void setLinkingDataverseErrorMessage(String linkingDataverseErrorMessage) {
        this.linkingDataverseErrorMessage = linkingDataverseErrorMessage;
    }
    
    private Boolean saveLink(Dataverse dataverse){
        boolean retVal = true;
        if (readOnly) {
            // Pass a "real", non-readonly dataset the the LinkDatasetCommand: 
            dataset = datasetService.find(dataset.getId());
        }
        LinkDatasetCommand cmd = new LinkDatasetCommand(dvRequestService.getDataverseRequest(), dataverse, dataset);
        linkingDataverse = dataverse;
        try {
            commandEngine.submit(cmd);           
        } catch (CommandException ex) {
            String msg = "There was a problem linking this dataset to yours: " + ex;
            logger.severe(msg);
            msg = BundleUtil.getStringFromBundle("dataset.notlinked.msg") + ex;
            /**
             * @todo how do we get this message to show up in the GUI?
             */
            linkingDataverseErrorMessage = msg;
            retVal = false;
        }
        return retVal;
    }

    
    public List<Dataverse> completeLinkingDataverse(String query) {
        dataset = datasetService.find(dataset.getId());
        if (session.getUser().isAuthenticated()) {
            return dataverseService.filterDataversesForLinking(query, dvRequestService.getDataverseRequest(), dataset);
        } else {
            return null;
        }
    }
    
    public List<Dataverse> completeHostDataverseMenuList(String query) {
        if (session.getUser().isAuthenticated()) {
            return dataverseService.filterDataversesForHosting(query, dvRequestService.getDataverseRequest());
        } else {
            return null;
        }
    }
    
    public String restrictFiles(boolean restricted) throws CommandException {
        List filesToRestrict = new ArrayList();
        
        if (fileMetadataForAction != null) {
            filesToRestrict.add(fileMetadataForAction);
        } else {
            filesToRestrict = this.getSelectedFiles();
        }
        
        restrictFiles(filesToRestrict, restricted);
        return save();
    }
    
    private void restrictFiles(List<FileMetadata> filesToRestrict, boolean restricted) throws CommandException {

        // todo: this seems to be have been added to get around an optmistic lock; it may be worth investigating
        // if there's a better way to handle
        if (workingVersion.isReleased()) {
            refreshSelectedFiles(filesToRestrict);
        }
        
        if (restricted) { // get values from access popup
            workingVersion.getTermsOfUseAndAccess().setTermsOfAccess(termsOfAccess);
            workingVersion.getTermsOfUseAndAccess().setFileAccessRequest(fileAccessRequest);  
        }
              

        Command<Void> cmd;
        for (FileMetadata fmd : filesToRestrict) {
            if (restricted  != fmd.isRestricted()) {
                cmd = new RestrictFileCommand(fmd.getDataFile(), dvRequestService.getDataverseRequest(), restricted);
                commandEngine.submit(cmd);
            }
        }
    }

    public int getRestrictedFileCount() {
        if (workingVersion == null){
            return 0;
        }
        int restrictedFileCount = 0;
        for (FileMetadata fmd : workingVersion.getFileMetadatas()) {
            if (fmd.isRestricted()) {
                restrictedFileCount++;
            }
        }

        return restrictedFileCount;
    }

    private List<FileMetadata> filesToBeDeleted = new ArrayList<>();

    public String deleteFiles() throws CommandException{
        List filesToDelete = new ArrayList();
        
        if (fileMetadataForAction != null) {
            filesToDelete.add(fileMetadataForAction);
        } else {
            bulkFileDeleteInProgress = true;
            filesToDelete = this.getSelectedFiles();
        }
        
        deleteFiles(filesToDelete);
        return save();
    }
        
    private void deleteFiles(List<FileMetadata> filesToDelete) {
        if (workingVersion.isReleased()) {
            refreshSelectedFiles(filesToDelete);
        }
        
        for (FileMetadata markedForDelete : filesToDelete) {
            
            if (markedForDelete.getId() != null) {
                // This FileMetadata has an id, i.e., it exists in the database. 
                // We are going to remove this filemetadata from the version: 
                dataset.getEditVersion().getFileMetadatas().remove(markedForDelete);
                // But the actual delete will be handled inside the UpdateDatasetCommand
                // (called later on). The list "filesToBeDeleted" is passed to the 
                // command as a parameter:
                filesToBeDeleted.add(markedForDelete);
            } else {
                // This FileMetadata does not have an id, meaning it has just been 
                // created, and not yet saved in the database. This in turn means this is 
                // a freshly created DRAFT version; specifically created because 
                // the user is trying to delete a file from an existing published 
                // version. This means we are not really *deleting* the file - 
                // we are going to keep it in the published version; we are simply 
                // going to save a new DRAFT version that does not contain this file. 
                // So below we are deleting the metadata from the version; we are 
                // NOT adding the file to the filesToBeDeleted list that will be 
                // passed to the UpdateDatasetCommand. -- L.A. Aug 2017
                Iterator<FileMetadata> fmit = dataset.getEditVersion().getFileMetadatas().iterator();
                while (fmit.hasNext()) {
                    FileMetadata fmd = fmit.next();
                    if (markedForDelete.getDataFile().getStorageIdentifier().equals(fmd.getDataFile().getStorageIdentifier())) {
                        // And if this is an image file that happens to be assigned 
                        // as the dataset thumbnail, let's null the assignment here:
                        
                        if (fmd.getDataFile().equals(dataset.getThumbnailFile())) {
                            dataset.setThumbnailFile(null);
                        }
                        /* It should not be possible to get here if this file 
                           is not in fact released! - so the code block below 
                           is not needed.
                        //if not published then delete identifier
                        if (!fmd.getDataFile().isReleased()){
                            try{
                                commandEngine.submit(new DeleteDataFileCommand(fmd.getDataFile(), dvRequestService.getDataverseRequest()));
                            } catch (CommandException e){
                                 //this command is here to delete the identifier of unreleased files
                                 //if it fails then a reserved identifier may still be present on the remote provider
                            }                           
                        } */
                        fmit.remove();
                        break;
                    }
                }
            }
        }

        /* 
           Do note that if we are deleting any files that have UNFs (i.e., 
           tabular files), we DO NEED TO RECALCULATE the UNF of the version!
           - but we will do this inside the UpdateDatasetCommand.
        */
    }
    
    private String enteredTermsOfAccess;

    public String getEnteredTermsOfAccess() {
        return enteredTermsOfAccess;
    }

    public void setEnteredTermsOfAccess(String enteredTermsOfAccess) {
        this.enteredTermsOfAccess = enteredTermsOfAccess;
    }
    
    private Boolean enteredFileAccessRequest;

    public Boolean getEnteredFileAccessRequest() {
        return enteredFileAccessRequest;
    }

    public void setEnteredFileAccessRequest(Boolean fileAccessRequest) {
        this.enteredFileAccessRequest = fileAccessRequest;
    }
    
    
     public String saveWithTermsOfUse() {
        workingVersion.getTermsOfUseAndAccess().setTermsOfAccess(enteredTermsOfAccess);
        workingVersion.getTermsOfUseAndAccess().setFileAccessRequest(enteredFileAccessRequest);
        return save();
    }

    public void validateDeaccessionReason(FacesContext context, UIComponent toValidate, Object value) {

        UIInput reasonRadio = (UIInput) toValidate.getAttributes().get("reasonRadio");
        Object reasonRadioValue = reasonRadio.getValue();
        Integer radioVal = new Integer(reasonRadioValue.toString());

        if (radioVal == 7 && (value == null || value.toString().isEmpty())) {
            ((UIInput) toValidate).setValid(false);
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "", BundleUtil.getStringFromBundle("file.deaccessionDialog.dialog.textForReason.error"));
            context.addMessage(toValidate.getClientId(context), message);

        } else {
            if (value == null || value.toString().length() <= DatasetVersion.VERSION_NOTE_MAX_LENGTH) {
                ((UIInput) toValidate).setValid(true);
            } else {
                ((UIInput) toValidate).setValid(false);
                Integer lenghtInt = DatasetVersion.VERSION_NOTE_MAX_LENGTH;
              String lengthString =   lenghtInt.toString();
               String userMsg = BundleUtil.getStringFromBundle("file.deaccessionDialog.dialog.limitChar.error", Arrays.asList(lengthString));
                FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "", userMsg);
                context.addMessage(toValidate.getClientId(context), message);
            }
        }
    }

    public void validateForwardURL(FacesContext context, UIComponent toValidate, Object value) {

        if ((value == null || value.toString().isEmpty())) {
            ((UIInput) toValidate).setValid(true);
            return;
        }

        String testVal = value.toString();

        if (!URLValidator.isURLValid(testVal)) {
            ((UIInput) toValidate).setValid(false);
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("file.deaccessionDialog.dialog.url.error"), BundleUtil.getStringFromBundle("file.deaccessionDialog.dialog.url.error"));
            context.addMessage(toValidate.getClientId(context), message);
            return;
        }
        
        if (value.toString().length() <= DatasetVersion.ARCHIVE_NOTE_MAX_LENGTH) {
            ((UIInput) toValidate).setValid(true);
        } else {
            ((UIInput) toValidate).setValid(false);
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("file.deaccessionDialog.dialog.url.error"), BundleUtil.getStringFromBundle("file.deaccessionDialog.dialog.url.error"));
            context.addMessage(toValidate.getClientId(context), message);

        }

    }
     
    public String save() {
        //Before dataset saved, write cached prov freeform to version
        if (systemConfig.isProvCollectionEnabled()) {
            provPopupFragmentBean.saveStageProvFreeformToLatestVersion();
        }

        // Before validating, ensure that the dataset has an owner:
        if (dataset.getOwner() == null || dataset.getOwner().getId() == null) {
            dataset.setOwner(ownerId != null ? dataverseService.find(ownerId) : null);
        }
        // Validate
        Set<ConstraintViolation> constraintViolations = workingVersion.validate();
        if (!constraintViolations.isEmpty()) {
            FacesContext.getCurrentInstance().validationFailed();
            return "";
        }
        

        
        // Use the Create or Update command to save the dataset: 
        Command<Dataset> cmd;
        Map<Long, String> deleteStorageLocations = null;
        
        try { 
            if (editMode == EditMode.CREATE) {
                if ( selectedTemplate != null ) {
                    if ( isSessionUserAuthenticated() ) {
                        cmd = new CreateNewDatasetCommand(dataset, dvRequestService.getDataverseRequest(), false, selectedTemplate); 
                    } else {
                        JH.addMessage(FacesMessage.SEVERITY_FATAL, BundleUtil.getStringFromBundle("dataset.create.authenticatedUsersOnly"));
                        return null;
                    }
                } else {
                   cmd = new CreateNewDatasetCommand(dataset, dvRequestService.getDataverseRequest());
                }
                
            } else {
                //Precheck - also checking db copy of dataset to catch edits in progress that would cause update command transaction to fail
                if (dataset.getId() != null) {
                    Dataset lockTest = datasetService.find(dataset.getId());
                    if (dataset.isLockedFor(DatasetLock.Reason.EditInProgress) || lockTest.isLockedFor(DatasetLock.Reason.EditInProgress)) {
                        logger.log(Level.INFO, "Couldn''t save dataset: {0}", "It is locked."
                                + "");
                        JH.addMessage(FacesMessage.SEVERITY_FATAL, BundleUtil.getStringFromBundle("dataset.locked.editInProgress.message"),BundleUtil.getStringFromBundle("dataset.locked.editInProgress.message.details", Arrays.asList(BrandingUtil.getSupportTeamName(null))));
                        return returnToDraftVersion();
                    }
                }
                if (!filesToBeDeleted.isEmpty()) {
                    deleteStorageLocations = datafileService.getPhysicalFilesToDelete(filesToBeDeleted);
                }
                cmd = new UpdateDatasetVersionCommand(dataset, dvRequestService.getDataverseRequest(), filesToBeDeleted, clone );
                ((UpdateDatasetVersionCommand) cmd).setValidateLenient(true);  
            }
            dataset = commandEngine.submit(cmd);
            if (editMode == EditMode.CREATE) {
                if (session.getUser() instanceof AuthenticatedUser) {
                    userNotificationService.sendNotification((AuthenticatedUser) session.getUser(), dataset.getCreateDate(), UserNotification.Type.CREATEDS, dataset.getLatestVersion().getId());
                }
            }
            logger.fine("Successfully executed SaveDatasetCommand.");
        } catch (EJBException ex) {
            StringBuilder error = new StringBuilder();
            error.append(ex).append(" ");
            error.append(ex.getMessage()).append(" ");
            Throwable cause = ex;
            while (cause.getCause()!= null) {
                cause = cause.getCause();
                error.append(cause).append(" ");
                error.append(cause.getMessage()).append(" ");
            }
            logger.log(Level.FINE, "Couldn''t save dataset: {0}", error.toString());
            populateDatasetUpdateFailureMessage();
            return returnToDraftVersion();
        } catch (CommandException ex) {
            //FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Dataset Save Failed", " - " + ex.toString()));
            logger.log(Level.SEVERE, "CommandException, when attempting to update the dataset: " + ex.getMessage(), ex);
            populateDatasetUpdateFailureMessage();
            return returnToDraftVersion();
        }
        
        // Have we just deleted some draft datafiles (successfully)? 
        // finalize the physical file deletes:
        // (DataFileService will double-check that the datafiles no 
        // longer exist in the database, before attempting to delete 
        // the physical files)
        
        if (deleteStorageLocations != null) {
            datafileService.finalizeFileDeletes(deleteStorageLocations);
        }
        
        if (editMode != null) {
            if (editMode.equals(EditMode.CREATE)) {
                // We allow users to upload files on Create: 
                int nNewFiles = newFiles.size();
                logger.fine("NEW FILES: "+nNewFiles);
                
                if (nNewFiles > 0) {
                    // Save the NEW files permanently and add the to the dataset: 
                    
                    // But first, fully refresh the newly created dataset (with a 
                    // datasetService.find().
                    // We have reasons to believe that the CreateDatasetCommand 
                    // returns the dataset that doesn't have all the  
                    // RoleAssignments properly linked to it - even though they
                    // have been created in the dataset. 
                    dataset = datasetService.find(dataset.getId());
                    
                    List<DataFile> filesAdded = ingestService.saveAndAddFilesToDataset(dataset.getEditVersion(), newFiles, null);
                    newFiles.clear();
                    
                    // and another update command: 
                    boolean addFilesSuccess = false;
                    cmd = new UpdateDatasetVersionCommand(dataset, dvRequestService.getDataverseRequest());
                    try {
                        dataset = commandEngine.submit(cmd);
                        addFilesSuccess = true; 
                    } catch (Exception ex) {
                        addFilesSuccess = false;
                    }
                    if (addFilesSuccess && dataset.getFiles().size() > 0) {
                        if (nNewFiles == dataset.getFiles().size()) {
                            JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("dataset.message.createSuccess").concat(" ").concat(datasetService.getReminderString(dataset, canPublishDataset())));
                        } else {
                            String partialSuccessMessage = BundleUtil.getStringFromBundle("dataset.message.createSuccess.partialSuccessSavingFiles");
                            partialSuccessMessage = partialSuccessMessage.replace("{0}", "" + dataset.getFiles().size() + "");
                            partialSuccessMessage = partialSuccessMessage.replace("{1}", "" + nNewFiles + "");
                            JsfHelper.addWarningMessage(partialSuccessMessage);
                        }
                    } else {
                        JsfHelper.addWarningMessage(BundleUtil.getStringFromBundle("dataset.message.createSuccess.failedToSaveFiles"));
                    }
                } else {                    
                    JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("dataset.message.createSuccess").concat(" ").concat(datasetService.getReminderString(dataset, canPublishDataset())));
                }
            }
            if (editMode.equals(EditMode.METADATA)) {
                JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("dataset.message.metadataSuccess").concat(" ").concat(datasetService.getReminderString(dataset, canPublishDataset())));
            }
            if (editMode.equals(EditMode.LICENSE)) {
                JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("dataset.message.termsSuccess").concat(" ").concat(datasetService.getReminderString(dataset, canPublishDataset())));
            }
            if (editMode.equals(EditMode.FILE)) {
                JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("dataset.message.filesSuccess").concat(" ").concat(datasetService.getReminderString(dataset, canPublishDataset())));
            }

        } else {
            // must have been a bulk file update or delete:
            if (bulkFileDeleteInProgress) {
                JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("dataset.message.bulkFileDeleteSuccess").concat(" ").concat(datasetService.getReminderString(dataset, canPublishDataset())));
            } else {
                JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("dataset.message.bulkFileUpdateSuccess").concat(" ").concat(datasetService.getReminderString(dataset, canPublishDataset())));
            }
        }

        editMode = null;
        bulkFileDeleteInProgress = false;


        
        // Call Ingest Service one more time, to 
        // queue the data ingest jobs for asynchronous execution: 
        ingestService.startIngestJobsForDataset(dataset, (AuthenticatedUser) session.getUser());

        //After dataset saved, then persist prov json data
        if(systemConfig.isProvCollectionEnabled()) {
            try {
                provPopupFragmentBean.saveStagedProvJson(false, dataset.getLatestVersion().getFileMetadatas());
            } catch (AbstractApiBean.WrappedResponse ex) {
                JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("file.metadataTab.provenance.error"));
                Logger.getLogger(DatasetPage.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        logger.fine("Redirecting to the Dataset page.");
        
        return returnToDraftVersion();
    }
    
    private void populateDatasetUpdateFailureMessage(){
        if (editMode == null) {
            // that must have been a bulk file update or delete:
            if (bulkFileDeleteInProgress) {
                JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("dataset.message.bulkFileDeleteFailure"));

            } else {
                JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("dataset.message.filesFailure"));
            }
        } else {

            if (editMode.equals(EditMode.CREATE)) {
                JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("dataset.message.createFailure"));
            }
            if (editMode.equals(EditMode.METADATA)) {
                JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("dataset.message.metadataFailure"));
            }
            if (editMode.equals(EditMode.LICENSE)) {
                JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("dataset.message.termsFailure"));
            }
            if (editMode.equals(EditMode.FILE)) {
                JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("dataset.message.filesFailure"));
            }
        }
        
        bulkFileDeleteInProgress = false;
    }
    
    private String returnToLatestVersion(){
         dataset = datasetService.find(dataset.getId());
         workingVersion = dataset.getLatestVersion();
         if (workingVersion.isDeaccessioned() && dataset.getReleasedVersion() != null) {
         workingVersion = dataset.getReleasedVersion();
         }
         setVersionTabList(resetVersionTabList()); 
         setReleasedVersionTabList(resetReleasedVersionTabList());
         newFiles.clear();
         editMode = null;         
         return "/dataset.xhtml?persistentId=" + dataset.getGlobalIdString() + "&version="+ workingVersion.getFriendlyVersionNumber() +  "&faces-redirect=true";       
    }
    
    private String returnToDatasetOnly(){
         dataset = datasetService.find(dataset.getId());
         editMode = null;         
         return "/dataset.xhtml?persistentId=" + dataset.getGlobalId().asString() +  "&faces-redirect=true";    
    }
    
    private String returnToDraftVersion(){      
         return "/dataset.xhtml?persistentId=" + dataset.getGlobalIdString() + "&version=DRAFT" + "&faces-redirect=true";    
    }

    public String cancel() {
        return  returnToLatestVersion();
    }
    
    public void cancelCreate() {
    	//Stop any uploads in progress (so that uploadedFiles doesn't change)
    	uploadInProgress.setValue(false);

    	logger.fine("Cancelling: " + newFiles.size() + " : " + uploadedFiles.size());

    	//Files that have been finished and are now in the lower list on the page
    	for (DataFile newFile : newFiles.toArray(new DataFile[0])) {
    		FileUtil.deleteTempFile(newFile, dataset, ingestService);
    	}
    	logger.fine("Deleted newFiles");

    	//Files in the upload process but not yet finished
    	//ToDo - if files are added to uploadFiles after we access it, those files are not being deleted. With uploadInProgress being set false above, this should be a fairly rare race condition.
    	for (DataFile newFile : uploadedFiles.toArray(new DataFile[0])) {
    		FileUtil.deleteTempFile(newFile, dataset, ingestService);
    	}
    	logger.fine("Deleted uploadedFiles");

    	try {
    		String alias = dataset.getOwner().getAlias();
    		logger.info("alias: " + alias);
    		FacesContext.getCurrentInstance().getExternalContext().redirect("/dataverse.xhtml?alias=" + alias);
    	} catch (IOException ex) {
    		logger.info("Failed to issue a redirect to file download url.");
    	}
    }

    private HttpClient getClient() {
        // TODO: 
        // cache the http client? -- L.A. 4.0 alpha
        return new HttpClient();
    }

    public void refreshLock() {
        //RequestContext requestContext = RequestContext.getCurrentInstance();
        logger.fine("checking lock");
        if (isStillLocked()) {
            logger.fine("(still locked)");
        } else {
            // OK, the dataset is no longer locked. 
            // let's tell the page to refresh:
            logger.fine("no longer locked!");
            stateChanged = true;
            lockedFromEditsVar = null;
            lockedFromDownloadVar = null;
            //requestContext.execute("refreshPage();");
        }
    }
    
    public void refreshIngestLock() {
        //RequestContext requestContext = RequestContext.getCurrentInstance();
        logger.fine("checking ingest lock");
        if (isStillLockedForIngest()) {
            logger.fine("(still locked)");
        } else {
            // OK, the dataset is no longer locked. 
            // let's tell the page to refresh:
            logger.fine("no longer locked!");
            stateChanged = true;
            lockedFromEditsVar = null;
            lockedFromDownloadVar = null;
            //requestContext.execute("refreshPage();");
        }
    }
        
    public void refreshAllLocks() {
        //RequestContext requestContext = RequestContext.getCurrentInstance();
        logger.fine("checking all locks");
        if (isStillLockedForAnyReason()) {
            logger.fine("(still locked)");
        } else {
            // OK, the dataset is no longer locked. 
            // let's tell the page to refresh:
            logger.fine("no longer locked!");
            stateChanged = true;
            lockedFromEditsVar = null;
            lockedFromDownloadVar = null;
            //requestContext.execute("refreshPage();");
        }
    }

    /* 

    public boolean isLockedInProgress() {
        if (dataset != null) {
            logger.log(Level.FINE, "checking lock status of dataset {0}", dataset.getId());
            if (dataset.isLocked()) {
                return true;
            }
        }
        return false;
    }*/
    
    public boolean isDatasetLockedInWorkflow() {
        return (dataset != null) 
                ? dataset.isLockedFor(DatasetLock.Reason.Workflow) 
                : false;
    }
    
    public boolean isStillLocked() {
        
        if (dataset != null && dataset.getId() != null) {
            logger.log(Level.FINE, "checking lock status of dataset {0}", dataset.getId());
            if(dataset.getLocks().size() == 1 && dataset.getLockFor(DatasetLock.Reason.InReview) != null){
                return false;
            }
            if (datasetService.checkDatasetLock(dataset.getId())) {
                return true;
            }
        }
        return false;
    }
    
    
    public boolean isStillLockedForIngest() {
        if (dataset.getId() != null) {
            Dataset testDataset = datasetService.find(dataset.getId());
            if (testDataset != null && testDataset.getId() != null) {
                logger.log(Level.FINE, "checking lock status of dataset {0}", dataset.getId());

                if (testDataset.getLockFor(DatasetLock.Reason.Ingest) != null) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public boolean isStillLockedForAnyReason() {
        if (dataset.getId() != null) {
            Dataset testDataset = datasetService.find(dataset.getId());
            if (testDataset != null && testDataset.getId() != null) {
                if (testDataset.getLocks().size() > 0) {
                    // Refresh the info messages, in case the dataset has been 
                    // re-locked with a different lock type:
                    displayLockInfo(testDataset);
                    return true;
                }
            }
        }
        return false;
    }
    
    public boolean isLocked() {
        if (stateChanged) {
            return false; 
        }
        
        if (dataset != null) {
            if (dataset.isLocked()) {
                return true;
            }
        }
        return false;
    }
    
    public boolean isLockedForIngest() {
        if (dataset.getId() != null) {
            Dataset testDataset = datasetService.find(dataset.getId());
            if (stateChanged) {
                return false;
            }

            if (testDataset != null) {
                if (testDataset.getLockFor(DatasetLock.Reason.Ingest) != null) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public boolean isLockedForAnyReason() {
        if (dataset.getId() != null) {
            Dataset testDataset = datasetService.find(dataset.getId());
            if (stateChanged) {
                return false;
            }

            if (testDataset != null) {
                if (testDataset.getLocks().size() > 0) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public void processPublishButton() {
        if (dataset.isReleased()) {
            PrimeFaces.current().executeScript("PF('publishDataset').show()");
        }

        if (!dataset.isReleased()) {
            if (dataset.getOwner().isReleased()) {
                PrimeFaces.current().executeScript("PF('publishDataset').show()");
                return;
            }
            if (!dataset.getOwner().isReleased()) {
                if (canPublishDataverse()) {
                    if (dataset.getOwner().getOwner() == null
                            || (dataset.getOwner().getOwner() != null && dataset.getOwner().getOwner().isReleased())) {
                        PrimeFaces.current().executeScript("PF('publishDataset').show()");
                        return;
                    }
                    if ((dataset.getOwner().getOwner() != null && !dataset.getOwner().getOwner().isReleased())) {
                        PrimeFaces.current().executeScript("PF('maynotPublishParent').show()");
                    }

                } else {
                    PrimeFaces.current().executeScript("PF('mayNotRelease').show()");
                }
            }
        }
    }

    public boolean releaseDraftPopup(){
        return dataset.isReleased();
    }
    
    public boolean publishDatasetPopup(){
        if (!dataset.isReleased()) {
            return dataset.getOwner().isReleased();
        }
       return false; 
    }
    
    public boolean publishBothPopup() {
        if (!dataset.getOwner().isReleased()) {
            if (canPublishDataverse()) {
                if (dataset.getOwner().getOwner() == null
                        || (dataset.getOwner().getOwner() != null && dataset.getOwner().getOwner().isReleased())) {
                    return true;
                }
            }
        }
        return false;
    } 
    
    public String getPublishButtonLabel(){
        if(publishDatasetPopup() || releaseDraftPopup()){
            BundleUtil.getStringFromBundle("continue");  
        }
        if(publishBothPopup()){
            BundleUtil.getStringFromBundle("dataset.mayNotBePublished.both.button");
        }        
        return "";
    }
    
    private Boolean lockedFromEditsVar;
    private Boolean lockedFromDownloadVar;    
    private boolean lockedDueToDcmUpload;
    private Boolean lockedDueToIngestVar;
    private Boolean lockedFromPublishingVar;

    /**
     * Authors are not allowed to publish but curators are allowed - when Dataset is inReview
     * For all other locks edit should be locked for all editors.
     */
    public boolean isLockedFromPublishing() {
        if (null == lockedFromPublishingVar || stateChanged) {
            try {
                permissionService.checkPublishDatasetLock(dataset, dvRequestService.getDataverseRequest(), new PublishDatasetCommand(dataset, dvRequestService.getDataverseRequest(), true));
                lockedFromPublishingVar = false;
            } catch (IllegalCommandException ex) {
                lockedFromPublishingVar = true;
            }
        }
        return lockedFromPublishingVar;
    }
    /**
     * Authors are not allowed to edit but curators are allowed - when Dataset is inReview
     * For all other locks edit should be locked for all editors.
     */
    public boolean isLockedFromEdits() {
        if (null == lockedFromEditsVar || stateChanged) {
            try {
                permissionService.checkEditDatasetLock(dataset, dvRequestService.getDataverseRequest(), new UpdateDatasetVersionCommand(dataset, dvRequestService.getDataverseRequest()));
                lockedFromEditsVar = false;
            } catch (IllegalCommandException ex) {
                lockedFromEditsVar = true;
            }
        }
        return lockedFromEditsVar;

    }
    
    /**
    Need to save ingest lock state to display success later.
     */
    public boolean isLockedDueToIngest() {
        if(null == lockedDueToIngestVar || stateChanged) {
               lockedDueToIngestVar = isLockedForIngest();
        }
        return lockedFromEditsVar;
    }
    
    
    // TODO: investigate why this method was needed in the first place?
    // It appears that it was written under the assumption that downloads 
    // should not be allowed when a dataset is locked... (why?)
    // There are calls to the method throghout the file-download-buttons fragment; 
    // except the way it's done there, it's actually disregarded (??) - so the 
    // download buttons ARE always enabled. The only place where this method is 
    // honored is on the batch (mutliple file) download buttons in filesFragment.xhtml. 
    // As I'm working on #4000, I've been asked to re-enable the batch download 
    // buttons there as well, even when the dataset is locked. I'm doing that - but 
    // I feel we should probably figure out why we went to the trouble of creating 
    // this code in the first place... is there some reason we are forgetting now, 
    // why we do actually want to disable downloads on locked datasets??? 
    // -- L.A. Aug. 2018
    public boolean isLockedFromDownload(){
        if(null == lockedFromDownloadVar || stateChanged) {
            try {
                permissionService.checkDownloadFileLock(dataset, dvRequestService.getDataverseRequest(), new CreateNewDatasetCommand(dataset, dvRequestService.getDataverseRequest()));
                lockedFromDownloadVar = false;
            } catch (IllegalCommandException ex) {
                lockedFromDownloadVar = true;
                return true;
            }
        }
        return lockedFromDownloadVar;
    }

    public boolean isLockedDueToDcmUpload() {
        return lockedDueToDcmUpload;
    }

    public void setLocked(boolean locked) {
        // empty method, so that we can use DatasetPage.locked in a hidden 
        // input on the page. 
    }
    
    public void setLockedForIngest(boolean locked) {
        // empty method, so that we can use DatasetPage.locked in a hidden 
        // input on the page. 
    }
    
    public void setLockedForAnyReason(boolean locked) {
        // empty method, so that we can use DatasetPage.locked in a hidden 
        // input on the page. 
    }
    
    public boolean isStateChanged() {
        return stateChanged;
    }
    
    public void setStateChanged(boolean stateChanged) {
        // empty method, so that we can use DatasetPage.stateChanged in a hidden 
        // input on the page. 
    }

    public DatasetVersionUI getDatasetVersionUI() {
        return datasetVersionUI;
    }

    
    public List<DatasetVersion> getVersionTabList() {
        return versionTabList;
    }
    
    public List<DatasetVersion> getVersionTabListForPostLoad(){
        return this.versionTabListForPostLoad;
    }

    public void setVersionTabListForPostLoad(List<DatasetVersion> versionTabListForPostLoad) {

        this.versionTabListForPostLoad = versionTabListForPostLoad;
    }
    
    public Integer getCompareVersionsCount() {
        Integer retVal = 0;
        for (DatasetVersion dvTest : dataset.getVersions()) {
            if (!dvTest.isDeaccessioned()) {
                retVal++;
            }
        }
        return retVal;
    }
    
    /**
     * To improve performance, Version Differences
     * are retrieved/calculated after the page load
     * 
     * See: dataset-versions.xhtml, remoteCommand 'postLoadVersionTablList'
    */
    public void postLoadSetVersionTabList(){

        if (this.getVersionTabList().isEmpty() && workingVersion.isDeaccessioned()){
            setVersionTabList(resetVersionTabList());
        }
        
        this.setVersionTabListForPostLoad(this.getVersionTabList());
        

        //this.versionTabList = this.resetVersionTabList();        
    }

    /**
     * 
     * 
     * @param versionTabList 
     */
    public void setVersionTabList(List<DatasetVersion> versionTabList) {
        
        this.versionTabList = versionTabList;
    }

    private List<DatasetVersion> releasedVersionTabList = new ArrayList<>();

    public List<DatasetVersion> getReleasedVersionTabList() {
        return releasedVersionTabList;
    }

    public void setReleasedVersionTabList(List<DatasetVersion> releasedVersionTabList) {
        this.releasedVersionTabList = releasedVersionTabList;
    }

    private List<DatasetVersion> selectedVersions;

    public List<DatasetVersion> getSelectedVersions() {
        return selectedVersions;
    }

    public void setSelectedVersions(List<DatasetVersion> selectedVersions) {
        this.selectedVersions = selectedVersions;
    }

    private List<DatasetVersion> selectedDeaccessionVersions;

    public List<DatasetVersion> getSelectedDeaccessionVersions() {
        return selectedDeaccessionVersions;
    }

    public void setSelectedDeaccessionVersions(List<DatasetVersion> selectedDeaccessionVersions) {
        this.selectedDeaccessionVersions = selectedDeaccessionVersions;
    }

    public DatasetVersionDifference getDatasetVersionDifference() {
        return datasetVersionDifference;
    }

    public void setDatasetVersionDifference(DatasetVersionDifference datasetVersionDifference) {
        this.datasetVersionDifference = datasetVersionDifference;
    }
        
    public void startMultipleFileDownload (){
        
        boolean doNotSaveGuestbookResponse = workingVersion.isDraft();
        // There's a chance that this is not really a batch download - i.e., 
        // there may only be one file on the downloadable list. But the fileDownloadService 
        // method below will check for that, and will redirect to the single download, if
        // that's the case. -- L.A.
        fileDownloadService.writeGuestbookAndStartBatchDownload(guestbookResponse, doNotSaveGuestbookResponse);
    }
 
    private String downloadType = "";

    public String getDownloadType() {
        return downloadType;
    }

    public void setDownloadType(String downloadType) {
        this.downloadType = downloadType;
    }
    
    
    public void openDownloadPopupForMultipleFileDownload() {
        if (this.selectedFiles.isEmpty()) {
            //RequestContext requestContext = RequestContext.getCurrentInstance();
            PrimeFaces.current().executeScript("PF('selectFilesForDownload').show()");
            return;
        }
        
        // There's a chance that this is not really a batch download - i.e., 
        // there may only be one file on the downloadable list. But the fileDownloadService 
        // method below will check for that, and will redirect to the single download, if
        // that's the case. -- L.A.
        
        this.guestbookResponse.setDownloadtype("Download");
        //RequestContext requestContext = RequestContext.getCurrentInstance();
        PrimeFaces.current().executeScript("PF('downloadPopup').show();handleResizeDialog('downloadPopup');");
    }

    /**
     * This method borrows heavily from
     * openDownloadPopupForMultipleFileDownload. It does not use the
     * selectedFiles field.
     */
    public void openDownloadPopupForDownloadAll() {
        // This is commented out because "download all" doesn't use selectedFiles.
//        if (this.selectedFiles.isEmpty()) {
//            //RequestContext requestContext = RequestContext.getCurrentInstance();
//            PrimeFaces.current().executeScript("PF('selectFilesForDownload').show()");
//            return;
//        }

        // There's a chance that this is not really a batch download - i.e.,
        // there may only be one file on the downloadable list. But the fileDownloadService
        // method below will check for that, and will redirect to the single download, if
        // that's the case. -- L.A.
        this.guestbookResponse.setDownloadtype("Download");
        //RequestContext requestContext = RequestContext.getCurrentInstance();
        PrimeFaces.current().executeScript("PF('downloadPopup').show();handleResizeDialog('downloadPopup');");
    }
    
    public void initGuestbookMultipleResponse(String selectedFileIds){
         initGuestbookResponse(null, "download", selectedFileIds);
    }

    public void initGuestbookResponse(FileMetadata fileMetadata, String downloadFormat, String selectedFileIds) {
        
        this.guestbookResponse = guestbookResponseService.initGuestbookResponse(fileMetadata, downloadFormat, selectedFileIds, session);
    }



    public void compareVersionDifferences() {
        //RequestContext requestContext = RequestContext.getCurrentInstance();
        if (this.selectedVersions.size() != 2) {
            PrimeFaces.current().executeScript("openCompareTwo();");
        } else {
            //order depends on order of selection - needs to be chronological order
            if (this.selectedVersions.get(0).getId().intValue() > this.selectedVersions.get(1).getId().intValue()) {
                updateVersionDifferences(this.selectedVersions.get(0), this.selectedVersions.get(1));
            } else {
                updateVersionDifferences(this.selectedVersions.get(1), this.selectedVersions.get(0));
            }
        }
    }

    public void updateVersionDifferences(DatasetVersion newVersion, DatasetVersion originalVersion) {
        if (originalVersion == null) {
            setDatasetVersionDifference(newVersion.getDefaultVersionDifference());
        } else {
            setDatasetVersionDifference(new DatasetVersionDifference(newVersion, originalVersion));
        }
    }
    
    
    

    private List<DatasetVersion> resetVersionTabList() {
        //if (true)return null;
        List<DatasetVersion> retList = new ArrayList<>();

        if (permissionService.on(dataset).has(Permission.ViewUnpublishedDataset)) {
            for (DatasetVersion version : dataset.getVersions()) {
                Collection<FileMetadata> fml = version.getFileMetadatas();
                for (FileMetadata fm : fml) {
                    fm.setVariableMetadatas(variableService.findVarMetByFileMetaId(fm.getId()));
                    fm.setVarGroups(variableService.findAllGroupsByFileMetadata(fm.getId()));
                }
                version.setContributorNames(datasetVersionService.getContributorsNames(version));
                retList.add(version);
            }

        } else {
            for (DatasetVersion version : dataset.getVersions()) {
                if (version.isReleased() || version.isDeaccessioned()) {
                    version.setContributorNames(datasetVersionService.getContributorsNames(version));
                    retList.add(version);
                }
            }
        }
        return retList;
    }


    
    private boolean existReleasedVersion;

    public boolean isExistReleasedVersion() {
        return existReleasedVersion;
    }

    public void setExistReleasedVersion(boolean existReleasedVersion) {
        this.existReleasedVersion = existReleasedVersion;
    }
    
    private boolean resetExistRealeaseVersion(){

        for (DatasetVersion version : dataset.getVersions()) {
            if (version.isReleased() || version.isArchived()) {
                return true;
            }
        }
        return false;
        
    }

    private List<DatasetVersion> resetReleasedVersionTabList() {
        List<DatasetVersion> retList = new ArrayList<>();
        for (DatasetVersion version : dataset.getVersions()) {
            if (version.isReleased() || version.isArchived()) {
                retList.add(version);
            }
        }
        return retList;
    }
    
    public boolean isDisplayPublishPopupCustomText() {
        if (dataset.isReleased()) {
            return isDatasetPublishPopupCustomTextOnAllVersions() && !getDatasetPublishCustomText().isEmpty();
        }
        return !getDatasetPublishCustomText().isEmpty();
    }

    public String getDatasetPublishCustomText(){
        String datasetPublishCustomText = settingsWrapper.getValueForKey(SettingsServiceBean.Key.DatasetPublishPopupCustomText);
        if( datasetPublishCustomText!= null && !datasetPublishCustomText.isEmpty()){
            return datasetPublishCustomText;        
        }
        return "";
    }
    
    public Boolean isDatasetPublishPopupCustomTextOnAllVersions(){
        return  settingsWrapper.isTrueForKey(SettingsServiceBean.Key.DatasetPublishPopupCustomTextOnAllVersions, false);
    }

    public String getVariableMetadataURL(Long fileid) {
        String myHostURL = getDataverseSiteUrl();
        String metaURL = myHostURL + "/api/meta/datafile/" + fileid;

        return metaURL;
    }

    public String getTabularDataFileURL(Long fileid) {
        String myHostURL = getDataverseSiteUrl();
        String dataURL = myHostURL + "/api/access/datafile/" + fileid;

        return dataURL;
    }

    public List< String[]> getExporters(){
        List<String[]> retList = new ArrayList<>();
        String myHostURL = getDataverseSiteUrl();
        for (String [] provider : ExportService.getInstance().getExportersLabels() ){
            String formatName = provider[1];
            String formatDisplayName = provider[0];
            
            Exporter exporter = null; 
            try {
                exporter = ExportService.getInstance().getExporter(formatName);
            } catch (ExportException ex) {
                exporter = null;
            }
            if (exporter != null && exporter.isAvailableToUsers()) {
                // Not all metadata exports should be presented to the web users!
                // Some are only for harvesting clients.
                
                String[] temp = new String[2];            
                temp[0] = formatDisplayName;
                temp[1] = myHostURL + "/api/datasets/export?exporter=" + formatName + "&persistentId=" + dataset.getGlobalIdString();
                retList.add(temp);
            }
        }
        return retList;  
    }
    

    private FileMetadata fileMetadataSelected = null;

    public void  setFileMetadataSelected(FileMetadata fm){
       setFileMetadataSelected(fm, null); 
    }
    
    public void setFileMetadataSelected(FileMetadata fm, String guestbook) {
        if (guestbook != null) {
            if (guestbook.equals("create")) {
                //
                /*
                FIX ME guestbook entry for subsetting
                */
                
                
                
                
               // guestbookResponseService.createSilentGuestbookEntry(fm, "Subset");
            } else {
                initGuestbookResponse(fm, "Subset", null);
            }
        }

        fileMetadataSelected = fm;
        logger.fine("set the file for the advanced options popup (" + fileMetadataSelected.getLabel() + ")");
    }

    public FileMetadata getFileMetadataSelected() {
        if (fileMetadataSelected != null) {
            logger.fine("returning file metadata for the advanced options popup (" + fileMetadataSelected.getLabel() + ")");
        } else {
            logger.fine("file metadata for the advanced options popup is null.");
        }
        return fileMetadataSelected;
    }

    public void clearFileMetadataSelected() {
        fileMetadataSelected = null;
    }
    
    public boolean isDesignatedDatasetThumbnail (FileMetadata fileMetadata) {
        if (fileMetadata != null) {
            if (fileMetadata.getDataFile() != null) {
                if (fileMetadata.getDataFile().getId() != null) {
                    if (fileMetadata.getDataFile().getOwner() != null) {
                        if (fileMetadata.getDataFile().equals(fileMetadata.getDataFile().getOwner().getThumbnailFile())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
    
    /* 
     * Items for the "Designated this image as the Dataset thumbnail: 
     */
    
    private FileMetadata fileMetadataSelectedForThumbnailPopup = null; 

    public void  setFileMetadataSelectedForThumbnailPopup(FileMetadata fm){
       fileMetadataSelectedForThumbnailPopup = fm; 
       alreadyDesignatedAsDatasetThumbnail = getUseAsDatasetThumbnail();

    }
    
    public FileMetadata getFileMetadataSelectedForThumbnailPopup() {
        return fileMetadataSelectedForThumbnailPopup;
    }
    
    public void clearFileMetadataSelectedForThumbnailPopup() {
        fileMetadataSelectedForThumbnailPopup = null;
    }
    
    private boolean alreadyDesignatedAsDatasetThumbnail = false; 
    
    public boolean getUseAsDatasetThumbnail() {

        if (fileMetadataSelectedForThumbnailPopup != null) {
            if (fileMetadataSelectedForThumbnailPopup.getDataFile() != null) {
                if (fileMetadataSelectedForThumbnailPopup.getDataFile().getId() != null) {
                    if (fileMetadataSelectedForThumbnailPopup.getDataFile().getOwner() != null) {
                        if (fileMetadataSelectedForThumbnailPopup.getDataFile().equals(fileMetadataSelectedForThumbnailPopup.getDataFile().getOwner().getThumbnailFile())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    
    
    public void setUseAsDatasetThumbnail(boolean useAsThumbnail) {
        if (fileMetadataSelectedForThumbnailPopup != null) {
            if (fileMetadataSelectedForThumbnailPopup.getDataFile() != null) {
                if (fileMetadataSelectedForThumbnailPopup.getDataFile().getId() != null) { // ?
                    if (fileMetadataSelectedForThumbnailPopup.getDataFile().getOwner() != null) {
                        if (useAsThumbnail) {
                            fileMetadataSelectedForThumbnailPopup.getDataFile().getOwner().setThumbnailFile(fileMetadataSelectedForThumbnailPopup.getDataFile());
                        } else if (getUseAsDatasetThumbnail()) {
                            fileMetadataSelectedForThumbnailPopup.getDataFile().getOwner().setThumbnailFile(null);
                        }
                    }
                }
            }
        }
    }

    public void saveAsDesignatedThumbnail() {
        // We don't need to do anything specific to save this setting, because
        // the setUseAsDatasetThumbnail() method, above, has already updated the
        // file object appropriately. 
        // However, once the "save" button is pressed, we want to show a success message, if this is 
        // a new image has been designated as such:
        if (getUseAsDatasetThumbnail() && !alreadyDesignatedAsDatasetThumbnail) {
            String successMessage = BundleUtil.getStringFromBundle("file.assignedDataverseImage.success");
            logger.fine(successMessage);
            successMessage = successMessage.replace("{0}", fileMetadataSelectedForThumbnailPopup.getLabel());
            JsfHelper.addFlashMessage(successMessage);
        }
        
        // And reset the selected fileMetadata:
        
        fileMetadataSelectedForThumbnailPopup = null;
    }
    
    /* 
     * Items for the "Tags (Categories)" popup.
     *
     */
    private FileMetadata fileMetadataSelectedForTagsPopup = null; 
    
    public void  setFileMetadataSelectedForTagsPopup(){

    }

    public void  setFileMetadataSelectedForTagsPopup(FileMetadata fm){
       fileMetadataSelectedForTagsPopup = fm; 
    }
    
    public FileMetadata getFileMetadataSelectedForTagsPopup() {
        return fileMetadataSelectedForTagsPopup;
    }
    
    public void clearFileMetadataSelectedForTagsPopup() {
        fileMetadataSelectedForTagsPopup = null;
    }
    
    public List <FileMetadata> getListFileMetadataSelectedForTagsPopup(){
        List<FileMetadata> retList = new ArrayList<>();
        for (FileMetadata fm : selectedFiles){
            retList.add(fm);
        }
        return retList;       
    }
    
    private List<String> categoriesByName;
    
    public void setCategoriesByName(List<String>  dummy){
        categoriesByName = dummy;
    }
    
    public void refreshTagsPopUp(){
        if (workingVersion.isReleased()) {
            refreshSelectedFiles(selectedFiles);
        }
        updateFileCounts();
        refreshCategoriesByName();
        refreshTabFileTagsByName();
    }
    
    private List<String> tabFileTagsByName;

    public List<String> getTabFileTagsByName() {
        return tabFileTagsByName;
    }

    public void setTabFileTagsByName(List<String> tabFileTagsByName) {
        this.tabFileTagsByName = tabFileTagsByName;
    }
    
    private void refreshCategoriesByName(){
        categoriesByName= new ArrayList<>();
        for (String category: dataset.getCategoriesByName() ){
            categoriesByName.add(category);
        }
        refreshSelectedTags();
    }
    
    
    
    
    public List<String> getCategoriesByName() {
            return categoriesByName;
    }
    
    /*
     * 1. Tabular File Tags: 
     */
    
    private List<String> tabFileTags = null;

    public List<String> getTabFileTags() {
        if (tabFileTags == null) {
            tabFileTags = DataFileTag.listTags();
        }
        return tabFileTags;
    }

    public void setTabFileTags(List<String> tabFileTags) {
        this.tabFileTags = tabFileTags;
    }
    
    private String[] selectedTabFileTags = {};

    public String[] getSelectedTabFileTags() {
        return selectedTabFileTags;
    }

    public void setSelectedTabFileTags(String[] selectedTabFileTags) {
        this.selectedTabFileTags = selectedTabFileTags;
    }

    private String[] selectedTags = {};
    
    public void handleSelection(final AjaxBehaviorEvent event) {
        if (selectedTags != null) {
            selectedTags = selectedTags.clone();
        }
    }
    
    public void handleCVVSelection(final AjaxBehaviorEvent event) {
        //Dummy method for AJAX update of items selected
    }
        
   
    private void refreshTabFileTagsByName(){
        
        tabFileTagsByName= new ArrayList<>();
        for (FileMetadata fm : selectedFiles) {
            if (fm.getDataFile().getTags() != null) {
                for (int i = 0; i < fm.getDataFile().getTags().size(); i++) {
                    if (!tabFileTagsByName.contains(fm.getDataFile().getTags().get(i).getTypeLabel())) {
                        tabFileTagsByName.add(fm.getDataFile().getTags().get(i).getTypeLabel());
                    }
                }
            }
        }
        refreshSelectedTabFileTags();
    }
    
    private void refreshSelectedTabFileTags() {
        selectedTabFileTags = null;
        selectedTabFileTags = new String[0];
        if (tabFileTagsByName.size() > 0) {
            selectedTabFileTags = new String[tabFileTagsByName.size()];
            for (int i = 0; i < tabFileTagsByName.size(); i++) {
                selectedTabFileTags[i] = tabFileTagsByName.get(i);
            }
        }
        Arrays.sort(selectedTabFileTags);
    }
    
    private boolean tabularDataSelected = false;

    public boolean isTabularDataSelected() {
        return tabularDataSelected;
    }

    public void setTabularDataSelected(boolean tabularDataSelected) {
        this.tabularDataSelected = tabularDataSelected;
    }

    public String[] getSelectedTags() {    

        return selectedTags;
    }

    public void setSelectedTags(String[] selectedTags) {
        this.selectedTags = selectedTags;
    }

    /*
     * "File Tags" (aka "File Categories"): 
    */
    
    private String newCategoryName = null;

    public String getNewCategoryName() {
        return newCategoryName;
    }

    public void setNewCategoryName(String newCategoryName) {
        this.newCategoryName = newCategoryName;
    }

    public String saveNewCategory() {
        if (newCategoryName != null && !newCategoryName.isEmpty()) {
            categoriesByName.add(newCategoryName);
        }
        //Now increase size of selectedTags and add new category
        String[] temp = new String[selectedTags.length + 1];
        System.arraycopy(selectedTags, 0, temp, 0, selectedTags.length);
        selectedTags = temp;
        selectedTags[selectedTags.length - 1] = newCategoryName;
        //Blank out added category
        newCategoryName = "";
        return "";
    }
    
    private void refreshSelectedTags() {
        selectedTags = null;
        selectedTags = new String[0];
        
        List<String> selectedCategoriesByName= new ArrayList<>();
        for (FileMetadata fm : selectedFiles) {
            if (fm.getCategories() != null) {
                for (int i = 0; i < fm.getCategories().size(); i++) {
                    if (!selectedCategoriesByName.contains(fm.getCategories().get(i).getName())) {
                    selectedCategoriesByName.add(fm.getCategories().get(i).getName());
                    }

                }

            }
        }

        if (selectedCategoriesByName.size() > 0) {
            selectedTags = new String[selectedCategoriesByName.size()];
            for (int i = 0; i < selectedCategoriesByName.size(); i++) {
                selectedTags[i] = selectedCategoriesByName.get(i);
            }
        }
        Arrays.sort(selectedTags);
    }
        
    /* This method handles saving both "tabular file tags" and 
     * "file categories" (which are also considered "tags" in 4.0)
    */
    public String saveFileTagsAndCategories() {
        // 1. New Category name:
        // With we don't need to do anything for the file categories that the user
        // selected from the pull down list; that was done directly from the 
        // page with the FileMetadata.setCategoriesByName() method. 
        // So here we only need to take care of the new, custom category
        // name, if entered: 
        if (workingVersion.isReleased()) {
            refreshSelectedFiles(selectedFiles);
        }
        for (FileMetadata fmd : workingVersion.getFileMetadatas()) {
            if (selectedFiles != null && selectedFiles.size() > 0) {
                for (FileMetadata fm : selectedFiles) {
                    if (fm.getDataFile().equals(fmd.getDataFile())) {
                        fmd.setCategories(new ArrayList<>());
                        if (newCategoryName != null) {
                            fmd.addCategoryByName(newCategoryName);
                        }
                        // 2. Tabular DataFile Tags: 
                        if (selectedTags != null) {
                            for (String selectedTag : selectedTags) {
                                fmd.addCategoryByName(selectedTag);
                            }
                        }
                        if (fmd.getDataFile().isTabularData()) {
                            fmd.getDataFile().setTags(null);
                            for (String selectedTabFileTag : selectedTabFileTags) {
                                DataFileTag tag = new DataFileTag();
                                try {
                                    tag.setTypeByLabel(selectedTabFileTag);
                                    tag.setDataFile(fmd.getDataFile());
                                    fmd.getDataFile().addTag(tag);
                                }catch (IllegalArgumentException iax) {
                                    // ignore 
                                }
                            }
                        }
                    }
                }
            }
        }
               // success message: 
                String successMessage = BundleUtil.getStringFromBundle("file.assignedTabFileTags.success");
                logger.fine(successMessage);
                successMessage = successMessage.replace("{0}", "Selected Files");
                JsfHelper.addFlashMessage(successMessage);
         selectedTags = null;

        logger.fine("New category name: " + newCategoryName);

        newCategoryName = null;
        
        if (removeUnusedTags){
            removeUnusedFileTagsFromDataset();
        }
        save();
        return  returnToDraftVersion();
    }
    
    /*
    Remove unused file tags
    When updating datafile tags see if any custom tags are not in use.
    Remove them
    
    */
    private void removeUnusedFileTagsFromDataset() {
        categoriesByName = new ArrayList<>();
        for (FileMetadata fm : workingVersion.getFileMetadatas()) {
            if (fm.getCategories() != null) {
                for (int i = 0; i < fm.getCategories().size(); i++) {
                    if (!categoriesByName.contains(fm.getCategories().get(i).getName())) {
                        categoriesByName.add(fm.getCategories().get(i).getName());
                    }
                }
            }
        }
        List<DataFileCategory> datasetFileCategoriesToRemove = new ArrayList<>();

        for (DataFileCategory test : dataset.getCategories()) {
            boolean remove = true;
            for (String catByName : categoriesByName) {
                if (catByName.equals(test.getName())) {
                    remove = false;
                    break;
                }
            }
            if (remove) {
                datasetFileCategoriesToRemove.add(test);
            }
        }

        if (!datasetFileCategoriesToRemove.isEmpty()) {
            for (DataFileCategory remove : datasetFileCategoriesToRemove) {
                dataset.getCategories().remove(remove);
            }

        }

    }

    
    /* 
     * Items for the "Advanced (Ingest) Options" popup. 
     * 
     */
    private FileMetadata fileMetadataSelectedForIngestOptionsPopup = null; 

    public void  setFileMetadataSelectedForIngestOptionsPopup(FileMetadata fm){
       fileMetadataSelectedForIngestOptionsPopup = fm; 
    }
    
    public FileMetadata getFileMetadataSelectedForIngestOptionsPopup() {
        return fileMetadataSelectedForIngestOptionsPopup;
    }
    
    public void clearFileMetadataSelectedForIngestOptionsPopup() {
        fileMetadataSelectedForIngestOptionsPopup = null;
    }
    
    private String ingestLanguageEncoding = null;

    public String getIngestLanguageEncoding() {
        if (ingestLanguageEncoding == null) {
            return "UTF8 (default)";
        }
        return ingestLanguageEncoding;
    }

    public void setIngestLanguageEncoding(String ingestLanguageEncoding) {
        this.ingestLanguageEncoding = ingestLanguageEncoding;
    }

    public void setIngestEncoding(String ingestEncoding) {
        ingestLanguageEncoding = ingestEncoding;
    }

    private String savedLabelsTempFile = null;

    public void handleLabelsFileUpload(FileUploadEvent event) {
        logger.fine("entering handleUpload method.");
        UploadedFile file = event.getFile();

        if (file != null) {

            InputStream uploadStream = null;
            try {
                uploadStream = file.getInputStream();
            } catch (IOException ioex) {
                logger.log(Level.WARNING, ioex, ()->"the file "+file.getFileName()+" failed to upload!");
                List<String> args = Arrays.asList(file.getFileName());
                String msg = BundleUtil.getStringFromBundle("dataset.file.uploadFailure.detailmsg", args);
                FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_WARN, BundleUtil.getStringFromBundle("dataset.file.uploadFailure"), msg);
                FacesContext.getCurrentInstance().addMessage(null, message);
                return;
            }

            savedLabelsTempFile = saveTempFile(uploadStream);

            logger.fine(()->file.getFileName() + " is successfully uploaded.");
            List<String> args = Arrays.asList(file.getFileName());
            FacesMessage message = new FacesMessage(BundleUtil.getStringFromBundle("dataset.file.upload", args));
            FacesContext.getCurrentInstance().addMessage(null, message);
        }

        // process file (i.e., just save it in a temp location; for now):
    }

    private String saveTempFile(InputStream input) {
        if (input == null) {
            return null;
        }
        byte[] buffer = new byte[8192];
        int bytesRead = 0;
        File labelsFile = null;
        FileOutputStream output = null;
        try {
            labelsFile = File.createTempFile("tempIngestLabels.", ".txt");
            output = new FileOutputStream(labelsFile);
            while ((bytesRead = input.read(buffer)) > -1) {
                output.write(buffer, 0, bytesRead);
            }
        } catch (IOException ioex) {
            return null;//leaving this purely in the spirit of minimizing changes.
        } finally {
            IOUtils.closeQuietly(input);
            IOUtils.closeQuietly(output);
        }
        if (labelsFile != null) {
            return labelsFile.getAbsolutePath();
        }
        return null;
    }

    public void saveAdvancedOptions() {
        
        // Language encoding for SPSS SAV (and, possibly, other tabular ingests:) 
        if (ingestLanguageEncoding != null) {
            if (fileMetadataSelectedForIngestOptionsPopup != null && fileMetadataSelectedForIngestOptionsPopup.getDataFile() != null) {
                if (fileMetadataSelectedForIngestOptionsPopup.getDataFile().getIngestRequest() == null) {
                    IngestRequest ingestRequest = new IngestRequest();
                    ingestRequest.setDataFile(fileMetadataSelectedForIngestOptionsPopup.getDataFile());
                    fileMetadataSelectedForIngestOptionsPopup.getDataFile().setIngestRequest(ingestRequest);

                }
                fileMetadataSelectedForIngestOptionsPopup.getDataFile().getIngestRequest().setTextEncoding(ingestLanguageEncoding);
            }
        }
        ingestLanguageEncoding = null;

        // Extra labels for SPSS POR (and, possibly, other tabular ingests:)
        // (we are adding this parameter to the IngestRequest now, instead of back
        // when it was uploaded. This is because we want the user to be able to 
        // hit cancel and bail out, until they actually click 'save' in the 
        // "advanced options" popup) -- L.A. 4.0 beta 11
        if (savedLabelsTempFile != null) {
            if (fileMetadataSelectedForIngestOptionsPopup != null && fileMetadataSelectedForIngestOptionsPopup.getDataFile() != null) {
                if (fileMetadataSelectedForIngestOptionsPopup.getDataFile().getIngestRequest() == null) {
                    IngestRequest ingestRequest = new IngestRequest();
                    ingestRequest.setDataFile(fileMetadataSelectedForIngestOptionsPopup.getDataFile());
                    fileMetadataSelectedForIngestOptionsPopup.getDataFile().setIngestRequest(ingestRequest);
                }
                fileMetadataSelectedForIngestOptionsPopup.getDataFile().getIngestRequest().setLabelsFile(savedLabelsTempFile);
            }
        }
        savedLabelsTempFile = null;

        fileMetadataSelectedForIngestOptionsPopup = null;
    }

    private Boolean downloadButtonAvailable = null; 
    
    public boolean isDownloadButtonAvailable(){
        
        if (downloadButtonAvailable != null) {
            return downloadButtonAvailable;
        }

        for (FileMetadata fmd : workingVersion.getFileMetadatas()) {
            if (this.fileDownloadHelper.canDownloadFile(fmd)) {
                downloadButtonAvailable = true;
                return true;
            }
        }
        downloadButtonAvailable = false;
        return false;
    }
    
    public boolean isFileAccessRequestMultiButtonRequired(){
        if (!isSessionUserAuthenticated() || !dataset.isFileAccessRequest()){
            return false;
        }
        if (workingVersion == null) {
            return false;
        }
        if (!workingVersion.getTermsOfUseAndAccess().isFileAccessRequest()){
           // return false;
        }
        for (FileMetadata fmd : workingVersion.getFileMetadatas()){
            if (!this.fileDownloadHelper.canDownloadFile(fmd)){
                return true;               
            }
        }
        return false;
    }

    public boolean isFileAccessRequestMultiButtonEnabled(){
        if (!isSessionUserAuthenticated() || !dataset.isFileAccessRequest()){
            return false;
        }
        if( this.selectedRestrictedFiles == null || this.selectedRestrictedFiles.isEmpty() ){
            return false;
        }
        for (FileMetadata fmd : this.selectedRestrictedFiles){
            if (!this.fileDownloadHelper.canDownloadFile(fmd)){
                return true;               
            }
        }
        return false;
    } 
    
    private Boolean downloadButtonAllEnabled = null;

    public boolean isDownloadAllButtonEnabled() {

        if (downloadButtonAllEnabled == null) {
            for (FileMetadata fmd : workingVersion.getFileMetadatas()) {
                if (!this.fileDownloadHelper.canDownloadFile(fmd)) {
                    downloadButtonAllEnabled = false;
                    break;
                }
            }
            downloadButtonAllEnabled = true;
        }
        return downloadButtonAllEnabled;
    }

    public boolean isDownloadSelectedButtonEnabled(){

        if( this.selectedFiles == null || this.selectedFiles.isEmpty() ){
            return false;
        }
        for (FileMetadata fmd : this.selectedFiles){
            if (this.fileDownloadHelper.canDownloadFile(fmd)){
                return true;               
            }
        }
        return false;
    } 
    
    public boolean isFileAccessRequestMultiSignUpButtonRequired(){
        if (isSessionUserAuthenticated()){
            return false;
        }
        // only show button if dataset allows an access request
        if (!dataset.isFileAccessRequest()){
            return false;
        }
        for (FileMetadata fmd : workingVersion.getFileMetadatas()){
            if (!this.fileDownloadHelper.canDownloadFile(fmd)){
                return true;               
            }
        }
        return false;
    }

    public boolean isFileAccessRequestMultiSignUpButtonEnabled(){
        if (isSessionUserAuthenticated()){
            return false;
        }
        if( this.selectedRestrictedFiles == null || this.selectedRestrictedFiles.isEmpty() ){
            return false;
        }
        // only show button if dataset allows an access request
        if (!dataset.isFileAccessRequest()){
            return false;
        }
        for (FileMetadata fmd : this.selectedRestrictedFiles){
            if (!this.fileDownloadHelper.canDownloadFile(fmd)){
                return true;               
            }
        }
        return false;
    }

    public boolean isDownloadPopupRequired() {
        return FileUtil.isDownloadPopupRequired(workingVersion);
    }
    
    public boolean isRequestAccessPopupRequired() {  
        return FileUtil.isRequestAccessPopupRequired(workingVersion);
    }
    
    public String requestAccessMultipleFiles() {

        if (selectedFiles.isEmpty()) {
            //RequestContext requestContext = RequestContext.getCurrentInstance();
            PrimeFaces.current().executeScript("PF('selectFilesForRequestAccess').show()");
            return "";
        } else {
            fileDownloadHelper.clearRequestAccessFiles();
            for (FileMetadata fmd : selectedFiles){
                 fileDownloadHelper.addMultipleFilesForRequestAccess(fmd.getDataFile());
            }
            if (isRequestAccessPopupRequired()) {
                //RequestContext requestContext = RequestContext.getCurrentInstance();                
                PrimeFaces.current().executeScript("PF('requestAccessPopup').show()");               
                return "";
            } else {
                //No popup required
                fileDownloadHelper.requestAccessIndirect();
                return "";
            }
        }
    }

    public boolean isSortButtonEnabled() {
        /**
         * @todo The "Sort" Button seems to stop responding to mouse clicks
         * after a while so it can't be shipped in 4.2 and will be deferred, to
         * be picked up in https://github.com/IQSS/dataverse/issues/2506
         */
        return false;
    }

    /*public void updateFileListing(String fileSortField, String fileSortOrder) {
        this.fileSortField = fileSortField;
        this.fileSortOrder = fileSortOrder;
        fileMetadatas = populateFileMetadatas();
    }

    private List<FileMetadata> populateFileMetadatas() {
        if (isSortButtonEnabled()) {
            List<FileMetadata> fileMetadatasToSet = new ArrayList<>();
            Long datasetVersion = workingVersion.getId();
            if (datasetVersion != null) {
                int unlimited = 0;
                int maxResults = unlimited;
                List<FileMetadata> dataFilesNew = datafileService.findFileMetadataByDatasetVersionId(datasetVersion, maxResults, fileSortField, fileSortOrder);
                fileMetadatasToSet.addAll(dataFilesNew);
            }
            return fileMetadatasToSet;
        } else {
            return new ArrayList<>();
        }
    }*/

    public String getFileSortField() {
        return fileSortField;
    }

    public void setFileSortField(String fileSortField) {
        this.fileSortField = fileSortField;
    }

    public String getFileSortOrder() {
        return fileSortOrder;
    }

    public void setFileSortOrder(String fileSortOrder) {
        this.fileSortOrder = fileSortOrder;
    }

    public List<FileMetadata> getFileMetadatas() {
        if (isSortButtonEnabled()) {
            return fileMetadatas;
        } else {
            return new ArrayList<>();
        }
    }

    public String getFileSortFieldName() {
        return FileSortFieldAndOrder.label;
    }

    public String getFileSortFieldDate() {
        return FileSortFieldAndOrder.createDate;
    }

    public String getFileSortFieldSize() {
        return FileSortFieldAndOrder.size;
    }

    public String getFileSortFieldType() {
        return FileSortFieldAndOrder.type;
    }

    public String getSortByAscending() {
        return SortBy.ASCENDING;
    }

    public String getSortByDescending() {
        return SortBy.DESCENDING;
    }

    PrivateUrl privateUrl;

    public PrivateUrl getPrivateUrl() {
        return privateUrl;
    }

    public void setPrivateUrl(PrivateUrl privateUrl) {
        this.privateUrl = privateUrl;
    }

    public void initPrivateUrlPopUp() {
        if (privateUrl != null) {
            setPrivateUrlJustCreatedToFalse();
        }
    }

    boolean privateUrlWasJustCreated;

    public boolean isPrivateUrlWasJustCreated() {
        return privateUrlWasJustCreated;
    }

    public void setPrivateUrlJustCreatedToFalse() {
        privateUrlWasJustCreated = false;
    }

    public boolean isShowLinkingPopup() {
        return showLinkingPopup;
    }

    public void setShowLinkingPopup(boolean showLinkingPopup) {
        this.showLinkingPopup = showLinkingPopup;
    }    
    
    private boolean showLinkingPopup = false;
    
    //
    
    /*
        public void setSelectedGroup(ExplicitGroup selectedGroup) {
        setShowDeletePopup(true);
        this.selectedGroup = selectedGroup;
    }
    */

    public void createPrivateUrl() {
        try {
            PrivateUrl createdPrivateUrl = commandEngine.submit(new CreatePrivateUrlCommand(dvRequestService.getDataverseRequest(), dataset));
            privateUrl = createdPrivateUrl;
            JH.addMessage(FacesMessage.SEVERITY_INFO, BundleUtil.getStringFromBundle("dataset.privateurl.header"),
                    BundleUtil.getStringFromBundle("dataset.privateurl.infoMessageAuthor", Arrays.asList(getPrivateUrlLink(privateUrl))));
            privateUrlWasJustCreated = true;
        } catch (CommandException ex) {
            String msg = BundleUtil.getStringFromBundle("dataset.privateurl.noPermToCreate", PrivateUrlUtil.getRequiredPermissions(ex));
            logger.info("Unable to create a Private URL for dataset id " + dataset.getId() + ". Message to user: " + msg + " Exception: " + ex);
            JH.addErrorMessage(msg);
        }
    }

    public void disablePrivateUrl() {
        try {
            commandEngine.submit(new DeletePrivateUrlCommand(dvRequestService.getDataverseRequest(), dataset));
            privateUrl = null;
            JH.addSuccessMessage(BundleUtil.getStringFromBundle("dataset.privateurl.disabledSuccess"));
        } catch (CommandException ex) {
            logger.info("CommandException caught calling DeletePrivateUrlCommand: " + ex);
        }
    }

    public boolean isUserCanCreatePrivateURL() {
        return dataset.getLatestVersion().isDraft();
    }

    public String getPrivateUrlLink(PrivateUrl privateUrl) {
        return privateUrl.getLink();
    }
    
    
    // todo: we should be able to remove - this is passed in the html pages to other fragments, but they could just access this service bean directly.
    public FileDownloadServiceBean getFileDownloadService() {
        return fileDownloadService;
    }

    public void setFileDownloadService(FileDownloadServiceBean fileDownloadService) {
        this.fileDownloadService = fileDownloadService;
    }
    
    
    public GuestbookResponseServiceBean getGuestbookResponseService() {
        return guestbookResponseService;
    }

    public void setGuestbookResponseService(GuestbookResponseServiceBean guestbookResponseService) {
        this.guestbookResponseService = guestbookResponseService;
    }

    /**
     * dataset title
     * @return title of workingVersion
     */
    public String getTitle() {
        assert (null != workingVersion);
        return workingVersion.getTitle();
    }

    /**
     * dataset description
     *
     * @return description of workingVersion
     */
    public String getDescription() {
        return workingVersion.getDescriptionPlainText();
    }

    /**
     * dataset authors
     *
     * @return list of author names
     */
    public List<String> getDatasetAuthors() {
        assert (workingVersion != null);
        return workingVersion.getDatasetAuthorNames();
    }

    /**
     * publisher (aka - name of root dataverse)
     *
     * @return the publisher of the version
     */
    public String getPublisher() {
        return dataverseService.getRootDataverseName();
    }
    
    public void downloadRsyncScript() {

        FacesContext ctx = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) ctx.getExternalContext().getResponse();
        response.setContentType("application/download");

        String contentDispositionString;

        contentDispositionString = "attachment;filename=" + rsyncScriptFilename;
        response.setHeader("Content-Disposition", contentDispositionString);

        try {
            ServletOutputStream out = response.getOutputStream();
            out.write(getRsyncScript().getBytes());
            out.flush();
            ctx.responseComplete();
        } catch (IOException e) {
            String error = "Problem getting bytes from rsync script: " + e;
            logger.warning(error);
            return; 
        }
        
        // If the script has been successfully downloaded, lock the dataset:
        String lockInfoMessage = "script downloaded";
        DatasetLock lock = datasetService.addDatasetLock(dataset.getId(), DatasetLock.Reason.DcmUpload, session.getUser() != null ? ((AuthenticatedUser)session.getUser()).getId() : null, lockInfoMessage);
        if (lock != null) {
            dataset.addLock(lock);
        } else {
            logger.log(Level.WARNING, "Failed to lock the dataset (dataset id={0})", dataset.getId());
        }
        
    }
    
    public void closeRsyncScriptPopup(CloseEvent event) {
        finishRsyncScriptAction();
    }
    
    public String finishRsyncScriptAction() { 
        // This method is called when the user clicks on "Close" in the "Rsync Upload" 
        // popup. If they have successfully downloaded the rsync script, the 
        // dataset should now be locked; which means we should put up the 
        // "dcm upload in progress" message - that will be shown on the page 
        // until the rsync upload is completed and the dataset is unlocked. 
        if (isLocked()) {
            JH.addMessage(FacesMessage.SEVERITY_WARN, BundleUtil.getStringFromBundle("file.rsyncUpload.inProgressMessage.summary"), BundleUtil.getStringFromBundle("file.rsyncUpload.inProgressMessage.details"));
        } 
        return "";
    }

    /**
     * this method returns the dataset fields to be shown in the dataset summary 
     * on the dataset page.
     * It returns the default summary fields( subject, description, keywords, related publications and notes)
     * if the custom summary datafields has not been set, otherwise will set the custom fields set by the sysadmins
     * 
     * @return the dataset fields to be shown in the dataset summary
     */
    public List<DatasetField> getDatasetSummaryFields() {
       customFields  = settingsWrapper.getValueForKey(SettingsServiceBean.Key.CustomDatasetSummaryFields);
       
        return DatasetUtil.getDatasetSummaryFields(workingVersion, customFields);
    }

    public boolean isShowPreviewButton(Long fileId) {
        List<ExternalTool> previewTools = getPreviewToolsForDataFile(fileId);
        return previewTools.size() > 0;
    }

    public List<ExternalTool> getPreviewToolsForDataFile(Long fileId) {
        return getCachedToolsForDataFile(fileId, ExternalTool.Type.PREVIEW);
    }
    

    public List<ExternalTool> getConfigureToolsForDataFile(Long fileId) {
        return getCachedToolsForDataFile(fileId, ExternalTool.Type.CONFIGURE);
    }

    public List<ExternalTool> getExploreToolsForDataFile(Long fileId) {
        return getCachedToolsForDataFile(fileId, ExternalTool.Type.EXPLORE);
    }

    public List<ExternalTool> getCachedToolsForDataFile(Long fileId, ExternalTool.Type type) {
        Map<Long, List<ExternalTool>> cachedToolsByFileId = new HashMap<>();
        List<ExternalTool> externalTools = new ArrayList<>();
        switch (type) {
            case EXPLORE:
                cachedToolsByFileId = exploreToolsByFileId;
                externalTools = exploreTools;
                break;
            case CONFIGURE:
                cachedToolsByFileId = configureToolsByFileId;
                externalTools = configureTools;
                break;
            case PREVIEW:
                cachedToolsByFileId = previewToolsByFileId;
                externalTools = previewTools;
                break;
            default:
                break;
        }
        List<ExternalTool> cachedTools = cachedToolsByFileId.get(fileId);
        if (cachedTools != null) { //if already queried before and added to list
            return cachedTools;
        }
        DataFile dataFile = datafileService.find(fileId);
        cachedTools = ExternalToolServiceBean.findExternalToolsByFile(externalTools, dataFile);
        cachedToolsByFileId.put(fileId, cachedTools); //add to map so we don't have to do the lifting again
        return cachedTools;
    }

    public List<ExternalTool> getDatasetExploreTools() {
        return datasetExploreTools;
    }

    Boolean thisLatestReleasedVersion = null;
    
    public boolean isThisLatestReleasedVersion() {
        if (thisLatestReleasedVersion != null) {
            return thisLatestReleasedVersion;
        }
        
        if (!workingVersion.isPublished()) {
            thisLatestReleasedVersion = false;
            return false;
        }
        
        DatasetVersion latestPublishedVersion = null;
        Command<DatasetVersion> cmd = new GetLatestPublishedDatasetVersionCommand(dvRequestService.getDataverseRequest(), dataset);
        try {
            latestPublishedVersion = commandEngine.submit(cmd);
        } catch (Exception ex) {
            // whatever...
        }
        
        thisLatestReleasedVersion = workingVersion.equals(latestPublishedVersion);
        
        return thisLatestReleasedVersion;
        
    }
    
    public String getJsonLd() {
        if (isThisLatestReleasedVersion()) {
            ExportService instance = ExportService.getInstance();
            String jsonLd = instance.getExportAsString(dataset, SchemaDotOrgExporter.NAME);
            if (jsonLd != null) {
                logger.fine("Returning cached schema.org JSON-LD.");
                return jsonLd;
            } else {
                logger.fine("No cached schema.org JSON-LD available. Going to the database.");
                String jsonLdProduced = workingVersion.getJsonLd(); 
                return  jsonLdProduced != null ? jsonLdProduced : "";
            }
        }
        return "";
    }

    public void selectAllFiles() {
        logger.fine("selectAllFiles called");
        selectedFiles = workingVersion.getFileMetadatas();
    }

    public void clearSelection() {
        logger.info("clearSelection called");
        selectedFiles = Collections.emptyList();
    }
    
    public void fileListingPaginatorListener(PageEvent event) {       
        setFilePaginatorPage(event.getPage());      
    }
    
    public void refreshPaginator() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        org.primefaces.component.datatable.DataTable dt = (org.primefaces.component.datatable.DataTable) facesContext.getViewRoot().findComponent("datasetForm:tabView:filesTable");
        setFilePaginatorPage(dt.getPage());      
        setRowsPerPage(dt.getRowsToRender());
    }  
    
    /**
     * This method can be called from *.xhtml files to allow archiving of a dataset
     * version from the user interface. It is not currently (11/18) used in the IQSS/develop
     * branch, but is used by QDR and is kept here in anticipation of including a
     * GUI option to archive (already published) versions after other dataset page
     * changes have been completed.
     * 
     * @param id - the id of the datasetversion to archive. 
     */
    public void archiveVersion(Long id) {
        if (session.getUser() instanceof AuthenticatedUser) {
            AuthenticatedUser au = ((AuthenticatedUser) session.getUser());

            DatasetVersion dv = datasetVersionService.retrieveDatasetVersionByVersionId(id).getDatasetVersion();
            String className = settingsService.getValueForKey(SettingsServiceBean.Key.ArchiverClassName);
            AbstractSubmitToArchiveCommand cmd = ArchiverUtil.createSubmitToArchiveCommand(className, dvRequestService.getDataverseRequest(), dv);
            if (cmd != null) {
                try {
                    DatasetVersion version = commandEngine.submit(cmd);
                    logger.info("Archived to " + version.getArchivalCopyLocation());
                    if (version.getArchivalCopyLocation() != null) {
                        resetVersionTabList();
                        this.setVersionTabListForPostLoad(getVersionTabList());
                        JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("datasetversion.archive.success"));
                    } else {
                        JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("datasetversion.archive.failure"));
                    }
                } catch (CommandException ex) {
                    logger.log(Level.SEVERE, "Unexpected Exception calling  submit archive command", ex);
                    JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("datasetversion.archive.failure"));
                }
            } else {
                logger.log(Level.SEVERE, "Could not find Archiver class: " + className);
                JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("datasetversion.archive.failure"));

            }
        }
    }

    private static Date getFileDateToCompare(FileMetadata fileMetadata) {
        DataFile datafile = fileMetadata.getDataFile();

        if (datafile.isReleased()) {
            return datafile.getPublicationDate();
        }

        return datafile.getCreateDate();
    }
    
    private static final Comparator<FileMetadata> compareByLabelZtoA = new Comparator<FileMetadata>() {
        @Override
        public int compare(FileMetadata o1, FileMetadata o2) {
            return o2.getLabel().toUpperCase().compareTo(o1.getLabel().toUpperCase());
        }
    };
    
    private static final Comparator<FileMetadata> compareByNewest = new Comparator<FileMetadata>() {
        @Override
        public int compare(FileMetadata o1, FileMetadata o2) {
            return getFileDateToCompare(o2).compareTo(getFileDateToCompare(o1));
        }
    };
    
    private static final Comparator<FileMetadata> compareByOldest = new Comparator<FileMetadata>() {
        @Override
        public int compare(FileMetadata o1, FileMetadata o2) {
            return getFileDateToCompare(o1).compareTo(getFileDateToCompare(o2));
        }
    };
    
    private static final Comparator<FileMetadata> compareBySize = new Comparator<FileMetadata>() {
        @Override
        public int compare(FileMetadata o1, FileMetadata o2) {
            return (new Long(o1.getDataFile().getFilesize())).compareTo(new Long(o2.getDataFile().getFilesize()));
        }
    };
    
    private static final Comparator<FileMetadata> compareByType = new Comparator<FileMetadata>() {
        @Override
        public int compare(FileMetadata o1, FileMetadata o2) {
            String type1 = StringUtil.isEmpty(o1.getDataFile().getFriendlyType()) ? "" : o1.getDataFile().getContentType();
            String type2 = StringUtil.isEmpty(o2.getDataFile().getFriendlyType()) ? "" : o2.getDataFile().getContentType();
            return type1.compareTo(type2);
        }
    };

    public void explore(ExternalTool externalTool) {
        ApiToken apiToken = null;
        User user = session.getUser();
        if (user instanceof AuthenticatedUser) {
            apiToken = authService.findApiTokenByUser((AuthenticatedUser) user);
        } else if (user instanceof PrivateUrlUser) {
            PrivateUrlUser privateUrlUser = (PrivateUrlUser) user;
            PrivateUrl privUrl = privateUrlService.getPrivateUrlFromDatasetId(privateUrlUser.getDatasetId());
            apiToken = new ApiToken();
            apiToken.setTokenString(privUrl.getToken());
        }
        ExternalToolHandler externalToolHandler = new ExternalToolHandler(externalTool, dataset, apiToken, session.getLocaleCode());
        String toolUrl = externalToolHandler.getToolUrlWithQueryParams();
        logger.fine("Exploring with " + toolUrl);
        PrimeFaces.current().executeScript("window.open('"+toolUrl + "', target='_blank');");
    }
           
    private FileMetadata fileMetadataForAction;

    public FileMetadata getFileMetadataForAction() {
        return fileMetadataForAction;
    }

    public void setFileMetadataForAction(FileMetadata fileMetadataForAction) {
        this.fileMetadataForAction = fileMetadataForAction;
    }
    
    private String termsOfAccess;
    private boolean fileAccessRequest;

    public String getTermsOfAccess() {
        return termsOfAccess;
    }

    public void setTermsOfAccess(String termsOfAccess) {
        this.termsOfAccess = termsOfAccess;
    }

    public boolean isFileAccessRequest() {
        return fileAccessRequest;
    }

    public void setFileAccessRequest(boolean fileAccessRequest) {
        this.fileAccessRequest = fileAccessRequest;
    }

    // wrapper method to see if the file has been deleted (or replaced) in the current version   
    public boolean isFileDeleted (DataFile dataFile) {
        if (dataFile.getDeleted() == null) {
            dataFile.setDeleted(datafileService.hasBeenDeleted(dataFile));
        }
        
        return dataFile.getDeleted();
    }
}
