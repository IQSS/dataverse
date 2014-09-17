package edu.harvard.iq.dataverse.authorization.groups;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * A single row in the database, used to store group information in an opaque way.
 * 
 * @author michael
 */
@Entity
public class GroupRow implements Serializable {
    
    @Id
    @GeneratedValue
    Long id;
    
    // TODO add common fields + blob.
}
