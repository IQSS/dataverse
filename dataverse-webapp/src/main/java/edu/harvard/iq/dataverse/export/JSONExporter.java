package edu.harvard.iq.dataverse.export;

import edu.harvard.iq.dataverse.citation.CitationFactory;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import org.apache.commons.lang.StringUtils;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;



@ApplicationScoped
public class JSONExporter extends ExporterBase {

    // -------------------- CONSTRUCTORS --------------------

    @Inject
    public JSONExporter(SettingsServiceBean settingsService, CitationFactory citationFactory) {
        super(citationFactory, settingsService);
    }

    // -------------------- LOGIC --------------------

    @Override
    public String getProviderName() {
        return ExporterType.JSON.getPrefix();
    }

    @Override
    public ExporterType getExporterType() {
        return ExporterType.JSON;
    }

    @Override
    public String getDisplayName() {
        return BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.json") != null
                ? BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.json") : "JSON";
    }

    @Override
    public String exportDataset(DatasetVersion version) {
        return createDatasetJsonString(version);
    }

    @Override
    public Boolean isXMLFormat() {
        return false;
    }

    @Override
    public Boolean isHarvestable() {
        return true;
    }

    @Override
    public Boolean isAvailableToUsers() {
        return true;
    }

    @Override
    public String getXMLNameSpace() {
        return StringUtils.EMPTY;
    }

    @Override
    public String getXMLSchemaLocation() {
        return StringUtils.EMPTY;
    }

    @Override
    public String getXMLSchemaVersion() {
        return StringUtils.EMPTY;
    }

    @Override
    public String getMediaType() {
        return MediaType.APPLICATION_JSON;
    }

}
