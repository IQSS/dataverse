package edu.harvard.iq.dataverse.persistence.dataset;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

import javax.ejb.Singleton;
import java.util.Optional;

@Singleton
public class DatasetFieldTypeRepository extends JpaRepository<Long, DatasetFieldType> {

    // -------------------- CONSTRUCTORS --------------------

    public DatasetFieldTypeRepository() {
        super(DatasetFieldType.class);
    }

    // -------------------- LOGIC --------------------

    public Optional<DatasetFieldType> findByName(String name) {
        return getSingleResult(em.createQuery(
                        "select t from DatasetFieldType t where t.name= :name",
                        DatasetFieldType.class)
                .setParameter("name", name));
    }
}
