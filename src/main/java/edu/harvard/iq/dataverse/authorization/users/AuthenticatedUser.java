package edu.harvard.iq.dataverse.authorization.users;

import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.DatasetLock;
import edu.harvard.iq.dataverse.DatasetVersionUser;
import edu.harvard.iq.dataverse.authorization.RoleAssigneeDisplayInfo;
import java.io.Serializable;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.validation.constraints.NotNull;

@NamedQueries({
    @NamedQuery( name="AuthenticatedUser.findAll",
                query="select au from AuthenticatedUser au")
})
@Entity
public class AuthenticatedUser implements User, Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    /**
     * In practice, this identifier is a username + idp prefix.
     */
    @NotNull
    @Column(nullable = false)
    private String userIdentifier;

    private String name;
    private String email;
    
    @Override
    public String getIdentifier() {
        return userIdentifier;
    }
    
    @OneToMany(mappedBy = "user", cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private List<DatasetLock> datasetLocks;
	
    public List<DatasetLock> getDatasetLocks() {
        return datasetLocks;
    }

    public void setDatasetLocks(List<DatasetLock> datasetLocks) {
        this.datasetLocks = datasetLocks;
    }
    
    @Override
    public RoleAssigneeDisplayInfo getDisplayInfo() {
        return new RoleAssigneeDisplayInfo(name, email);
    }
    
    @Override
    public boolean isAuthenticated() { return true; }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    
    
}
