/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.User;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author xyang
 */
@Stateless
@Named
public class DataverseUserServiceBean {

    private static final Logger logger = Logger.getLogger(DataverseUserServiceBean.class.getCanonicalName());

    @EJB IndexServiceBean indexService;

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    
    public String encryptPassword(String plainText) {
        return PasswordEncryption.getInstance().encrypt(plainText);
    }
       
    public DataverseUser save(DataverseUser dataverseUser) {
        DataverseUser savedUser = em.merge(dataverseUser);
        return savedUser;
    }
    
    public User findByIdentifier( String idtf ) {
        return null; // TODO implement
    }
	
    public DataverseUser find(Long pk) {
        return em.find(DataverseUser.class, pk);
    }    
    
    public DataverseUser findByUserName(String userName) {
        String query = "SELECT u from DataverseUser u where u.userName = :userName ";
        DataverseUser user = null;
        try {
            user = (DataverseUser) em.createQuery(query).setParameter("userName", userName).getSingleResult();
        } catch (javax.persistence.NoResultException e) {
        }
        return user;
    }
	
	public List<DataverseUser> listByUsernamePart ( String part ) {
		return em.createNamedQuery("DataverseUser.listByUserNameLike", DataverseUser.class)
				.setParameter("userNameLike", "%" + part + "%")
				.getResultList();
	}
	
    public DataverseUser findByEmail(String email) {
        String query = "SELECT u from DataverseUser u where u.email = :email ";
        DataverseUser user = null;
        try {
            user = (DataverseUser) em.createQuery(query).setParameter("email", email).getSingleResult();
        } catch (javax.persistence.NoResultException e) {
        }
        return user;
    }
    
    public List<DataverseUser> findAll() {
		return em.createNamedQuery("DataverseUser.findAll", DataverseUser.class).getResultList();
	}
}
