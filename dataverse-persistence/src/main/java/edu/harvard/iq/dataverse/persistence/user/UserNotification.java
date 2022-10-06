package edu.harvard.iq.dataverse.persistence.user;

import edu.harvard.iq.dataverse.persistence.JpaEntity;
import edu.harvard.iq.dataverse.persistence.config.PostgresJsonConverter;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.io.Serializable;
import java.sql.Timestamp;

/**
 * @author xyang
 */
@Entity
@Table(indexes = {@Index(columnList = "user_id")})
public class UserNotification implements Serializable, JpaEntity<Long> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(nullable = false)
    private AuthenticatedUser user;

    @ManyToOne
    @JoinColumn(nullable = true)
    private AuthenticatedUser requestor;

    private Timestamp sendDate;

    private boolean readNotification;

    @Column(nullable = false)
    private String type;

    private Long objectId;

    @Column(nullable = true)
    private String additionalMessage;

    private boolean emailed;

    @Convert(converter = PostgresJsonConverter.class)
    private String parameters;

    // -------------------- GETTERS --------------------

    public Long getId() {
        return id;
    }

    public AuthenticatedUser getUser() {
        return user;
    }

    public AuthenticatedUser getRequestor() {
        return requestor;
    }

    public Timestamp getSendDate() {
        return sendDate;
    }

    public boolean isReadNotification() {
        return readNotification;
    }

    /**
     * Main types are here - {@link NotificationType}
     */
    public String getType() {
        return type.toUpperCase();
    }

    public Long getObjectId() {
        return objectId;
    }

    /**
     * required only for {@link NotificationType.RETURNEDDS}
     * optional for {@link NotificationType.SUBMITTEDS}
     * @return provided by user reason for dataset publish rejection
     */
    public String getAdditionalMessage() {
        return additionalMessage;
    }

    public boolean isEmailed() {
        return emailed;
    }

    public String getParameters() {
        return parameters;
    }

    // -------------------- SETTERS --------------------

    public void setId(Long id) {
        this.id = id;
    }

    public void setUser(AuthenticatedUser user) {
        this.user = user;
    }

    public void setRequestor(AuthenticatedUser requestor) {
        this.requestor = requestor;
    }

    public void setSendDate(Timestamp sendDate) {
        this.sendDate = sendDate;
    }

    public void setEmailed(boolean emailed) {
        this.emailed = emailed;
    }

    public void setReadNotification(boolean readNotification) {
        this.readNotification = readNotification;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setObjectId(Long objectId) {
        this.objectId = objectId;
    }

    public void setAdditionalMessage(String returnToAuthorReason) {
        this.additionalMessage = returnToAuthorReason;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }
}
