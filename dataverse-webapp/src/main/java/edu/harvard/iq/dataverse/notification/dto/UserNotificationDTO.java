package edu.harvard.iq.dataverse.notification.dto;

import edu.harvard.iq.dataverse.notification.NotificationObjectType;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;

import java.sql.Timestamp;
import java.util.Date;

public class UserNotificationDTO {

    private Long id;

    private Date sendDate;

    private String type;

    private String additionalMessage;

    private boolean displayAsRead;

    private String roleString;

    private Object theObject;

    private NotificationObjectType theObjectType;

    private String requestorName;

    private String requestorEmail;

    private String rejectedOrGrantedBy;

    private String replyTo;

    // -------------------- GETTERS --------------------

    public Long getId() {
        return id;
    }

    public Date getSendDate() {
        return sendDate;
    }

    public String getType() {
        return type;
    }

    public String getAdditionalMessage() {
        return additionalMessage;
    }

    public boolean isDisplayAsRead() {
        return displayAsRead;
    }

    public String getRoleString() {
        return roleString;
    }

    public Object getTheObject() {
        return theObject;
    }

    public NotificationObjectType getTheObjectType() {
        return theObjectType;
    }

    public String getRequestorName() {
        return requestorName;
    }

    public String getRequestorEmail() {
        return requestorEmail;
    }

    public String getRejectedOrGrantedBy() {
        return rejectedOrGrantedBy;
    }

    public String getReplyTo() {
        return replyTo;
    }

    // -------------------- LOGIC --------------------

    public void setTheDataverseObject(Dataverse dataverse) {
        this.theObjectType = NotificationObjectType.DATAVERSE;
        this.theObject = dataverse;
    }
    public void setTheDatasetObject(Dataset dataset) {
        this.theObjectType = NotificationObjectType.DATASET;
        this.theObject = dataset;
    }
    public void setTheDatasetVersionObject(DatasetVersion datasetVersion) {
        this.theObjectType = NotificationObjectType.DATASET_VERSION;
        this.theObject = datasetVersion;
    }
    public void setTheDataFileObject(DataFile dataFile) {
        this.theObjectType = NotificationObjectType.DATAFILE;
        this.theObject = dataFile;
    }
    public void setTheFileMetadataObject(FileMetadata fileMetadata) {
        this.theObjectType = NotificationObjectType.FILEMETADATA;
        this.theObject = fileMetadata;
    }
    public void setTheAuthenticatedUserObject(AuthenticatedUser authenticatedUser) {
        this.theObjectType = NotificationObjectType.AUTHENTICATED_USER;
        this.theObject = authenticatedUser;
    }

    // -------------------- SETTERS --------------------

    public void setId(Long id) {
        this.id = id;
    }

    public void setSendDate(Timestamp sendDate) {
        this.sendDate = sendDate;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setAdditionalMessage(String additionalMessage) {
        this.additionalMessage = additionalMessage;
    }

    public void setDisplayAsRead(boolean displayAsRead) {
        this.displayAsRead = displayAsRead;
    }

    public void setRoleString(String roleString) {
        this.roleString = roleString;
    }

    public void setRequestorName(String requestorName) {
        this.requestorName = requestorName;
    }

    public void setRequestorEmail(String requestorEmail) {
        this.requestorEmail = requestorEmail;
    }

    public void setRejectedOrGrantedBy(String rejectedOrGrantedBy) {
        this.rejectedOrGrantedBy = rejectedOrGrantedBy;
    }

    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }
}
