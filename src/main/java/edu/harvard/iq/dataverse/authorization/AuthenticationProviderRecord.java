package edu.harvard.iq.dataverse.authorization;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * Persistent view of an {@link AuthenticationProvider}. Used by {@link AuthenticationProviderFactory}s 
 * to actually generate the providers.
 * 
 * @author michael
 */
@Entity
class AuthenticationProviderRecord implements java.io.Serializable {
    
    @Id
    @GeneratedValue()
    private Long id;
}
