package edu.harvard.iq.dataverse.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import java.util.List;
import java.util.Optional;

import static java.util.Optional.ofNullable;

/**
 * Base repository (data access) class for JPA entities.
 * @param <ID> type of entity identifier.
 * @param <T> type of entity.
 *
 * @author kaczynskid
 */
public abstract class JpaRepository<ID, T extends JpaEntity<ID>> implements JpaOperations<ID, T> {

    protected static final Logger log = LoggerFactory.getLogger(JpaRepository.class);

    private final Class<T> entityClass;

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    protected EntityManager em;

    // -------------------- CONSTRUCTORS --------------------

    public JpaRepository(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    // -------------------- LOGIC --------------------

    @Override
    public List<T> findAll() {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<T> query = criteriaBuilder.createQuery(entityClass);
        return em.createQuery(query).getResultList();
    }

    @Override
    public Long countAll() {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<Long> query = criteriaBuilder.createQuery(Long.class);
        query.select(criteriaBuilder.count(query.from(entityClass)));
        
        return em.createQuery(query).getSingleResult();
    }

    @Override
    public Optional<T> findById(ID id) {
        return ofNullable(em.find(entityClass, id));
    }

    @Override
    public T getById(ID id) {
        return findById(id)
                .orElseThrow(() -> new EntityNotFoundException(entityClass.getSimpleName() + " with ID " + id + " not found"));
    }

    @Override
    public T save(T entity) {
        return save(entity, false, false);
    }

    public T saveAndFlush(T entity) {
        return save(entity, true, false);
    }

    @Override
    public T saveFlushAndClear(T entity) {
        return save(entity, true, true);
    }

    private T save(T entity, boolean flush, boolean clear) {
        T saved;
        if (entity.isNew()) {
            em.persist(entity);
            em.flush();
            saved = entity;
        } else {
            saved  = em.merge(entity);
            if (flush) {
                em.flush();
            }
        }
        if (clear) {
            em.clear();
        }
        return saved;
    }

    @Override
    public void deleteById(ID id) {
        delete(em.find(entityClass, id));
    }

    @Override
    public void delete(T entity) {
        em.remove(entity);
    }

    public void mergeAndDelete(T entity) {
        entity = em.merge(entity);
        delete(entity);
    }

    protected static <T> Optional<T> getSingleResult(TypedQuery<T> query) {
        try {
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
}
