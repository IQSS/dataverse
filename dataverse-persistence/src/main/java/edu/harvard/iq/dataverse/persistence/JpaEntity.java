package edu.harvard.iq.dataverse.persistence;

public interface JpaEntity<ID> {

    ID getId();

    default boolean isNew() {
        return getId() == null;
    }
}
