package edu.harvard.iq.dataverse.dataset;

import edu.harvard.iq.dataverse.actionlogging.ActionLogRecord;
import edu.harvard.iq.dataverse.api.AbstractApiBean;
import jakarta.ejb.Stateless;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@Named
public class DatasetTypeServiceBean {

    private static final Logger logger = Logger.getLogger(DatasetTypeServiceBean.class.getName());

    @PersistenceContext
    EntityManager em;

    public List<DatasetType> listAll() {
        return em.createNamedQuery("DatasetType.findAll", DatasetType.class).getResultList();
    }

    public DatasetType getById(long id) {
        try {
            return em.createNamedQuery("DatasetType.findById", DatasetType.class)
                    .setParameter("id", id)
                    .getSingleResult();
        } catch (NoResultException noResultException) {
            logger.log(Level.WARNING, "Couldn't find a dataset type with id " + id);
            return null;
        }
    }

    public DatasetType getByName(String name) {
        try {
            return em.createNamedQuery("DatasetType.findByName", DatasetType.class)
                    .setParameter("name", name)
                    .getSingleResult();
        } catch (NoResultException noResultException) {
            logger.log(Level.WARNING, "Couldn't find a dataset type named " + name);
            return null;
        }
    }

    public DatasetType save(DatasetType datasetType) throws AbstractApiBean.WrappedResponse {
        if (datasetType.getId() != null) {
            throw new AbstractApiBean.WrappedResponse(new IllegalArgumentException("There shouldn't be an ID in the request body"), null);
        }
        try {
            em.persist(datasetType);
            em.flush();
        } catch (PersistenceException p) {
            if (p.getMessage().contains("duplicate key")) {
                throw new AbstractApiBean.WrappedResponse(new IllegalStateException("A dataset type with the same name is already present.", p), null);
            } else {
                throw p;
            }
        }
        return datasetType;
    }

    public int deleteById(long id) throws AbstractApiBean.WrappedResponse {
        try {
            return em.createNamedQuery("DatasetType.deleteById").setParameter("id", id).executeUpdate();
        } catch (PersistenceException p) {
            if (p.getMessage().contains("violates foreign key constraint")) {
                throw new AbstractApiBean.WrappedResponse(new IllegalStateException("Dataset type with id " + id + " is referenced and cannot be deleted.", p), null);
            } else {
                throw p;
            }
        }
    }

}
