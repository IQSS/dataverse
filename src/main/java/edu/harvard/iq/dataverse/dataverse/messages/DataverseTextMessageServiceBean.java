/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.dataverse.messages;

import edu.harvard.iq.dataverse.DataverseSession;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.logging.Logger;

/**
 *
 * @author tjanek
 */
@Stateless
@Named
public class DataverseTextMessageServiceBean implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(DataverseTextMessageServiceBean.class.getCanonicalName());

    @Inject
    private DataverseSession session;

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    public void deactivateAllowMessagesAndBanners(Long dataverseId) {
        if (session.getUser().isSuperuser()) {
            logger.info("As superuser, deactivating text messages for dataverse: " + dataverseId);
            em.createNativeQuery("update dataversetextmessage set active = false where dataverse_id = ?")
                    .setParameter(1, dataverseId)
                    .executeUpdate();
        }
    }

}
