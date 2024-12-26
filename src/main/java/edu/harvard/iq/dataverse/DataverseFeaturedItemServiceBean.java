package edu.harvard.iq.dataverse;

import jakarta.ejb.Stateless;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.io.Serializable;

@Stateless
@Named
public class DataverseFeaturedItemServiceBean implements Serializable {

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    public DataverseFeaturedItem save(DataverseFeaturedItem dataverseFeaturedItem) {
        if (dataverseFeaturedItem.getId() == null) {
            em.persist(dataverseFeaturedItem);
            return dataverseFeaturedItem;
        } else {
            return em.merge(dataverseFeaturedItem);
        }
    }
}
