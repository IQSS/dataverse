/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.authorization.providers.builtin;

import edu.harvard.iq.dataverse.IndexServiceBean;
import edu.harvard.iq.dataverse.PasswordEncryption;
import edu.harvard.iq.dataverse.authorization.users.User;
import java.util.List;
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
public class BuiltinUserServiceBean {

    @EJB IndexServiceBean indexService;

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    
    public String encryptPassword(String plainText) {
        return PasswordEncryption.getInstance().encrypt(plainText);
    }
       
    public BuiltinUser save(BuiltinUser dataverseUser) {
        BuiltinUser savedUser = em.merge(dataverseUser);
        return savedUser;
    }
    
    public User findByIdentifier( String idtf ) {
        return null; // TODO implement
    }
	
    public BuiltinUser find(Long pk) {
        return em.find(BuiltinUser.class, pk);
    }    
    
    public BuiltinUser findByUserName(String userName) {
        String query = "SELECT u from DataverseUser u where u.userName = :userName ";
        BuiltinUser user = null;
        try {
            user = (BuiltinUser) em.createQuery(query).setParameter("userName", userName).getSingleResult();
        } catch (javax.persistence.NoResultException e) {
        }
        return user;
    }
	
	public List<BuiltinUser> listByUsernamePart ( String part ) {
		return em.createNamedQuery("DataverseUser.listByUserNameLike", BuiltinUser.class)
				.setParameter("userNameLike", "%" + part + "%")
				.getResultList();
	}
	
    public BuiltinUser findByEmail(String email) {
        String query = "SELECT u from DataverseUser u where u.email = :email ";
        BuiltinUser user = null;
        try {
            user = (BuiltinUser) em.createQuery(query).setParameter("email", email).getSingleResult();
        } catch (javax.persistence.NoResultException e) {
        }
        return user;
    }
    
    public List<BuiltinUser> findAll() {
		return em.createNamedQuery("DataverseUser.findAll", BuiltinUser.class).getResultList();
	}
}
