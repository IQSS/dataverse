/*
   Copyright (C) 2005-2012, by the President and Fellows of Harvard College.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

   Dataverse Network - A web application to share, preserve and analyze research data.
   Developed at the Institute for Quantitative Social Science, Harvard University.
   Version 3.0.
*/

package edu.harvard.iq.dataverse;

import static edu.harvard.iq.dataverse.DatasetLock.Reason.Workflow;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import java.util.Date;
import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

/**
 *
 * Holds the reason a dataset is locked, and possibly the user that created the lock.
 * 
 * @author Leonid Andreev
 * @author Michael Bar-Sinai
 * 
 */
@Entity
@Table(indexes = {@Index(columnList="user_id"), @Index(columnList="dataset_id")})
@NamedQueries({
    @NamedQuery(name = "DatasetLock.findAll",
            query="SELECT lock FROM DatasetLock lock ORDER BY lock.id"),
    @NamedQuery(name = "DatasetLock.getLocksByDatasetId",
            query = "SELECT lock FROM DatasetLock lock WHERE lock.dataset.id=:datasetId"),
    @NamedQuery(name = "DatasetLock.getLocksByType",
            query = "SELECT lock FROM DatasetLock lock WHERE lock.reason=:lockType ORDER BY lock.id"),
    @NamedQuery(name = "DatasetLock.getLocksByAuthenticatedUserId",
            query = "SELECT lock FROM DatasetLock lock WHERE lock.user.id=:authenticatedUserId  ORDER BY lock.id"),
    @NamedQuery(name = "DatasetLock.getLocksByTypeAndAuthenticatedUserId",
            query = "SELECT lock FROM DatasetLock lock WHERE lock.reason=:lockType AND lock.user.id=:authenticatedUserId  ORDER BY lock.id")
}
)
public class DatasetLock implements Serializable {
    
    public enum Reason {
        /** Data being ingested *//** Data being ingested */
        Ingest,
        
        /** Waits for a {@link Workflow} to end */
        Workflow,
        
        /** Waiting for a curator to approve/send back to author */
        InReview, 
        
        /** DCM (rsync) upload in progress */
        DcmUpload,

        /** Globus upload in progress */
        GlobusUpload,

        /** Tasks handled by FinalizeDatasetPublicationCommand:
         Registering PIDs for DS and DFs and/or file validation */
        finalizePublication,
        
        /*Another edit is in progress*/
        EditInProgress,
        
        /* Some files in the dataset failed validation */
        FileValidationFailed
        
    }
    
    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date startTime;    

    @ManyToOne
    @JoinColumn(nullable=false)
    private Dataset dataset;

    @ManyToOne
    @JoinColumn(nullable=false)
    private AuthenticatedUser user;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private Reason reason;
    
    private String info;

     /**
     * Constructing a lock for the given reason.
     * @param aReason Why the dataset gets locked.  Cannot be {@code null}.
     * @param aUser The user causing the lock. Cannot be {@code null}.
     * @throws IllegalArgumentException if any of the parameters are null. That's
     *         because JPA would throw an exception later anyway.
     */
    public DatasetLock( Reason aReason, AuthenticatedUser aUser ) {
        this(aReason, aUser, null);
    }
    
    /**
     * Constructing a lock for the given reason, with the specified descriptive info message.
     * @param aReason Why the dataset gets locked.  Cannot be {@code null}.
     * @param aUser The user causing the lock. Cannot be {@code null}.
     * @param infoMessage Descriptive message.
     * @throws IllegalArgumentException if any of the parameters are null. That's
     *         because JPA would throw an exception later anyway.
     */
    public DatasetLock(Reason aReason, AuthenticatedUser aUser, String infoMessage) {
        if ( aReason == null ) throw new IllegalArgumentException("Cannot lock a dataset for a null reason");
        if ( aUser == null ) throw new IllegalArgumentException("Cannot lock a dataset for a null user");
        reason = aReason;
        startTime = new Date();
        user = aUser;
        info = infoMessage;
     
    }
    
    /**
     * JPA no-args constructor. Client code should use the public constructor
     * and not this one.
     * 
     * @see #DatasetLock(edu.harvard.iq.dataverse.DatasetLock.Reason) 
     */
    protected DatasetLock(){}
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }
    
    public Dataset getDataset() {
        return dataset;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }
    
    public AuthenticatedUser getUser() {
        return user;
    }

    public void setUser(AuthenticatedUser user) {
        this.user = user;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public Reason getReason() {
        return reason;
    }

    public void setReason(Reason reason) {
        this.reason = reason;
    }
    
    @Override
    public int hashCode() {
        return (id != null ? id.hashCode() : 0);
    }

    @Override
    public boolean equals(Object object) {
        if ( object == null ) return false;
        if ( object == this ) return true;
        
        if (!(object instanceof DatasetLock)) {
            return false;
        }
        DatasetLock other = (DatasetLock) object;
        
        return (id==null && other.id==null) || (id!=null && id.equals(other.getId()));
    }

    @Override
    public String toString() {
        return "edu.harvard.iq.dataverse.DatasetLock[ id=" + id + " ]";
    }
    
}
