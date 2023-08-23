package edu.harvard.iq.dataverse.actionlogging;

import java.util.Date;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

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

    //Switches all actions from one identifier to another identifier, via native query
    //This is needed for when we change a userIdentifier or merge one account into another
    public void changeUserIdentifierInHistory(String oldIdentifier, String newIdentifier) {
        em.createNativeQuery(
                "UPDATE actionlogrecord "
                        + "SET useridentifier='"+newIdentifier+"', "
                        + "info='orig from "+oldIdentifier+" | ' || info "
                        + "WHERE useridentifier='"+oldIdentifier+"'"
        ).executeUpdate();
    }
   

    
}
