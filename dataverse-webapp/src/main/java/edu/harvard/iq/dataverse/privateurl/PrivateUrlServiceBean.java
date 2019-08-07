package edu.harvard.iq.dataverse.privateurl;

import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.user.PrivateUrlUser;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignment;
import edu.harvard.iq.dataverse.util.SystemConfig;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.io.Serializable;
import java.util.logging.Logger;

/**
 * PrivateUrlServiceBean depends on Glassfish and Postgres being available and
 * it is tested with API tests in DatasetIT. Code that can execute without any
 * runtime dependencies should be put in PrivateUrlUtil so it can be unit
 * tested.
 */
@Stateless
public class PrivateUrlServiceBean implements Serializable {

    private static final Logger logger = Logger.getLogger(PrivateUrlServiceBean.class.getCanonicalName());

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    @EJB
    DatasetServiceBean datasetServiceBean;

    @EJB
    SystemConfig systemConfig;

    /**
     * @return A PrivateUrl if the dataset has one or null.
     */
    public PrivateUrl getPrivateUrlFromDatasetId(long datasetId) {
        RoleAssignment roleAssignment = getPrivateUrlRoleAssignmentFromDataset(datasetServiceBean.find(datasetId));
        return PrivateUrlUtil.getPrivateUrlFromRoleAssignment(roleAssignment, systemConfig.getDataverseSiteUrl());
    }

    /**
     * @return A PrivateUrlUser if one can be found using the token or null.
     */
    public PrivateUrlUser getPrivateUrlUserFromToken(String token) {
        return PrivateUrlUtil.getPrivateUrlUserFromRoleAssignment(getRoleAssignmentFromPrivateUrlToken(token));
    }

    /**
     * @return PrivateUrlRedirectData if it can be found using the token or
     * null.
     */
    public PrivateUrlRedirectData getPrivateUrlRedirectDataFromToken(String token) {
        return PrivateUrlUtil.getPrivateUrlRedirectData(getRoleAssignmentFromPrivateUrlToken(token));
    }

    /**
     * @return A RoleAssignment or null.
     * @todo This might be a good place for Optional.
     */
    private RoleAssignment getRoleAssignmentFromPrivateUrlToken(String privateUrlToken) {
        if (privateUrlToken == null) {
            return null;
        }
        TypedQuery<RoleAssignment> query = em.createNamedQuery(
                "RoleAssignment.listByPrivateUrlToken",
                RoleAssignment.class);
        query.setParameter("privateUrlToken", privateUrlToken);
        try {
            RoleAssignment roleAssignment = query.getSingleResult();
            return roleAssignment;
        } catch (NoResultException | NonUniqueResultException ex) {
            return null;
        }
    }

    /**
     * @param dataset A non-null dataset;
     * @return A role assignment for a Private URL, if found, or null.
     * @todo This might be a good place for Optional.
     */
    private RoleAssignment getPrivateUrlRoleAssignmentFromDataset(Dataset dataset) {
        if (dataset == null) {
            return null;
        }
        TypedQuery<RoleAssignment> query = em.createNamedQuery(
                "RoleAssignment.listByAssigneeIdentifier_DefinitionPointId",
                RoleAssignment.class);
        PrivateUrlUser privateUrlUser = new PrivateUrlUser(dataset.getId());
        query.setParameter("assigneeIdentifier", privateUrlUser.getIdentifier());
        query.setParameter("definitionPointId", dataset.getId());
        try {
            return query.getSingleResult();
        } catch (NoResultException | NonUniqueResultException ex) {
            return null;
        }
    }

}
