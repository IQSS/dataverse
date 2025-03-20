package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.DateUtil;

import java.io.Serializable;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Collections;
import java.util.HashSet;
import java.util.stream.Collectors;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 *
 * @author xyang
 */
@Entity
@Table(indexes = {@Index(columnList="user_id")})

public class UserNotification implements Serializable {
    // Keep in sync with list at admin/user-administration.rst
    public enum Type {
        ASSIGNROLE, REVOKEROLE, CREATEDV, CREATEDS, CREATEACC, SUBMITTEDDS, RETURNEDDS, 
        PUBLISHEDDS, REQUESTFILEACCESS, GRANTFILEACCESS, REJECTFILEACCESS, FILESYSTEMIMPORT, 
        CHECKSUMIMPORT, CHECKSUMFAIL, CONFIRMEMAIL, APIGENERATED, INGESTCOMPLETED, INGESTCOMPLETEDWITHERRORS, 
        PUBLISHFAILED_PIDREG, WORKFLOW_SUCCESS, WORKFLOW_FAILURE, STATUSUPDATED, DATASETCREATED, DATASETMENTIONED,
        GLOBUSUPLOADCOMPLETED, GLOBUSUPLOADCOMPLETEDWITHERRORS,
        GLOBUSDOWNLOADCOMPLETED, GLOBUSDOWNLOADCOMPLETEDWITHERRORS, REQUESTEDFILEACCESS,
        GLOBUSUPLOADREMOTEFAILURE, GLOBUSUPLOADLOCALFAILURE, PIDRECONCILED;
        
        public String getDescription() {
            return BundleUtil.getStringFromBundle("notification.typeDescription." + this.name());
        }

        public boolean hasDescription() {
            final String description = getDescription();
            return description != null && !description.isEmpty();
        }

        public static Set<Type> tokenizeToSet(String tokens) {
            if (tokens == null || tokens.isEmpty()) {
                return new HashSet<>();
            }
            return Collections.list(new StringTokenizer(tokens, ",")).stream()
                .map(token -> Type.valueOf(((String) token).trim()))
                .collect(Collectors.toSet());
        }

        public static String toStringValue(Set<Type> typesSet) {
            if (typesSet == null || typesSet.isEmpty()) {
                return null;
            }
            return String.join(",", typesSet.stream().map(x -> x.name()).collect(Collectors.toList()));
        }
    };
    
    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn( nullable = false )
    private AuthenticatedUser user;
    @ManyToOne
    /** Requestor now has a more general meaning of 'actor' - the person who's action is causing the emails.
     * The original use of that was for people requesting dataset access
     * This is also now used for DATASETCREATED messages where it indicates who created the dataset
    */
    @JoinColumn( nullable = true )
    private AuthenticatedUser requestor;
    private Timestamp sendDate;
    private boolean readNotification;
    
    @Enumerated
    @Column( nullable = false )
    private Type type;
    private Long objectId;
    
    private String additionalInfo;

    @Transient
    private boolean displayAsRead;
    
    @Transient 
    String roleString;

    private boolean emailed;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AuthenticatedUser getUser() {
        return user;
    }

    public void setUser(AuthenticatedUser user) {
        this.user = user;
    }
        
    public AuthenticatedUser getRequestor() {
        return requestor;
    }

    public void setRequestor(AuthenticatedUser requestor) {
        this.requestor = requestor;
    }

    public String getSendDate() {
        return new SimpleDateFormat("MMMM d, yyyy h:mm a z").format(sendDate);
    }

    public Timestamp getSendDateTimestamp() {
        return sendDate;
    }

    public void setSendDate(Timestamp sendDate) {
        this.sendDate = sendDate;
    }

    public boolean isReadNotification() {
        return readNotification;
    }

    public void setReadNotification(boolean readNotification) {
        this.readNotification = readNotification;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }
    
    public Long getObjectId() {
        return objectId;
    }

    public void setObjectId(Long objectId) {
        this.objectId = objectId;
    }
    
    @Transient 
    private Object theObject;

    public Object getTheObject() {
        return theObject;
    }

    public void setTheObject(Object theObject) {
        this.theObject = theObject;
    }
    
        
    public boolean isDisplayAsRead() {
        return displayAsRead;
    }

    public void setDisplayAsRead(boolean displayAsRead) {
        this.displayAsRead = displayAsRead;
    }

    public boolean isEmailed() {
        return emailed;
    }

    public void setEmailed(boolean emailed) {
        this.emailed = emailed;
    }    
    
    public String getRoleString() {
        return roleString;
    }

    public void setRoleString(String roleString) {
        this.roleString = roleString;
    }

    public String getLocaleSendDate() {
        return DateUtil.formatDate(sendDate);
    }

    public String getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(String additionalInfo) {
        this.additionalInfo = additionalInfo;
    }
}
