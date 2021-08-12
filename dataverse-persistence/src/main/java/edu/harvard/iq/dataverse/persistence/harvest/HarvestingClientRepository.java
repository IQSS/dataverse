package edu.harvard.iq.dataverse.persistence.harvest;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

import javax.ejb.Stateless;

@Stateless
public class HarvestingClientRepository extends JpaRepository<Long, HarvestingClient> {

    // -------------------- CONSTRUCTORS --------------------

    public HarvestingClientRepository() {
        super(HarvestingClient.class);
    }
}
