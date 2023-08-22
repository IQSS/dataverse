package edu.harvard.iq.dataverse.privateurl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.authorization.users.PrivateUrlUser;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.io.Serializable;
import java.util.logging.Logger;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

/**
 *
 * PrivateUrlServiceBean depends on Glassfish and Postgres being available and
 * it is tested with API tests in DatasetIT. Code that can execute without any
 * runtime dependencies should be put in PrivateUrlUtil so it can be unit
 * tested.
 */
@Stateless
@Named
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
     * @return DatasetVersion if it can be found using the token or null.
     */
    public DatasetVersion getDraftDatasetVersionFromToken(String token) {
        return PrivateUrlUtil.getDraftDatasetVersionFromRoleAssignment(getRoleAssignmentFromPrivateUrlToken(token));
    }

    /**
     * @return A RoleAssignment or null.
     *
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
     *
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
