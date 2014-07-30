/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

/**
 *
 * @author gdurand
 */
@Stateless
@Named
public class DataverseServiceBean {

    private static final Logger logger = Logger.getLogger(DataverseServiceBean.class.getCanonicalName());
    @EJB
    IndexServiceBean indexService;

    @Inject
    DataverseSession session;

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    public Dataverse save(Dataverse dataverse) {
        Dataverse savedDataverse = em.merge(dataverse);
        String indexingResult = indexService.indexDataverse(savedDataverse);
        logger.info("during dataverse save, indexing result was: " + indexingResult);
        return savedDataverse;
    }

    public Dataverse find(Object pk) {
        return (Dataverse) em.find(Dataverse.class, pk);
    }

    public List<Dataverse> findAll() {
        return em.createQuery("select object(o) from Dataverse as o order by o.name").getResultList();
    }

    public List<Dataverse> findByOwnerId(Long ownerId) {
        Query query = em.createQuery("select object(o) from Dataverse as o where o.owner.id =:ownerId order by o.name");
        query.setParameter("ownerId", ownerId);
        return query.getResultList();
    }

    public Dataverse findRootDataverse() {
        return (Dataverse) em.createQuery("select object(o) from Dataverse as o where o.owner.id = null").getSingleResult();
    }
    
    public List<Dataverse> findAllPublishedByOwnerId(Long ownerId) {
        List<Dataverse> retVal = new ArrayList();
        boolean keepGoing = false;
        List<Dataverse> previousLevel = findByOwnerId(ownerId);

        if (!previousLevel.isEmpty()) {
            keepGoing = true;
            for (Dataverse dvf : previousLevel) {
                if (dvf.isReleased()) {
                    retVal.add(dvf);
                }
            }
        }

        while (keepGoing) {
            List<Dataverse> thisLevel = new ArrayList();
            for (Dataverse dv : previousLevel) {
                List<Dataverse> children = findByOwnerId(dv.getId());
                for (Dataverse dvl : children) {
                    if (dvl.isReleased()) {
                        thisLevel.add(dvl);
                        retVal.add(dvl);
                    }
                }
            }
            if (thisLevel.isEmpty()) {
                keepGoing = false;
            }
            previousLevel = thisLevel;
        }
        return retVal;
    }

    public Dataverse findByAlias(String anAlias) {
        try {
            return (anAlias.equals(":root"))
				? findRootDataverse()
				: em.createQuery("select d from Dataverse d WHERE d.alias=:alias", Dataverse.class)
					.setParameter("alias", anAlias)
					.getSingleResult();
        } catch ( NoResultException nre ) {
            return null;
        }
    }
	
	public boolean hasData( Dataverse dv ) {
		TypedQuery<Long> amountQry = em.createNamedQuery("Dataverse.ownedObjectsById", Long.class)
								.setParameter("id", dv.getId());
		
		return (amountQry.getSingleResult()>0);
	}
	
    public boolean isRootDataverseExists() {
        long count = em.createQuery("SELECT count(dv) FROM Dataverse dv WHERE dv.owner.id=null", Long.class).getSingleResult();
        return (count == 1);
    }

    public String determineDataversePath(Dataverse dataverse) {
        List<String> dataversePathSegments = new ArrayList();
        indexService.findPathSegments(dataverse, dataversePathSegments);
        StringBuilder dataversePath = new StringBuilder();
        for (String segment : dataversePathSegments) {
            dataversePath.append("/" + segment);
        }
        return dataversePath.toString();
    }

    public MetadataBlock findMDB(Long id) {
        return (MetadataBlock) em.find(MetadataBlock.class, id);
    }

    public MetadataBlock findMDBByName(String name) {
        return em.createQuery("select m from MetadataBlock m WHERE m.name=:name", MetadataBlock.class)
                .setParameter("name", name)
                .getSingleResult();
    }

    public List<MetadataBlock> findAllMetadataBlocks() {
        return em.createQuery("select object(o) from MetadataBlock as o order by o.id").getResultList();
    }
    
    public DataverseFacet findFacet(Long id) {
        return (DataverseFacet) em.find(DataverseFacet.class, id);
    }
    
    public List<DataverseFacet> findAllDataverseFacets() {
        return em.createQuery("select object(o) from DataverseFacet as o order by o.display").getResultList();
    }  
}  
