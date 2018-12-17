/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.dataverse.messages;

import javax.ejb.Stateless;
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

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    public void deactivateAllowMessagesAndBanners(Long dataverseId) {
        logger.info("Deactivating text messages for dataverse: " + dataverseId);
        em.createNativeQuery("update dataversetextmessage set active = false where dataverse_id = ?")
                .setParameter(1, dataverseId)
                .executeUpdate();
    }

}
