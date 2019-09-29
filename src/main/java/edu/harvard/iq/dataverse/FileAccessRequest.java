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

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "datafile_id")
    private DataFile dataFile;
    
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "authenticated_user_id")
    private AuthenticatedUser user;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "guestbookresponse_id")
    private GuestbookResponse guestbookResponse;
    
    public FileAccessRequest(){
        
    }
    
    public FileAccessRequest(DataFile df, AuthenticatedUser au){
        setDataFile(df);
        setRequester(au);
    }
    
    public FileAccessRequest(DataFile df, AuthenticatedUser au, GuestbookResponse gbr){
        setDataFile(df);
        setRequester(au);
        setGuestbookResponse(gbr);
    }
    
    public DataFile getDataFile(){
        return dataFile;
    }
    
    public void setDataFile(DataFile df){
        this.dataFile = df;
    }
    
    public AuthenticatedUser getRequester(){
        return user;
    }
    
    public void setRequester(AuthenticatedUser au){
        this.user = au;
    }
    
    public GuestbookResponse getGuestbookResponse(){
        return guestbookResponse;
    }
    
    public void setGuestbookResponse(GuestbookResponse gbr){
        this.guestbookResponse = gbr;
    }
    
    
}