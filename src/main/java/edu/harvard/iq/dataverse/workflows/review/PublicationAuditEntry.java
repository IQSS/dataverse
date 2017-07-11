package edu.harvard.iq.dataverse.workflows.review;

import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.UserNotification;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Transient;

@Entity
public class PublicationAuditEntry implements Serializable {

    public enum EntryType {
        SUBMIT, RETURN
    };

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JoinColumn(nullable = false)
    private DatasetVersion datasetVersion;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EntryType entryType;

    // This is nullable because when you submit for review, especially the first time, you don't have to enter a message.
    @Column(nullable = true, columnDefinition = "TEXT")
    private String message;

    // This is nullable so the user can be deleted if necessary.
    @JoinColumn(nullable = true)
    private AuthenticatedUser authenticatedUser;

    @Column(nullable = false)
    private Timestamp created;

    // TODO: support editing in the GUI some day. We won't send a second email, however. Only one shot for email.
    @Column(nullable = false)
    private Timestamp modified;

    // TODO: How should we best associate these entries to notifications, which can go to multiple authors and curators?
    @Transient
    private List<UserNotification> notifications;

    public PublicationAuditEntry(DatasetVersion datasetVersion, EntryType entryType, String message) {
        this.datasetVersion = datasetVersion;
        this.entryType = entryType;
        this.message = message;
        Timestamp now = new Timestamp(new Date().getTime());
        this.modified = now;
        this.created = now;
    }

    /**
     * This default constructor is only here to prevent this error at
     * deployment:
     *
     * Exception Description: The instance creation method
     * [...PublicationAuditTrail.<Default Constructor>], with no parameters,
     * does not exist, or is not accessible
     *
     * Don't use it.
     */
    @Deprecated
    public PublicationAuditEntry() {
    }

    public Long getId() {
        return id;
    }

    public DatasetVersion getDatasetVersion() {
        return datasetVersion;
    }

    public EntryType getEntryType() {
        return entryType;
    }

    public String getMessage() {
        return message;
    }

    public AuthenticatedUser getAuthenticatedUser() {
        return authenticatedUser;
    }

    public Timestamp getCreated() {
        return created;
    }

    public Timestamp getModified() {
        return modified;
    }

}
