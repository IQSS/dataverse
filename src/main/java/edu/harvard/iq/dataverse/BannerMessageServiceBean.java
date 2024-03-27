/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import jakarta.ejb.Stateless;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

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
        return em.createQuery("select object(o) from BannerMessage as o where o.active = 'true' and o.dismissibleByUser = 'false'", BannerMessage.class)
                .getResultList();
    }
    
    public List<BannerMessage> findBannerMessages(Long auId) {
        return em.createQuery("select object(o) from BannerMessage as o where (o.active = 'true' and  o.dismissibleByUser = 'false') "
                + " or (o.active = 'true' and o.dismissibleByUser = 'true' and o.id not in (select ubm.bannerMessage.id from UserBannerMessage as ubm where ubm.user.id  =:authenticatedUserId))", BannerMessage.class)
                .setParameter("authenticatedUserId", auId)
                .getResultList();
    }
    
    public List<BannerMessage> findAllBannerMessages() {
        return em.createQuery("select o from BannerMessage o where o.active = 'true' ")
                .getResultList();
    }
    
    public void save( BannerMessage message ) {
        em.persist(message);
    }
    
    public void deleteBannerMessage(Object pk) {
        BannerMessage message = em.find(BannerMessage.class, pk);

        if (message != null) { 
            em.remove(message);
        }
    }
            
    public void deactivateBannerMessage(Object pk) {
        BannerMessage message = em.find(BannerMessage.class, pk);

        if (message != null) { 
            message.setActive(false);
            em.merge(message);
        }
    }
    
    public void dismissMessageByUser(BannerMessage message, AuthenticatedUser user) {

        UserBannerMessage ubm = new UserBannerMessage();
        ubm.setUser(user);
        ubm.setBannerMessage(message);
        ubm.setBannerDismissalTime(new Timestamp(new Date().getTime()));
        em.persist(ubm);
        em.flush();

    }
    
}
