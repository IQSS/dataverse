package edu.harvard.iq.dataverse.persistence.datafile;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

import javax.ejb.Singleton;

@Singleton
public class DataFileRepository extends JpaRepository<Long, DataFile> {

    // -------------------- CONSTRUCTORS --------------------

    public DataFileRepository() {
        super(DataFile.class);
    }
}
