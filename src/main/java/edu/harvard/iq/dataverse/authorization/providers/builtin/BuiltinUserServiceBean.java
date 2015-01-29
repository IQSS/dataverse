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
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
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
        if ( dataverseUser.getId() == null ) {
            // see that the username is unique
            if ( em.createNamedQuery("BuiltinUser.findByUserName")
                    .setParameter("userName", dataverseUser.getUserName()).getResultList().size() > 0 ) {
                throw new IllegalArgumentException( "BuiltinUser with username '" + dataverseUser.getUserName() + "' already exists.");
            }
            em.persist( dataverseUser );
            return dataverseUser;
        } else {
            return em.merge(dataverseUser);
        }
    }
    
    public User findByIdentifier( String idtf ) {
        return null; // TODO implement
    }
	
    public BuiltinUser find(Long pk) {
        return em.find(BuiltinUser.class, pk);
    }    
    
    public BuiltinUser findByUserName(String userName) {
        try {
            return em.createNamedQuery("BuiltinUser.findByUserName", BuiltinUser.class)
                    .setParameter("userName", userName)
                    .getSingleResult();
        } catch (javax.persistence.NoResultException e) {
            return null;
        }
    }
	
	public List<BuiltinUser> listByUsernamePart ( String part ) {
		return em.createNamedQuery("BuiltinUser.listByUserNameLike", BuiltinUser.class)
				.setParameter("userNameLike", "%" + part + "%")
				.getResultList();
	}

    /**
     * @param email email of the user.
     * @return A {@link BuiltinUser} or null if not found
     */
    public BuiltinUser findByEmail(String email) {
        try {
            return em.createNamedQuery("BuiltinUser.findByEmail", BuiltinUser.class)
                    .setParameter("email", email)
                    .getSingleResult();
        } catch (NoResultException | NonUniqueResultException ex) {
            return null;
        }
    }
    
    public List<BuiltinUser> findAll() {
		return em.createNamedQuery("BuiltinUser.findAll", BuiltinUser.class).getResultList();
	}
}
