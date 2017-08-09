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

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import java.util.Date;
import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

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
@NamedQueries(
        @NamedQuery(name="DatasetLock.getLocksByDatasetId",
                    query="SELECT l FROM DatasetLock l WHERE l.dataset.id=:datasetId")
)
public class DatasetLock implements Serializable {
    
    public enum Reason {
        Ingest,
        Workflow,
        InReview
    }
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Constructing a lock for the given reason.
     * @param aReason Why the dataset gets locked.
     */
    public DatasetLock( Reason aReason ) {
        reason = aReason;
        startTime = new Date();
    }
    
    /**
     * JPA no-args constructor
     */
    protected DatasetLock(){}
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date startTime;    

    @OneToOne
    @JoinColumn(nullable=false)
    private Dataset dataset;

    @ManyToOne
    @JoinColumn(nullable=false)
    private AuthenticatedUser user;    
    
    @Enumerated(EnumType.STRING)
    private Reason reason;
    
    private String info;

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
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof DatasetLock)) {
            return false;
        }
        DatasetLock other = (DatasetLock) object;
        return !((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id)));
    }

    @Override
    public String toString() {
        return "edu.harvard.iq.dataverse.DatasetLock[ id=" + id + " ]";
    }
    
}
