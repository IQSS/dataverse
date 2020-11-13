/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.util.List;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author skraffmi
 */
@Stateless
@Named
public class BannerMessageServiceBean implements java.io.Serializable {
    private static final Logger logger = Logger.getLogger(BannerMessageServiceBean.class.getCanonicalName());

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    
    public List<BannerMessage> findBannerMessages() {
        return em.createQuery("select object(o) from BannerMessage as o where  o.dismissibleByUser = 'false'", BannerMessage.class)
                .getResultList();
    }
    
    public List<BannerMessage> findBannerMessages(Long auId) {
        return em.createQuery("select object(o) from BannerMessage as o where  o.dismissibleByUser = 'false'"
                + " or o.id not in (select ubm.id from UserBannerMessage as ubm where ubm.user.id  =:authenticatedUserId)", BannerMessage.class)
                .setParameter("authenticatedUserId", auId)
                .getResultList();
    }
    
}
