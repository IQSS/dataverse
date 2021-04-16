package edu.harvard.iq.dataverse.engine;

/**
 * A test entity manager that overrides commonly called methods with a no-op.
 * @author michael
 */
public class NoOpTestEntityManager extends TestEntityManager {

    @Override
    public <T> T merge(T entity) {
        return entity;
    }

    @Override
    public void persist(Object entity) {
        //
    }

    @Override
    public void flush() {
        //nothing to do here
    }

}
