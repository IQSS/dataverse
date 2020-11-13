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
                + " or (o.dismissibleByUser = 'true' and o.id not in (select ubm.bannerMessage.id from UserBannerMessage as ubm where ubm.user.id  =:authenticatedUserId))", BannerMessage.class)
                .setParameter("authenticatedUserId", auId)
                .getResultList();
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
