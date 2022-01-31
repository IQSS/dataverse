package edu.harvard.iq.dataverse.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import edu.harvard.iq.dataverse.common.Util;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import io.vavr.control.Option;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class DatasetDTO {
    private Long id;
    private String identifier;
    private String persistentUrl;
    private String protocol;
    private String authority;
    private String publisher;
    private String publicationDate;
    private String storageIdentifier;
    private Boolean hasActiveGuestbook;
    private String embargoDate;
    private Boolean embargoActive;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private DatasetVersionDTO datasetVersion;

    // -------------------- GETTERS --------------------

    public Long getId() {
        return id;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getPersistentUrl() {
        return persistentUrl;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getAuthority() {
        return authority;
    }

    public String getPublisher() {
        return publisher;
    }

    public String getPublicationDate() {
        return publicationDate;
    }

    public String getStorageIdentifier() {
        return storageIdentifier;
    }

    public Boolean getHasActiveGuestbook() {
        return hasActiveGuestbook;
    }

    public String getEmbargoDate() {
        return embargoDate;
    }

    public Boolean getEmbargoActive() {
        return embargoActive;
    }

    public DatasetVersionDTO getDatasetVersion() {
        return datasetVersion;
    }

    // -------------------- LOGIC --------------------

    public Map<String, Object> asMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", getId());
        result.put("identifier", getIdentifier());
        result.put("persistentUrl", getPersistentUrl());
        result.put("protocol", getProtocol());
        result.put("authority", getAuthority());
        result.put("publisher", getPublisher());
        result.put("publicationDate", getPublicationDate());
        result.put("storageIdentifier", getStorageIdentifier());
        result.put("hasActiveGuestbook", getHasActiveGuestbook());
        result.put("embargoDate", getEmbargoDate());
        result.put("embargoActive", getEmbargoActive());
        for (Iterator<String> iterator = result.keySet().iterator(); iterator.hasNext(); ) {
            String key = iterator.next();
            Object value = result.get(key);
            if (value == null || (value instanceof String && ((String) value).isEmpty())) {
                iterator.remove();
            }
        }
        return result;
    }

    // -------------------- SETTERS --------------------

    public void setId(Long id) {
        this.id = id;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public void setPersistentUrl(String persistentUrl) {
        this.persistentUrl = persistentUrl;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public void setPublicationDate(String publicationDate) {
        this.publicationDate = publicationDate;
    }

    public void setStorageIdentifier(String storageIdentifier) {
        this.storageIdentifier = storageIdentifier;
    }

    public void setHasActiveGuestbook(Boolean hasActiveGuestbook) {
        this.hasActiveGuestbook = hasActiveGuestbook;
    }

    public void setEmbargoDate(String embargoDate) {
        this.embargoDate = embargoDate;
    }

    public void setEmbargoActive(Boolean embargoActive) {
        this.embargoActive = embargoActive;
    }

    public void setDatasetVersion(DatasetVersionDTO datasetVersion) {
        this.datasetVersion = datasetVersion;
    }

    // -------------------- INNER CLASSES --------------------

    public static class Converter {

        // -------------------- LOGIC --------------------

        public DatasetDTO convert(Dataset dataset) {
            DatasetDTO converted = new DatasetDTO();
            converted.setId(dataset.getId());
            converted.setIdentifier(dataset.getIdentifier());
            converted.setPersistentUrl(dataset.getPersistentURL());
            converted.setProtocol(dataset.getProtocol());
            converted.setAuthority(dataset.getAuthority());
            converted.setPublisher(getRootDataverseName(dataset));
            converted.setPublicationDate(dataset.getPublicationDateFormattedYYYYMMDD());
            converted.setStorageIdentifier(dataset.getStorageIdentifier());
            converted.setHasActiveGuestbook(dataset.getGuestbook() != null);
            Option<Date> embargoDate = dataset.getEmbargoDate();
            converted.setEmbargoDate(embargoDate.isDefined() ? Util.getDateFormat().format(embargoDate.get()) : null);
            converted.setEmbargoActive(dataset.hasActiveEmbargo());
            return converted;
        }

        // -------------------- PRIVATE --------------------

        private String getRootDataverseName(Dataset dataset) {
            Dataverse root = dataset.getOwner();
            while (root.getOwner() != null) {
                root = root.getOwner();
            }
            String rootDataverseName = root.getName();
            return StringUtils.isNotEmpty(rootDataverseName) ? rootDataverseName : StringUtils.EMPTY;
        }
    }
}
