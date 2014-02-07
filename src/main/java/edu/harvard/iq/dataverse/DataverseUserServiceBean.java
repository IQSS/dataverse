/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import java.util.List;
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
	
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    
	private static final String GUEST_USERNAME = "__GUEST__";
	
    public String encryptPassword(String plainText) {
        return PasswordEncryption.getInstance().encrypt(plainText);
    }
       
    public DataverseUser save(DataverseUser dataverseUser) {
         return em.merge(dataverseUser);
    }
    
	public DataverseUser findGuestUser() {
		return findByUserName(GUEST_USERNAME);
	}
	
	public DataverseUser createGuestUser() {
		DataverseUser guest = new DataverseUser();
		guest.setUserName(GUEST_USERNAME);
		guest.setEmail("hello@world.com");
		guest.setFirstName("Guest");
		guest.setLastName("Guest");
		return save(guest);
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

    public DataverseUser findByEmail(String email) {
        String query = "SELECT u from DataverseUser u where u.email = :email ";
        DataverseUser user = null;
        try {
            user = (DataverseUser) em.createQuery(query).setParameter("email", email).getSingleResult();
        } catch (javax.persistence.NoResultException e) {
        }
        return user;
    }
    
    public DataverseUser findDataverseUser() {
        return (DataverseUser) em.createQuery("select object(o) from DataverseUser as o where o.id = 1").getSingleResult();
    }
	
	public List<DataverseUser> findAll() {
		return em.createNamedQuery("DataverseUser.findAll", DataverseUser.class).getResultList();
	}
}
