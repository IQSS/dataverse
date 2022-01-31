package edu.harvard.iq.dataverse.export;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.harvard.iq.dataverse.api.dto.DatasetDTO;
import edu.harvard.iq.dataverse.api.dto.DatasetVersionDTO;
import edu.harvard.iq.dataverse.citation.CitationFactory;
import edu.harvard.iq.dataverse.export.spi.Exporter;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ExporterBase implements Exporter {
    private static final Logger logger = LoggerFactory.getLogger(ExporterBase.class);

    private CitationFactory citationFactory;
    private SettingsServiceBean settingsService;

    // -------------------- CONTRUCTORS --------------------

    public ExporterBase(CitationFactory citationFactory, SettingsServiceBean settingsService) {
        this.citationFactory = citationFactory;
        this.settingsService = settingsService;
    }

    // -------------------- LOGIC --------------------

    protected String createDatasetJsonString(DatasetVersion datasetVersion) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(createDTO(datasetVersion));
        } catch (JsonProcessingException jpe) {
            logger.warn("Exception during JSON creation. Empty JSON returned", jpe);
            return "{}";
        }
    }

    protected DatasetDTO createDTO(DatasetVersion datasetVersion) {
        DatasetDTO datasetDto = new DatasetDTO.Converter().convert(datasetVersion.getDataset());
        DatasetVersionDTO versionDto = new DatasetVersionDTO.Converter(citationFactory).convertWithCitation(datasetVersion);
        datasetDto.setDatasetVersion(versionDto);
        if (settingsService.isTrueForKey(SettingsServiceBean.Key.ExcludeEmailFromExport)) {
            versionDto.clearEmailFields();
        }
        return datasetDto;
    }
}
