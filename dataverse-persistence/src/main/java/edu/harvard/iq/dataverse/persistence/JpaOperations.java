package edu.harvard.iq.dataverse.persistence;

import javax.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;

public interface JpaOperations<ID, T extends JpaEntity<ID>> {

    List<T> findAll();

    Long countAll();
    
    Optional<T> findById(ID id);

    default T getById(ID id) {
        return findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Entity with ID " + id + " not found"));
    }

    T save(T entity);

    default T saveAndFlush(T entity) {
        return save(entity);
    }

    default T saveFlushAndClear(T entity) {
        return save(entity);
    }

    void deleteById(ID id);

    default void delete(T entity) {
        deleteById(entity.getId());
    }
}
