package edu.harvard.iq.dataverse;

import java.io.Serializable;
import java.util.Date;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

/**
 *
 * @author Marina
 */

@Entity
@Table(name = "fileaccessrequests")

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
                query = "SELECT far FROM FileAccessRequest far WHERE far.dataFile.id=:dataFileId and far.requestState=:requestState"),
        @NamedQuery(name = "FileAccessRequest.findByAuthenticatedUserIdAndDataFileIdAndRequestState",
                query = "SELECT far FROM FileAccessRequest far WHERE far.user.id=:authenticatedUserId and far.dataFile.id=:dataFileId and far.requestState=:requestState")
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
    
    public enum RequestState {CREATED, GRANTED, REJECTED};
    //private RequestState state;
    @Enumerated(EnumType.STRING)
    @Column(name="request_state", nullable=false )
    private RequestState requestState;
    
    @Temporal(value = TemporalType.TIMESTAMP)
    @Column(name = "creation_time")
    private Date creationTime;
    
    public FileAccessRequest(){
    }
    
    public FileAccessRequest(DataFile df, AuthenticatedUser au){
        setDataFile(df);
        setRequester(au);
        setState(RequestState.CREATED);
        setCreationTime(new Date());
    }
    
    public FileAccessRequest(DataFile df, AuthenticatedUser au, GuestbookResponse gbr){
        this(df, au);
        setGuestbookResponse(gbr);
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
        if(isStateGranted()) {
            return "granted";
        }
        if(isStateRejected()) {
            return "rejected";
        }
        return null; 
    }
    
    public void setStateCreated() {
        this.requestState = RequestState.CREATED;
    }
    
    public void setStateGranted() {
        this.requestState = RequestState.GRANTED;
    }

    public void setStateRejected() {
        this.requestState = RequestState.REJECTED;
    }

    public boolean isStateCreated() {
        return this.requestState == RequestState.CREATED;
    }
   
    public boolean isStateGranted() {
        return this.requestState == RequestState.GRANTED;
    }

    public boolean isStateRejected() {
        return this.requestState == RequestState.REJECTED;
    }
    
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    public Date getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
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