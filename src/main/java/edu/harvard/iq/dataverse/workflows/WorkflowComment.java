package edu.harvard.iq.dataverse.workflows;

import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;

@Entity
public class WorkflowComment implements Serializable {

    /*
    This release only supports Return to Author as a comment type
    More may be added in future releases,
    */
    public enum Type {
        RETURN_TO_AUTHOR, //, SUBMIT_FOR_REVIEW not available in this release but may be added in the future
        WORKFLOW_SUCCESS,
        WORKFLOW_FAILURE
    };
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Right now only datasetVersions are the only supported entity that can be
     * discussed via workflow comments. In the future, we plan to also use this
     * table for discussion of other entities such as files in the "Request
     * Access" workflow. We may even allow for discussion of datasets to allow
     * users to explain why they'd like to have a contributor role on a dataset.
     *
     * TODO: In the future, make this nullable=true for when entities other than
     * DatasetVersion are being discussed, such as DataFile or Dataset.
     */
    @JoinColumn(nullable = false)
    private DatasetVersion datasetVersion;

    /**
     * The WorkflowAction string has a namespace for the workflow followed by
     * the action.
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Type type;

    // This is nullable because when you submit for review, especially the first time, you don't have to enter a message.
    @Column(nullable = true, columnDefinition = "TEXT")
    private String message;

    // This is nullable so the user can be deleted if necessary.
    @JoinColumn(nullable = true)
    private AuthenticatedUser authenticatedUser;

    @Column(nullable = false)
    private Timestamp created;

    private boolean toBeShown;
    
    // TODO: Consider support editing in the GUI some day, like GitHub issue comments (show "Edited" in the UI). We won't send a second email, however. You only get one shot to prevent spam.
//    @Transient
//    private Timestamp modified;
    // TODO: How should we best associate these entries to notifications, which can go to multiple authors and curators?
    //FWIW: Workflow success/failure messages get shown to the user running the workflow if/when on the relevant dataset version page
//    @Transient
//    private List<UserNotification> notifications;
    public WorkflowComment(DatasetVersion version, WorkflowComment.Type type, String message, AuthenticatedUser authenticatedUser) {
        this.type = type;
        this.datasetVersion = version;
        this.message = message;
        this.authenticatedUser = authenticatedUser;
        this.created = new Timestamp(new Date().getTime());
        this.setToBeShown(true);
    }

    /**
     * This default constructor is only here to prevent this error at
     * deployment:
     *
     * Exception Description: The instance creation method [...[class
     * name].<Default Constructor>], with no parameters, does not exist, or is
     * not accessible
     *
     * Don't use this constructor.
     */
    @Deprecated
    public WorkflowComment() {
    }

    public Long getId() {
        return id;
    }

    public DatasetVersion getDatasetVersion() {
        return datasetVersion;
    }

    public Type getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public AuthenticatedUser getAuthenticatedUser() {
        return authenticatedUser;
    }
    
    public void setAuthenticatedUser(AuthenticatedUser authenticatedUser){
        this.authenticatedUser = authenticatedUser;
    }

    public Timestamp getCreated() {
        return created;
    }

    public void setDatasetVersion(DatasetVersion dv) {
        datasetVersion=dv;
    }

    public boolean isToBeShown() {
        return toBeShown;
    }

    public void setToBeShown(boolean toBeShown) {
        this.toBeShown = toBeShown;
    }

}
