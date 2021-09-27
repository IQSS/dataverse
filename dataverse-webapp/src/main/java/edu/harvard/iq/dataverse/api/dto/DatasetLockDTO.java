package edu.harvard.iq.dataverse.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetLock;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class DatasetLockDTO {
    private String lockType;
    private String date;
    private String user;
    private String message;

    // -------------------- GETTERS --------------------

    public String getLockType() {
        return lockType;
    }

    public String getDate() {
        return date;
    }

    public String getUser() {
        return user;
    }

    public String getMessage() {
        return message;
    }

    // -------------------- SETTERS --------------------

    public void setLockType(String lockType) {
        this.lockType = lockType;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    // -------------------- INNER CLASSES --------------------

    public static class Converter {
        public DatasetLockDTO convert(DatasetLock datasetLock) {
            DatasetLockDTO converted = new DatasetLockDTO();
            converted.setLockType(datasetLock.getReason().toString());
            converted.setDate(datasetLock.getStartTime().toString());
            converted.setUser(datasetLock.getUser().getIdentifier());
            converted.setMessage(datasetLock.getInfo());
            return converted;
        }
    }
}
