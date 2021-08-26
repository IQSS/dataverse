package edu.harvard.iq.dataverse.dataset.tab;

import edu.harvard.iq.dataverse.DatasetDao;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.dataset.DatasetFieldsInitializer;
import edu.harvard.iq.dataverse.export.ExportService;
import edu.harvard.iq.dataverse.export.spi.Exporter;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldUtil;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldsByType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import edu.harvard.iq.dataverse.util.SystemConfig;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.omnifaces.cdi.ViewScoped;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ViewScoped
@Named("DatasetMetadataTab")
public class DatasetMetadataTab implements Serializable {

    private PermissionsWrapper permissionsWrapper;
    private ExportService exportService;
    private SystemConfig systemConfig;
    private DatasetFieldsInitializer datasetFieldsInitializer;
    private DatasetDao datasetDao;

    private Dataset dataset;
    private boolean isDatasetLocked;
    private Map<MetadataBlock, List<DatasetFieldsByType>> metadataBlocks;

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated /*JEE requirement */
    DatasetMetadataTab() {
    }

    @Inject
    public DatasetMetadataTab(PermissionsWrapper permissionsWrapper,
                              ExportService exportService,
                              SystemConfig systemConfig,
                              DatasetFieldsInitializer datasetVersionUI,
                              DatasetDao datasetDao) {
        this.permissionsWrapper = permissionsWrapper;
        this.exportService = exportService;
        this.systemConfig = systemConfig;
        this.datasetFieldsInitializer = datasetVersionUI;
        this.datasetDao = datasetDao;
    }

    // -------------------- GETTERS --------------------

    public Dataset getDataset() {
        return dataset;
    }

    public boolean isDatasetLocked() {
        return isDatasetLocked;
    }

    /**
     * Metadata blocks meant for view.
     */
    public Map<MetadataBlock, List<DatasetFieldsByType>> getMetadataBlocks() {
        return metadataBlocks;
    }

    // -------------------- LOGIC --------------------

    public void init(DatasetVersion datasetVersion,
                     boolean isDatasetLocked) {
        this.dataset = datasetVersion.getDataset();
        this.isDatasetLocked = isDatasetLocked;
        
        List<DatasetField> datasetFields = datasetFieldsInitializer.prepareDatasetFieldsForView(datasetVersion.getDatasetFields());
        this.metadataBlocks = DatasetFieldUtil.groupByBlockAndType(datasetFields);
    }

    public boolean showEditMetadataButton() {
        return permissionsWrapper.canCurrentUserUpdateDataset(dataset) && !dataset.isDeaccessioned();
    }
    
    public boolean showExportButton() {
        return dataset.containsReleasedVersion();
    }

    
    /**
     * Extracts exporters display name and redirect url.
     */
    public List<Tuple2<String, String>> getExportersDisplayNameAndURL() {
        List<Tuple2<String, String>> exportersInfo = new ArrayList<>();

        exportService.getAllExporters().values().stream()
                .filter(Exporter::isAvailableToUsers)
                .forEach(exporter -> exportersInfo.add(Tuple.of(exporter.getDisplayName(), createExporterURL(exporter, systemConfig.getDataverseSiteUrl()))));

        return exportersInfo;
    }

    public String getAlternativePersistentIdentifier() {
        return datasetDao.find(dataset.getId()).getAlternativePersistentIdentifier();
    }

    // -------------------- PRIVATE --------------------

    private String createExporterURL(Exporter exporter, String myHostURL) {
        return myHostURL + "/api/datasets/export?exporter=" + exporter.getProviderName() + "&persistentId=" + dataset.getGlobalIdString();
    }
}
