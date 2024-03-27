package edu.harvard.iq.dataverse;

import java.util.List;
import jakarta.ejb.Stateless;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;

/**
 *
 * @author michael
 */
@Stateless
@Named
public class MetadataBlockServiceBean {
    
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    
    public MetadataBlock save(MetadataBlock mdb) {
       return em.merge(mdb);
    }   
    
    
    public List<MetadataBlock> listMetadataBlocks() {
        return em.createNamedQuery("MetadataBlock.listAll", MetadataBlock.class).getResultList();
    }
    
    public MetadataBlock findById( Long id ) {
        return em.find(MetadataBlock.class, id);
    }
    
    public MetadataBlock findByName( String name ) {
        try {
            return em.createNamedQuery("MetadataBlock.findByName", MetadataBlock.class)
                        .setParameter("name", name)
                        .getSingleResult();
        } catch ( NoResultException nre ) {
            return null;
        }
    }
}
