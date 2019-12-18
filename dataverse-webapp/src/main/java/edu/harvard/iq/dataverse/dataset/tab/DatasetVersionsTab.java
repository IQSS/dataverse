package edu.harvard.iq.dataverse.dataset.tab;

import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.VersionSummaryDTO;
import edu.harvard.iq.dataverse.dataset.datasetversion.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.dataset.difference.DatasetVersionDifference;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import org.omnifaces.cdi.ViewScoped;
import org.primefaces.context.RequestContext;

import javax.ejb.EJB;
import javax.inject.Named;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@ViewScoped
@Named("datasetVersionsTab")
public class DatasetVersionsTab implements Serializable {
    
    @EJB
    private DatasetVersionServiceBean datasetVersionService;
    
    @EJB
    private PermissionServiceBean permissionService;

    private Dataset dataset;
    private List<DatasetVersion> datasetVersions = new ArrayList<>();
    
    
    private List<VersionSummaryDTO> selectedVersions;
    
    private List<VersionSummaryDTO> versionsSummary = new ArrayList<>();
    
    private DatasetVersionDifference versionsDifferenceForDialog;
    
    private long comparableVersionsCount = 0;

    
    // -------------------- GETTERS --------------------

    public List<VersionSummaryDTO> getSelectedVersions() {
        return selectedVersions;
    }

    public Dataset getDataset() {
        return dataset;
    }
    
    public DatasetVersionDifference getVersionsDifferenceForDialog() {
        return versionsDifferenceForDialog;
    }
    
    public List<VersionSummaryDTO> getVersionsSummary() {
        return versionsSummary;
    }
    
    public long getComparableVersionsCount() {
        return comparableVersionsCount;
    }
    
    // -------------------- LOGIC --------------------

    
    public void init(Dataset dataset) {
        this.dataset = dataset;
    }

    /**
     * To improve performance, Version Differences
     * are retrieved/calculated after the page load
     * <p>
     * See: dataset-versions.xhtml, remoteCommand 'postLoadVersionTablList'
     */
    public void postLoadSetVersionTabList() {
        datasetVersions = retrieveDatasetVersions();
        versionsSummary.clear();
        
        for (int i=0; i<datasetVersions.size(); ++i) {
            DatasetVersionDifference differenceFromPreviousVersion = getDefaultVersionDifference(i);
            
            versionsSummary.add(new VersionSummaryDTO(
                    datasetVersions.get(i),
                    datasetVersionService.getContributorsNames(datasetVersions.get(i)),
                    canShowLinkToDatasetVersion(datasetVersions.get(i)),
                    canBeCompared(datasetVersions.get(i)),
                    differenceFromPreviousVersion));
        }
        comparableVersionsCount = versionsSummary.stream().filter(v -> v.isCanBeCompared()).count();
    }
    
    public void updateVersionDiffForModalFromSelected() {
        RequestContext requestContext = RequestContext.getCurrentInstance();
        
        if (this.selectedVersions.size() != 2) {
            requestContext.execute("PF('needTwoVersionsToCompare').show()");
        } else {
            DatasetVersion newVersion;
            DatasetVersion oldVersion;

            //order depends on order of selection - needs to be chronological order
            if (selectedVersions.get(0).getVersion().getId() > selectedVersions.get(1).getVersion().getId()) {
                newVersion = selectedVersions.get(0).getVersion();
                oldVersion = selectedVersions.get(1).getVersion();
            } else {
                oldVersion = selectedVersions.get(0).getVersion();
                newVersion = selectedVersions.get(1).getVersion();
            }
            updateVersionDiffForDialog(new DatasetVersionDifference(newVersion, oldVersion));
        }
    }
    
    public void updateVersionDiffForDialog(DatasetVersionDifference difference) {
        versionsDifferenceForDialog = difference;
    }
    
    // -------------------- PRIVATE --------------------

    private List<DatasetVersion> retrieveDatasetVersions() {
        List<DatasetVersion> retList = new ArrayList<>();

        for (DatasetVersion version : dataset.getVersions()) {
            if (canShowVersion(version)) {
                retList.add(version);
            }
        }
        
        return retList;
    }

    private boolean canShowVersion(DatasetVersion datasetVersion) {
        if (datasetVersion.isDraft() && !permissionService.on(dataset).has(Permission.ViewUnpublishedDataset)) {
            return false;
        }
        return true;
    }

    private boolean canShowLinkToDatasetVersion(DatasetVersion datasetVersion) {
        if (datasetVersion.isDeaccessioned() || datasetVersion.isDraft()) {
            return permissionService.on(dataset).has(Permission.ViewUnpublishedDataset);
        }
        return true;
    }

    private boolean canBeCompared(DatasetVersion datasetVersion) {
        return !datasetVersion.isDeaccessioned();
    }
    
    private DatasetVersionDifference getDefaultVersionDifference(int datasetVersionIndex) {
        if (!canBeCompared(datasetVersions.get(datasetVersionIndex))) {
            return null;
        }
        
        DatasetVersion previousVersion = retrieveNextComparableVersion(datasetVersionIndex);
        if (previousVersion == null) {
            return null;
        }
        return new DatasetVersionDifference(datasetVersions.get(datasetVersionIndex), previousVersion);
    }
    
    private DatasetVersion retrieveNextComparableVersion(int datasetVersionIndex) {
        for (int i=datasetVersionIndex+1; i<datasetVersions.size(); ++i) {
            if (canBeCompared(datasetVersions.get(i))) {
                return datasetVersions.get(i);
            }
        }
        return null;
    }
    
    // -------------------- SETTERS --------------------

    public void setSelectedVersions(List<VersionSummaryDTO> selectedVersions) {
        this.selectedVersions = selectedVersions;
    }
    
}
