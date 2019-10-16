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
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import javax.persistence.GenerationType;

/**
 *
 * @author Marina
 */

@Entity
@Table(name = "fileaccessrequests", //having added the guestbookresponse_id column to fileaccessrequests
    uniqueConstraints=@UniqueConstraint(columnNames={"datafile_id", "authenticated_user_id"}) //this may not make sense at some future point
)
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