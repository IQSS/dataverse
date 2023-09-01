package edu.harvard.iq.dataverse.actionlogging;

import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

/**
 * Logs a single action in the action log.
 * @author michael
 */
@Entity
@Table(indexes = {@Index(columnList="useridentifier"), @Index(columnList="actiontype"), @Index(columnList="starttime")})
public class ActionLogRecord implements java.io.Serializable {
    
    public enum Result {
        OK, BadRequest, PermissionError, InternalError
    }
    
    public enum ActionType {
        /** login, logout */
        SessionManagement,
        
        /** Command execution */
        Command,
        
        BuiltinUser, 
        
        /** A setting being updated */
        Setting,
        
        Auth,
        
        Admin,

        ExternalTool,
        
        GlobalGroups
    }
    
    @Id
    @Column( length=36 )
    private String id;
    
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date startTime;
    
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date endTime;
    
    @Enumerated(EnumType.STRING)
    private Result actionResult;
    
    private String userIdentifier;
    
    @Enumerated(EnumType.STRING)
    private ActionType actionType;
    
    private String actionSubType;
    
    @Column(columnDefinition="TEXT")
    private String info;
    
    public ActionLogRecord(){}
    
    /**
     * @param anActionType
     * @param anActionSubType
     */
    // TODO: Add ability to set `info` in constructor.
    public ActionLogRecord( ActionType anActionType, String anActionSubType ) {
        actionType = anActionType;
        actionSubType = anActionSubType;
        startTime = new Date();
    }
    
    @Override
    public String toString() {
        return "[ActionLogRecord id:" + getId() + " type:" + getActionType() 
                    + "/" + getActionSubType()
                    + " result:" + getActionResult() + "]";
    }
    
    @PrePersist
    void prepresist() {
        if ( id == null ) {
            id = UUID.randomUUID().toString();
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getStartTime() {
        return startTime;
    }

    public ActionLogRecord setStartTime(Date startTime) {
        this.startTime = startTime;
        return this;
    }

    public Date getEndTime() {
        return endTime;
    }

    public ActionLogRecord setEndTime(Date endTime) {
        this.endTime = endTime;
        return this;
    }

    public Result getActionResult() {
        return actionResult;
    }

    public ActionLogRecord setActionResult(Result actionResult) {
        this.actionResult = actionResult;
        return this;
    }

    public String getUserIdentifier() {
        return userIdentifier;
    }

    public ActionLogRecord setUserIdentifier(String userIdentifier) {
        this.userIdentifier = userIdentifier;
        return this;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public ActionLogRecord setActionType(ActionType actionType) {
        this.actionType = actionType;
        return this;
    }

    public String getActionSubType() {
        return actionSubType;
    }

    public ActionLogRecord setActionSubType(String actionSubType) {
        this.actionSubType = actionSubType;
        return this;
    }

    public String getInfo() {
        return info;
    }

    public ActionLogRecord setInfo(String info) {
        this.info = info;
        return this;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 71 * hash + Objects.hashCode(this.id);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ActionLogRecord other = (ActionLogRecord) obj;
        if (!Objects.equals(this.startTime, other.startTime)) {
            return false;
        }
        if (!Objects.equals(this.endTime, other.endTime)) {
            return false;
        }
        if (this.actionResult != other.actionResult) {
            return false;
        }
        if (!Objects.equals(this.userIdentifier, other.userIdentifier)) {
            return false;
        }
        if (this.actionType != other.actionType) {
            return false;
        }
        if (!Objects.equals(this.actionSubType, other.actionSubType)) {
            return false;
        }
        return Objects.equals(this.info, other.info);
    }
    
    
    
}
