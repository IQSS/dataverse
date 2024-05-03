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
package edu.harvard.iq.dataverse.harvest.server;

import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 *
 * @author Leonid Andreev
 * based on the DVN 3 implementation, 
 * @author Ellen Kraffmiller 
 * 
 */
@Entity
public class OAISet implements Serializable {

    private static final long serialVersionUID = 1L;
    
    public OAISet() {
    } 
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    
    @Column(columnDefinition="TEXT")
    private String name;
    @Column(columnDefinition="TEXT", nullable = false, unique=true)
    @Size(max = 30, message = "{setspec.maxLength}")
    @Pattern.List({@Pattern(regexp = "[a-zA-Z0-9\\_\\-]*", message = "{dataverse.nameIllegalCharacters}")})
    //    @Pattern(regexp=".*\\D.*", message="{setspec.notNumber}")})
    private String spec;
    
    @Column(columnDefinition="TEXT", nullable = false)
    private String definition;
   
    @Column(columnDefinition="TEXT", nullable = false)
    private String description;

    @Version
    private Long version;
    
    private boolean updateInProgress;
    
    private boolean deleted;

    public boolean isUpdateInProgress() {
        return this.updateInProgress;
    }

    public void setUpdateInProgress(boolean updateInProgress) {
        this.updateInProgress = updateInProgress; 
    }
    
    public boolean isDeleteInProgress() {
        return this.deleted;
    }

    public void setDeleteInProgress(boolean deleteInProgress) {
        this.deleted = deleteInProgress; 
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSpec() {
        return spec;
    }

    public void setSpec(String spec) {
        this.spec = spec;
    }

    public String getDefinition() {
        return definition;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    
    public Long getVersion() {
        return version;
    }
    
    public void setVersion(Long version) {
        this.version = version;
    }
    
    public boolean isDefaultSet() {
        return "".equals(this.spec);
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
        if (!(object instanceof OAISet)) {
            return false;
        }
        OAISet other = (OAISet) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "edu.harvard.iq.dataverse.harvest.server.OaiSet[ id=" + id + " ]";
    }
    
}
