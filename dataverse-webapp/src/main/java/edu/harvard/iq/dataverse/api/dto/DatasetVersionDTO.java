package edu.harvard.iq.dataverse.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import edu.harvard.iq.dataverse.citation.CitationFactory;
import edu.harvard.iq.dataverse.common.Util;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class DatasetVersionDTO {
        private Long id;
        private String storageIdentifier;
        private Long versionNumber;
        private Long versionMinorNumber;
        private String versionState;
        private String versionNote;
        private String archiveNote;
        private String deaccessionLink;
        private String distributionDate;
        private String productionDate;
        private String UNF;
        private String archiveTime;
        private String lastUpdateTime;
        private String releaseTime;
        private String createTime;
        private Map<String, MetadataBlockWithFieldsDTO> metadataBlocks = Collections.emptyMap();
        private List<FileMetadataDTO> files;

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private String citation;

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private String releaseDate;

    // -------------------- GETTERS --------------------

    public Long getId() {
        return id;
    }

    public String getStorageIdentifier() {
        return storageIdentifier;
    }

    public Long getVersionNumber() {
        return versionNumber;
    }

    public Long getVersionMinorNumber() {
        return versionMinorNumber;
    }

    public String getVersionState() {
        return versionState;
    }

    public String getVersionNote() {
        return versionNote;
    }

    public String getArchiveNote() {
        return archiveNote;
    }

    public String getDeaccessionLink() {
        return deaccessionLink;
    }

    public String getDistributionDate() {
        return distributionDate;
    }

    public String getProductionDate() {
        return productionDate;
    }

    public String getUNF() {
        return UNF;
    }

    public String getArchiveTime() {
        return archiveTime;
    }

    public String getLastUpdateTime() {
        return lastUpdateTime;
    }

    public String getReleaseTime() {
        return releaseTime;
    }

    public String getCreateTime() {
        return createTime;
    }

    public Map<String, MetadataBlockWithFieldsDTO> getMetadataBlocks() {
        return metadataBlocks;
    }

    public List<FileMetadataDTO> getFiles() {
        return files;
    }

    public String getCitation() {
        return citation;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    // -------------------- LOGIC --------------------

    public DatasetVersionDTO clearEmailFields() {
        metadataBlocks.values().forEach(MetadataBlockWithFieldsDTO::clearEmailFields);
        return this;
    }

    // -------------------- SETTERS --------------------

    public void setId(Long id) {
        this.id = id;
    }

    public void setStorageIdentifier(String storageIdentifier) {
        this.storageIdentifier = storageIdentifier;
    }

    public void setVersionNumber(Long versionNumber) {
        this.versionNumber = versionNumber;
    }

    public void setVersionMinorNumber(Long versionMinorNumber) {
        this.versionMinorNumber = versionMinorNumber;
    }

    public void setVersionState(String versionState) {
        this.versionState = versionState;
    }

    public void setVersionNote(String versionNote) {
        this.versionNote = versionNote;
    }

    public void setArchiveNote(String archiveNote) {
        this.archiveNote = archiveNote;
    }

    public void setDeaccessionLink(String deaccessionLink) {
        this.deaccessionLink = deaccessionLink;
    }

    public void setDistributionDate(String distributionDate) {
        this.distributionDate = distributionDate;
    }

    public void setProductionDate(String productionDate) {
        this.productionDate = productionDate;
    }

    public void setUNF(String UNF) {
        this.UNF = UNF;
    }

    public void setArchiveTime(String archiveTime) {
        this.archiveTime = archiveTime;
    }

    public void setLastUpdateTime(String lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    public void setReleaseTime(String releaseTime) {
        this.releaseTime = releaseTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public void setMetadataBlocks(Map<String, MetadataBlockWithFieldsDTO> metadataBlocks) {
        this.metadataBlocks = metadataBlocks;
    }

    public void setFiles(List<FileMetadataDTO> files) {
        this.files = files;
    }

    public void setCitation(String citation) {
        this.citation = citation;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    // -------------------- INNER CLASSES --------------------

    public static class Converter {

        private CitationFactory citationFactory;

        // -------------------- CONSTRUCTORS --------------------

        public Converter() { }

        public Converter(CitationFactory citationFactory) {
            this.citationFactory = citationFactory;
        }

        // -------------------- LOGIC --------------------

        public DatasetVersionDTO convert(DatasetVersion datasetVersion) {
            DatasetVersionDTO converted = new DatasetVersionDTO();
            converted.setId(datasetVersion.getId());
            converted.setStorageIdentifier(datasetVersion.getDataset().getStorageIdentifier());
            converted.setVersionNumber(datasetVersion.getVersionNumber());
            converted.setVersionMinorNumber(datasetVersion.getMinorVersionNumber());
            converted.setVersionState(datasetVersion.getVersionState().name());
            converted.setVersionNote(datasetVersion.getVersionNote());
            converted.setArchiveNote(datasetVersion.getArchiveNote());
            converted.setDeaccessionLink(datasetVersion.getDeaccessionLink());
            converted.setDistributionDate(datasetVersion.getDistributionDate());
            // converted.setProductionDate(datasetVersion.getProductionDate());
            converted.setUNF(datasetVersion.getUNF());
            converted.setArchiveTime(formatIfNotNull(datasetVersion.getArchiveTime()));
            converted.setLastUpdateTime(formatIfNotNull(datasetVersion.getLastUpdateTime()));
            converted.setReleaseTime(formatIfNotNull(datasetVersion.getReleaseTime()));
            converted.setCreateTime(formatIfNotNull(datasetVersion.getCreateTime()));
            converted.setMetadataBlocks(extractMetadatablocks(datasetVersion));
            converted.setFiles(!datasetVersion.getDataset().hasActiveEmbargo()
                    ? new FileMetadataDTO.Converter().convert(datasetVersion.getFileMetadatas()) : null);
            return converted;
        }

        public DatasetVersionDTO convertWithCitation(DatasetVersion datasetVersion) {
            DatasetVersionDTO converted = convert(datasetVersion);
            converted.setCitation(citationFactory.create(datasetVersion).toString(false));
            return converted;
        }

        // -------------------- PRIVATE --------------------

        private Map<String, MetadataBlockWithFieldsDTO> extractMetadatablocks(DatasetVersion datasetVersion) {
            MetadataBlockWithFieldsDTO.Creator creator = new MetadataBlockWithFieldsDTO.Creator();
            List<DatasetField> fields = datasetVersion.getDatasetFields();
            Map<MetadataBlock, List<DatasetField>> fieldsByBlock = DatasetField.groupByBlock(fields);
            return fieldsByBlock.entrySet().stream()
                    .collect(Collectors.toMap(e -> e.getKey().getName(), e -> creator.create(e.getKey(), e.getValue()),
                            (prev, next) -> next, LinkedHashMap::new));
        }

        private String formatIfNotNull(Date date) {
            return date != null ? Util.getDateTimeFormat().format(date) : null;
        }
    }
}
