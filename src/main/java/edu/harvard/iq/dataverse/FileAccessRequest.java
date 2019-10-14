package edu.harvard.iq.dataverse;

import java.io.Serializable;
import javax.persistence.CascadeType;
import javax.persistence.GeneratedValue;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Index;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;

/**
 *
 * @author Marina
 */

@Entity
@Table(name = "fileaccessrequests") //having added the guestbookresponse_id column to fileaccessrequests
public class FileAccessRequest implements Serializable{
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    @JoinColumn(nullable=false)
    private DataFile dataFile;
    
    @ManyToOne
    @JoinColumn(nullable=false)
    private AuthenticatedUser authenticatedUser;

    @OneToOne
    @JoinColumn(nullable=true)
    private GuestbookResponse guestbookResponse;
    
    public FileAccessRequest(){
        
    }
    
    public FileAccessRequest(DataFile df, AuthenticatedUser au){
        setDataFile(df);
        setRequester(au);
    }
    
    public FileAccessRequest(DataFile df, AuthenticatedUser au, GuestbookResponse gbr){
        this(df,au);
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
    
    public void setDataFile(DataFile df){
        this.dataFile = df;
    }
    
    public AuthenticatedUser getRequester(){
        return authenticatedUser;
    }
    
    public void setRequester(AuthenticatedUser au){
        this.authenticatedUser = au;
    }
    
    public GuestbookResponse getGuestbookResponse(){
        return guestbookResponse;
    }
    
    public void setGuestbookResponse(GuestbookResponse gbr){
        this.guestbookResponse = gbr;
    }
    
    
}