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
import javax.persistence.TypedQuery;

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
        try {
            return em.createNamedQuery("BuiltinUser.findByUserName", BuiltinUser.class)
                    .setParameter("userName", userName)
                    .getSingleResult();
        } catch (javax.persistence.NoResultException e) {
            return null;
        }
    }
	
	public List<BuiltinUser> listByUsernamePart ( String part ) {
		return em.createNamedQuery("DataverseUser.listByUserNameLike", BuiltinUser.class)
				.setParameter("userNameLike", "%" + part + "%")
				.getResultList();
	}

    /**
     * @return A {@link BuiltinUser} or null if not found
     */
    public BuiltinUser findByEmail(String email) {
        TypedQuery<BuiltinUser> typedQuery = em.createQuery("SELECT OBJECT(o) FROM BuiltinUser o WHERE o.email = :email", BuiltinUser.class);
        typedQuery.setParameter("email", email);
        try {
            BuiltinUser builtinUser = typedQuery.getSingleResult();
            return builtinUser;
        } catch (NoResultException | NonUniqueResultException ex) {
            return null;
        }
    }
    
    public List<BuiltinUser> findAll() {
		return em.createNamedQuery("DataverseUser.findAll", BuiltinUser.class).getResultList();
	}
}
