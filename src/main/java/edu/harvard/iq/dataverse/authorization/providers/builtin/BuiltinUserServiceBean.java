package edu.harvard.iq.dataverse.authorization.providers.builtin;

import edu.harvard.iq.dataverse.search.IndexServiceBean;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.passwordreset.PasswordResetData;
import edu.harvard.iq.dataverse.passwordreset.PasswordResetException;
import edu.harvard.iq.dataverse.passwordreset.PasswordResetInitResponse;
import edu.harvard.iq.dataverse.passwordreset.PasswordResetServiceBean;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

/**
 *
 * @author xyang
 */
@Stateless
@Named
public class BuiltinUserServiceBean {

    private static final Logger logger = Logger.getLogger(BuiltinUserServiceBean.class.getCanonicalName());

    @EJB
    IndexServiceBean indexService;
    
    @EJB
    PasswordResetServiceBean passwordResetService;

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    
    public String encryptPassword(String plainText) {
        return PasswordEncryption.get().encrypt(plainText);
    }
       
    public BuiltinUser save(BuiltinUser dataverseUser) {
        /* Trim the email address no matter what the user entered or is entered
         * on their behalf in the case of Shibboleth assertions.
         *
         * @todo Why doesn't Bean Validation report that leading and trailing
         * whitespace in an email address is a problem?
         */
        dataverseUser.setEmail(dataverseUser.getEmail().trim());
        /* We throw a proper IllegalArgumentException here because otherwise
         * from the API you get a 500 response and "Can't save user: null".
         */
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        Set<ConstraintViolation<BuiltinUser>> violations = validator.validate(dataverseUser);
        if (violations.size() > 0) {
            StringBuilder sb = new StringBuilder();
            violations.stream().forEach((violation) -> {
                sb.append(" Invalid value: <<<").append(violation.getInvalidValue()).append(">>> for ").append(violation.getPropertyPath()).append(" at ").append(violation.getLeafBean()).append(" - ").append(violation.getMessage());
            });
            throw new IllegalArgumentException("BuiltinUser could not be saved to due constraint violations: " + sb);
        }
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
        } catch (NonUniqueResultException ex) {
            logger.log(Level.WARNING, "multiple accounts found for username {0}", userName);
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

    /**
     * @param usernameOrEmail Username or email address of the user.
     * @return A {@link BuiltinUser} or null if not found
     */
    public BuiltinUser findByUsernameOrEmail(String usernameOrEmail) {
        BuiltinUser userFoundByUsername = findByUserName(usernameOrEmail);
        if (userFoundByUsername != null) {
            return userFoundByUsername;
        } else {
            BuiltinUser userFoundByEmail = findByEmail(usernameOrEmail);
            if (userFoundByEmail != null) {
                return userFoundByEmail;
            } else {
                return null;
            }
        }
    }
    
    public List<BuiltinUser> findAll() {
		return em.createNamedQuery("BuiltinUser.findAll", BuiltinUser.class).getResultList();
	}
    
    public String requestPasswordUpgradeLink( BuiltinUser aUser ) throws PasswordResetException {
        PasswordResetInitResponse prir = passwordResetService.requestPasswordReset(aUser, false, PasswordResetData.Reason.UPGRADE_REQUIRED );
        return "passwordreset.xhtml?token=" + prir.getPasswordResetData().getToken() + "&faces-redirect=true";
    }
}
