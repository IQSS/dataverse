package edu.harvard.iq.dataverse.workflow;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * A workflow whose current step waits for an external system to complete a
 * (probably lengthy) process. Meanwhile, it sits in the database, pending.
 * 
 * @author michael
 */
@Entity
class PendingWorkflowInvocation implements Serializable {
    
    @Id
    String invocationId;
    
    
    
}
