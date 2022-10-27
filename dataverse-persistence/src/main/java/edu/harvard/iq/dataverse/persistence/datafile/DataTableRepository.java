package edu.harvard.iq.dataverse.persistence.datafile;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

import javax.ejb.Stateless;

@Stateless
public class DataTableRepository extends JpaRepository<Long, DataTable> {

    // -------------------- CONSTRUCTORS --------------------

    public DataTableRepository() {
        super(DataTable.class);
    }
}
