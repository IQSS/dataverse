package edu.harvard.iq.dataverse.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.reflect.FieldUtils.writeField;

/**
 * Simple, map backed, in-memory implementation of {@link JpaOperations}.
 *
 * @param <T> {@link JpaEntity} type.
 * @author kaczynskid
 */
public class StubJpaRepository<T extends JpaEntity<Long>> implements JpaOperations<Long, T> {

    private final AtomicLong seq = new AtomicLong();
    private final Map<Long, T> storage = new ConcurrentHashMap<>();

    // -------------------- LOGIC --------------------

    @Override
    public List<T> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public Long countAll() {
        return Long.valueOf(storage.size());
    }

    public List<T> findAll(Predicate<T> condition) {
        return storage.values().stream()
                .filter(condition)
                .collect(toList());
    }

    @Override
    public Optional<T> findById(Long id) {
        return ofNullable(storage.get(id));
    }

    public Optional<T> findOne(Predicate<T> condition) {
        return storage.values().stream()
                .filter(condition)
                .findFirst();
    }

    @Override
    public T save(T entity) {
        if (entity.isNew()) {
            try {
                Long id = seq.incrementAndGet();
                writeField(entity, "id", id, true);
                storage.put(id, entity);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Unexpected error!", e);
            }
        } else {
            storage.put(entity.getId(), entity);
        }
        return entity;
    }

    @Override
    public T refresh(T entity) {
        return storage.get(entity.getId());
    }

    @Override
    public void deleteById(Long id) {
        storage.remove(id);
    }
}
