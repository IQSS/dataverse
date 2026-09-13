package edu.harvard.iq.dataverse.datasetrelation;

import edu.harvard.iq.dataverse.api.AbstractApiBean.WrappedResponse;
import edu.harvard.iq.dataverse.api.ApiConstants;
import edu.harvard.iq.dataverse.engine.command.exception.InvalidCommandArgumentsException;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder;
import jakarta.ejb.Stateless;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.persistence.exceptions.DatabaseException;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service bean for managing dataset relation types.
 *
 * @author Vera Clemens (ZB MED)
 */
@Stateless
@Named
public class DatasetRelationTypeServiceBean {

    private static final Logger logger = Logger.getLogger(DatasetRelationTypeServiceBean.class.getName());

    @PersistenceContext
    EntityManager em;

    public List<DatasetRelationType> listAll() {
        return em.createNamedQuery("DatasetRelationType.findAll", DatasetRelationType.class).getResultList();
    }

    public DatasetRelationType findById(long id) {
        try {
            return em.createNamedQuery("DatasetRelationType.getById", DatasetRelationType.class)
                    .setParameter("id", id)
                    .getSingleResult();
        } catch (NoResultException e) {
            logger.log(Level.WARNING, "Couldn't find a dataset relation type with id " + id);
            return null;
        }
    }

    public DatasetRelationType findByName(String name) {
        if (name == null) {
            return null;
        }

        try {
            return em.createNamedQuery("DatasetRelationType.getByName", DatasetRelationType.class)
                    .setParameter("name", name)
                    .getSingleResult();
        } catch (NoResultException e) {
            logger.log(Level.WARNING, "Couldn't find a dataset relation type with name " + name);
            return null;
        }
    }

    public DatasetRelationType getDefault() {
        try {
            return em.createNamedQuery("DatasetRelationType.getDefault", DatasetRelationType.class)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public DatasetRelationType save(DatasetRelationType relationType) throws DatasetRelationTypeException {
        validate(relationType);
        relationType.setDefault(getDefault() == null);
        em.persist(relationType);
        em.flush();
        return relationType;
    }

    public void delete(DatasetRelationType doomed) throws DatasetRelationTypeException {
        if (doomed.isDefault()) {
            throw new DatasetRelationTypeException(BundleUtil.getStringFromBundle("datasets.api.datasetRelationType.error.delete.default"));
        }
        DatasetRelationType inverse = doomed.getInverse();

        if (isInUse(doomed) || (inverse != null && isInUse(inverse))) {
            throw new DatasetRelationTypeException(BundleUtil.getStringFromBundle("datasets.api.datasetRelationType.error.delete.referenced"));
        }

        if (inverse != null) {
            inverse.setInverse(null);
            doomed.setInverse(null);
            em.merge(inverse);
            em.merge(doomed);
        }
        em.remove(em.merge(doomed));
        em.flush();
    }

    public void setDefault(DatasetRelationType relationType) {
        DatasetRelationType prevDefault = getDefault();
        if (prevDefault != null) {
            prevDefault.setDefault(false);
            em.merge(prevDefault);
        }
        relationType.setDefault(true);
        em.merge(relationType);
        em.flush();
    }

    private void validate(DatasetRelationType relationType) {
        if (isBlank(relationType.getName()) || isBlank(relationType.getDisplayName())) {
            throw new DatasetRelationTypeException(BundleUtil.getStringFromBundle("datasets.api.datasetRelationType.error.create.notNull"));
        }
        if (exists(relationType.getName(), relationType.getDisplayName())) {
            throw new DatasetRelationTypeException(BundleUtil.getStringFromBundle("datasets.api.datasetRelationType.error.create.duplicate"));
        }
        DatasetRelationType inverse = relationType.getInverse();
        if (inverse != null && inverse != relationType) {
            if (isBlank(inverse.getName()) || isBlank(inverse.getDisplayName())) {
                throw new DatasetRelationTypeException(BundleUtil.getStringFromBundle("datasets.api.datasetRelationType.error.create.notNull"));
            }
            if (inverse.getName().equals(relationType.getName()) || inverse.getDisplayName().equals(relationType.getDisplayName())) {
                throw new DatasetRelationTypeException(BundleUtil.getStringFromBundle("datasets.api.datasetRelationType.error.create.duplicate"));
            }
            if (exists(inverse.getName(), inverse.getDisplayName())) {
                throw new DatasetRelationTypeException(BundleUtil.getStringFromBundle("datasets.api.datasetRelationType.error.create.duplicate"));
            }
        }
    }

    private boolean exists(String name, String displayName) {
        return em.createQuery("SELECT COUNT(drt) FROM DatasetRelationType drt "
                        + "WHERE drt.name = :name OR drt.displayName = :displayName", Long.class)
                .setParameter("name", name)
                .setParameter("displayName", displayName)
                .getSingleResult() > 0;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isInUse(DatasetRelationType drt) {
        return em.createQuery(
                        "SELECT COUNT(relation) FROM DatasetRelation relation WHERE relation.relationType = :relationType",
                        Long.class)
                .setParameter("relationType", drt)
                .getSingleResult() > 0;
    }

}
