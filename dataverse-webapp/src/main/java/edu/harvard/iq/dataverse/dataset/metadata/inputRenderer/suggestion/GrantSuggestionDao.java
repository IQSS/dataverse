package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.suggestion;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class GrantSuggestionDao {

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
}
