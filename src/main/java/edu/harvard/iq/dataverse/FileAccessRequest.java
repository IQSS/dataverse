package edu.harvard.iq.dataverse;

import java.io.Serializable;
import javax.persistence.CascadeType;
import javax.persistence.GeneratedValue;
import javax.persistence.UniqueConstraint;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Index;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.EnumType;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.datavariable.DataVariable;
import javax.persistence.Column;
import javax.persistence.Enumerated;
import javax.persistence.GenerationType;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

/**
 *
 * @author Marina
 */

@Entity
@Table(name = "fileaccessrequests", //having added the guestbookresponse_id column to fileaccessrequests
    uniqueConstraints=@UniqueConstraint(columnNames={"datafile_id", "authenticated_user_id"}) //this may not make sense at some future point
)

@NamedQueries({
        @NamedQuery(name = "FileAccessRequest.findByAuthenticatedUserId",
                query = "SELECT far FROM FileAccessRequest far WHERE far.user.id=:authenticatedUserId"),
        @NamedQuery(name = "FileAccessRequest.findByGuestbookResponseId",
                query = "SELECT far FROM FileAccessRequest far WHERE far.guestbookResponse.id=:guestbookResponseId"),
        @NamedQuery(name = "FileAccessRequest.findByDataFileId",
                query = "SELECT far FROM FileAccessRequest far WHERE far.dataFile.id=:dataFileId"),
        @NamedQuery(name = "FileAccessRequest.findByRequestState",
                query = "SELECT far FROM FileAccessRequest far WHERE far.requestState=:requestState"),
        @NamedQuery(name = "FileAccessRequest.findByAuthenticatedUserIdAndRequestState",
                query = "SELECT far FROM FileAccessRequest far WHERE far.user.id=:authenticatedUserId and far.requestState=:requestState"),
        @NamedQuery(name = "FileAccessRequest.findByGuestbookResponseIdAndRequestState",
                query = "SELECT far FROM FileAccessRequest far WHERE far.guestbookResponse.id=:guestbookResponseId and far.requestState=:requestState"),
        @NamedQuery(name = "FileAccessRequest.findByDataFileIdAndRequestState",
                query = "SELECT far FROM FileAccessRequest far WHERE far.dataFile.id=:dataFileId and far.requestState=:requestState")
})

public class FileAccessRequest implements Serializable{
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(nullable=false)
    private DataFile dataFile;
    
    @ManyToOne
    @JoinColumn(name="authenticated_user_id",nullable=false)
    private AuthenticatedUser user;
    
    @OneToOne
    @JoinColumn(nullable=true)
    private GuestbookResponse guestbookResponse;
    
    public enum RequestState {CREATED,EDITED,GRANTED,REJECTED,RESUBMIT,INVALIDATED,CLOSED};
    //private RequestState state;
    @Enumerated(EnumType.STRING)
    @Column( nullable=false )
    private RequestState requestState;
    
    public FileAccessRequest(){
        
    }
    
    public FileAccessRequest(DataFile df, AuthenticatedUser au){
        setDataFile(df);
        setRequester(au);
        setState(RequestState.CREATED);
    }
    
    public FileAccessRequest(DataFile df, AuthenticatedUser au, GuestbookResponse gbr){
        setDataFile(df);
        setRequester(au);
        setGuestbookResponse(gbr);
        setState(RequestState.CREATED);
    }
     
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    
    public DataFile getDataFile(){
        return dataFile;
    }
    
    public final void setDataFile(DataFile df){
        this.dataFile = df;
    }
    
    public AuthenticatedUser getRequester(){
        return user;
    }
    
    public final void setRequester(AuthenticatedUser au){
        this.user = au;
    }
    
    public GuestbookResponse getGuestbookResponse(){
        return guestbookResponse;
    }
    
    public final void setGuestbookResponse(GuestbookResponse gbr){
        this.guestbookResponse = gbr;
    }
    
    public RequestState getState() {
        return this.requestState;
    }
    
    public void setState(RequestState requestState) {
        this.requestState = requestState;
    }
    
    public String getStateLabel() {
        if(isStateCreated()){
            return "created";
        }
        if(isStateEdited()) {
            return "edited";
        }
        if(isStateGranted()) {
            return "granted";
        }
        if(isStateRejected()) {
            return "rejected";
        }
        if(isStateResubmit()) {
            return "resubmit";
        }
        if(isStateInvalidated()) {
            return "invalidated";
        }
        if(isStateClosed()) {
            return "closed";
        }
        return null; 
    }
    
    public void setStateCreated() {
        this.requestState = RequestState.CREATED;
    }
    
    public void setStateEdited() {
        this.requestState = RequestState.EDITED;
    }
    
    public void setStateGranted() {
        this.requestState = RequestState.GRANTED;
    }

    public void setStateRejected() {
        this.requestState = RequestState.REJECTED;
    }

    public void setStateResubmit() {
        this.requestState = RequestState.RESUBMIT;
    }
    
    public void setStateInvalidated() {
        this.requestState = RequestState.INVALIDATED;
    }

    public void setStateClosed() {
        this.requestState = RequestState.CLOSED;
    }

    
    public boolean isStateCreated() {
        return this.requestState == RequestState.CREATED;
    }
   
    public boolean isStateEdited() {
        return this.requestState == RequestState.EDITED;
    }
    
    public boolean isStateGranted() {
        return this.requestState == RequestState.GRANTED;
    }

    public boolean isStateRejected() {
        return this.requestState == RequestState.REJECTED;
    }

    public boolean isStateResubmit() {
        return this.requestState == RequestState.RESUBMIT;
    }
    
    public boolean isStateInvalidated() {
        return this.requestState == RequestState.INVALIDATED;
    }

    public boolean isStateClosed() {
        return this.requestState == RequestState.CLOSED;
    }
    
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof FileAccessRequest)) {
            return false;
        }
        FileAccessRequest other = (FileAccessRequest) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }
    
    
}