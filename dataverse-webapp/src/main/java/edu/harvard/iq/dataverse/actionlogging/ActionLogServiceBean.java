package edu.harvard.iq.dataverse.actionlogging;

import java.util.Date;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * A service bean that persists {@link ActionLogRecord}s to the DB.
 * @author michael
 */
@Stateless
public class ActionLogServiceBean {
    
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    
    /**
     * Log the record. Set default values.
     * @param rec 
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void log( ActionLogRecord rec ) {
        if ( rec.getEndTime() == null ) {
            rec.setEndTime( new Date() );
        }
        if ( rec.getActionResult() == null 
                && rec.getActionType() != ActionLogRecord.ActionType.Command ) {
            rec.setActionResult(ActionLogRecord.Result.OK);
        }
        em.persist(rec);
    }

}
