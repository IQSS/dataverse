package edu.harvard.iq.dataverse.api.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse;
import edu.harvard.iq.dataverse.persistence.datafile.license.License;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class FileMetadataDTO {
    private String description;
    private String label;
    private Boolean restricted;
    private String termsOfUseType;
    private String licenseName;
    private String licenseUrl;
    private String accessConditions;
    private String accessConditionsCustomText;
    private String directoryLabel;
    private Long version;
    private Long datasetVersionId;
    private List<String> categories;
    private DataFileDTO dataFile;

    // -------------------- GETTERS --------------------

    public String getDescription() {
        return description;
    }

    public String getLabel() {
        return label;
    }

    public Boolean getRestricted() {
        return restricted;
    }

    public String getTermsOfUseType() {
        return termsOfUseType;
    }

    public String getLicenseName() {
        return licenseName;
    }

    public String getLicenseUrl() {
        return licenseUrl;
    }

    public String getAccessConditions() {
        return accessConditions;
    }

    public String getAccessConditionsCustomText() {
        return accessConditionsCustomText;
    }

    public String getDirectoryLabel() {
        return directoryLabel;
    }

    public Long getVersion() {
        return version;
    }

    public Long getDatasetVersionId() {
        return datasetVersionId;
    }

    public List<String> getCategories() {
        return categories;
    }

    public DataFileDTO getDataFile() {
        return dataFile;
    }

    // -------------------- SETTERS --------------------

    public void setDescription(String description) {
        this.description = description;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setRestricted(Boolean restricted) {
        this.restricted = restricted;
    }

    public void setTermsOfUseType(String termsOfUseType) {
        this.termsOfUseType = termsOfUseType;
    }

    public void setLicenseName(String licenseName) {
        this.licenseName = licenseName;
    }

    public void setLicenseUrl(String licenseUrl) {
        this.licenseUrl = licenseUrl;
    }

    public void setAccessConditions(String accessConditions) {
        this.accessConditions = accessConditions;
    }

    public void setAccessConditionsCustomText(String accessConditionsCustomText) {
        this.accessConditionsCustomText = accessConditionsCustomText;
    }

    public void setDirectoryLabel(String directoryLabel) {
        this.directoryLabel = directoryLabel;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public void setDatasetVersionId(Long datasetVersionId) {
        this.datasetVersionId = datasetVersionId;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    public void setDataFile(DataFileDTO dataFile) {
        this.dataFile = dataFile;
    }

    // -------------------- INNER CLASSES --------------------

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
    public static class DataFileDTO {
        private Long id;
        private String persistentId;
        private String pidURL;
        private String filename;
        private String contentType;
        private Long filesize;
        private String description;
        private String storageIdentifier;
        private String originalFileFormat;
        private String originalFormatLabel;
        private Long originalFileSize;

        @JsonProperty("UNF")
        private String UNF;

        private Long rootDataFileId;
        private Long previousDataFileId;
        private String md5;
        private ChecksumDTO checksum;
        private List<String> tabularTags;

        // -------------------- GETTERS --------------------

        public Long getId() {
            return id;
        }

        public String getPersistentId() {
            return persistentId;
        }

        public String getPidURL() {
            return pidURL;
        }

        public String getFilename() {
            return filename;
        }

        public String getContentType() {
            return contentType;
        }

        public Long getFilesize() {
            return filesize;
        }

        public String getDescription() {
            return description;
        }

        public String getStorageIdentifier() {
            return storageIdentifier;
        }

        public String getOriginalFileFormat() {
            return originalFileFormat;
        }

        public String getOriginalFormatLabel() {
            return originalFormatLabel;
        }

        public Long getOriginalFileSize() {
            return originalFileSize;
        }

        public String getUNF() {
            return UNF;
        }

        public Long getRootDataFileId() {
            return rootDataFileId;
        }

        public Long getPreviousDataFileId() {
            return previousDataFileId;
        }

        public String getMd5() {
            return md5;
        }

        public ChecksumDTO getChecksum() {
            return checksum;
        }

        public List<String> getTabularTags() {
            return tabularTags;
        }

        // -------------------- SETTERS --------------------

        public void setId(Long id) {
            this.id = id;
        }

        public void setPersistentId(String persistentId) {
            this.persistentId = persistentId;
        }

        public void setPidURL(String pidURL) {
            this.pidURL = pidURL;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        public void setFilesize(Long filesize) {
            this.filesize = filesize;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public void setStorageIdentifier(String storageIdentifier) {
            this.storageIdentifier = storageIdentifier;
        }

        public void setOriginalFileFormat(String originalFileFormat) {
            this.originalFileFormat = originalFileFormat;
        }

        public void setOriginalFormatLabel(String originalFormatLabel) {
            this.originalFormatLabel = originalFormatLabel;
        }

        public void setOriginalFileSize(Long originalFileSize) {
            this.originalFileSize = originalFileSize;
        }

        public void setUNF(String UNF) {
            this.UNF = UNF;
        }

        public void setRootDataFileId(Long rootDataFileId) {
            this.rootDataFileId = rootDataFileId;
        }

        public void setPreviousDataFileId(Long previousDataFileId) {
            this.previousDataFileId = previousDataFileId;
        }

        public void setMd5(String md5) {
            this.md5 = md5;
        }

        public void setChecksum(ChecksumDTO checksum) {
            this.checksum = checksum;
        }

        public void setTabularTags(List<String> tabularTags) {
            this.tabularTags = tabularTags;
        }
    }

    public static class Converter {

        private ChecksumDTO.Creator checksumCreator = new ChecksumDTO.Creator();

        // -------------------- LOGIC --------------------

        public FileMetadataDTO convert(FileMetadata metadata) {
            FileMetadataDTO converted = new FileMetadataDTO();
            converted.setDescription(metadata.getDescription());
            converted.setLabel(metadata.getLabel());
            converted.setRestricted(metadata.getTermsOfUse().getTermsOfUseType() == FileTermsOfUse.TermsOfUseType.RESTRICTED);
            converted.setTermsOfUseType(metadata.getTermsOfUse().getTermsOfUseType().toString());
            Optional<License> license = Optional.ofNullable(metadata.getTermsOfUse())
                    .map(FileTermsOfUse::getLicense);
            converted.setLicenseName(license.map(License::getName).orElse(null));
            converted.setLicenseUrl(license.map(License::getUrl).orElse(null));
            converted.setAccessConditions(Optional.ofNullable(metadata.getTermsOfUse().getRestrictType())
                    .map(FileTermsOfUse.RestrictType::name)
                    .orElse(null));
            converted.setAccessConditionsCustomText(metadata.getTermsOfUse().getRestrictCustomText());
            converted.setDirectoryLabel(metadata.getDirectoryLabel());
            converted.setVersion(metadata.getVersion());
            converted.setDatasetVersionId(metadata.getDatasetVersion().getId());
            converted.setCategories(metadata.getCategoriesByName());
            converted.setDataFile(convert(metadata.getLabel(), metadata.getDataFile()));
            return converted;
        }

        public List<FileMetadataDTO> convert(List<FileMetadata> metadatas) {
            return metadatas.stream()
                    .map(this::convert)
                    .collect(Collectors.toList());
        }

        // -------------------- PRIVATE --------------------

        private DataFileDTO convert(String fileName, DataFile dataFile) {
            DataFileDTO converted = new DataFileDTO();
            converted.setId(dataFile.getId());
            converted.setPersistentId(dataFile.getGlobalId().asString());
            converted.setPidURL(Optional.ofNullable(dataFile.getGlobalId().toURL())
                    .map(URL::toString)
                    .orElse(null));
            converted.setFilename(fileName);
            converted.setContentType(dataFile.getContentType());
            converted.setFilesize(dataFile.getFilesize());
            converted.setDescription(dataFile.getDescription());
            converted.setStorageIdentifier(dataFile.getStorageIdentifier());
            converted.setOriginalFileFormat(dataFile.getOriginalFileFormat());
            converted.setOriginalFormatLabel(dataFile.getOriginalFormatLabel());
            converted.setOriginalFileSize(dataFile.getOriginalFileSize());
            converted.setUNF(dataFile.getUnf());
            converted.setRootDataFileId(dataFile.getRootDataFileId());
            converted.setPreviousDataFileId(dataFile.getPreviousDataFileId());
            converted.setMd5(DataFile.ChecksumType.MD5.equals(dataFile.getChecksumType()) ? dataFile.getChecksumValue() : null);
            converted.setChecksum(checksumCreator.create(dataFile));
            converted.setTabularTags(dataFile.getTagLabels());
            return converted;
        }
    }
}
