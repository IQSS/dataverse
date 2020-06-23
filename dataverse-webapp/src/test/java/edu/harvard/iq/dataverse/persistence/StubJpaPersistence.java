package edu.harvard.iq.dataverse.persistence;

import org.mockito.stubbing.Answer;

import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.reflect.MethodUtils.invokeMethod;
import static org.mockito.Mockito.mock;

/**
 * Stub persistence for {@link JpaRepository} based data access.
 * <p>
 * Allows for easy creation of {@link StubJpaRepository} backed mocks for given {@link JpaEntity} types.
 * Such mocks can be used as regular ones, but have all {@link JpaOperations} by default proxied to
 * the underlying {@link StubJpaRepository}. This way we eliminate bulk of unreadable mocking of CRUD operations
 * in favour of simple map based in-memory storage.
 *
 * @author kaczynskid
 */
public class StubJpaPersistence {

    private final Map<Class<? extends JpaEntity<Long>>, StubJpaRepository<?>> repositories = new HashMap<>();

    // -------------------- LOGIC --------------------

    @SuppressWarnings("unchecked")
    public <T extends JpaEntity<Long>> StubJpaRepository<T> of(Class<T> entityType) {
        repositories.computeIfAbsent(entityType, t -> new StubJpaRepository<>());
        return (StubJpaRepository<T>) repositories.get(entityType);
    }

    public Answer<Object> answerFor(Class<? extends JpaEntity<Long>> entityType) {
        return invocation -> invokeMethod(
                of(entityType),
                invocation.getMethod().getName(),
                invocation.getArguments());
    }

    @SuppressWarnings("unchecked")
    public <T extends JpaRepository<Long, ? extends JpaEntity<Long>>> T stub(Class<T> repositoryType) {
        Class<? extends JpaEntity<Long>> entityType = (Class<? extends JpaEntity<Long>>)
                ((ParameterizedType) repositoryType.getGenericSuperclass()).getActualTypeArguments()[1];
        return mock(repositoryType, answerFor(entityType));
    }
}
