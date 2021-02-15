package edu.harvard.iq.dataverse.actionlogging;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import edu.harvard.iq.dataverse.persistence.ActionLogRecord;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * A service bean that persists {@link ActionLogRecord}s to the DB.
 *
 * @author michael
 */
@Stateless
public class ActionLogServiceBean {

    private static final Logger logger = LoggerFactory.getLogger(ActionLogServiceBean.class);

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    @Inject
    private SystemConfig systemConfig;

    /**
     * Log the record. Set default values.
     *
     * @param rec
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void log(ActionLogRecord rec) {
        if (rec.getEndTime() == null) {
            rec.setEndTime(new Date());
        }
        if (rec.getActionResult() == null) {
            rec.setActionResult(ActionLogRecord.Result.OK);
        }
        
        if (systemConfig.isReadonlyMode()) {
            logger.info(rec.toString());
        } else {
            em.persist(rec);
        }
    }

}
