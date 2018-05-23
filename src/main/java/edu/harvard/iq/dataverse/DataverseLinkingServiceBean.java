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
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

/**
 *
 * @author skraffmiller
 */
@Stateless
@Named
public class DataverseLinkingServiceBean implements java.io.Serializable {
    private static final Logger logger = Logger.getLogger(DataverseLinkingServiceBean.class.getCanonicalName());

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    
    @EJB
    DataverseServiceBean dataverseService;
    
    
    public List<Dataverse> findLinkedDataverses(Long linkingDataverseId) {
        List<Dataverse> retList = new ArrayList<>();
        Query query = em.createQuery("select object(o) from DataverseLinkingDataverse as o where o.linkingDataverse.id =:linkingDataverseId order by o.id");
        query.setParameter("linkingDataverseId", linkingDataverseId);
        for (Object o : query.getResultList()) {
            DataverseLinkingDataverse convterted = (DataverseLinkingDataverse) o;
            retList.add(convterted.getDataverse());
        }
        return retList;
    }

    public List<Dataverse> findLinkingDataverses(Long dataverseId) {
        List<Dataverse> retList = new ArrayList<>();
        Query query = em.createQuery("select object(o) from DataverseLinkingDataverse as o where o.dataverse.id =:dataverseId order by o.id");
        query.setParameter("dataverseId", dataverseId);
        for (Object o : query.getResultList()) {
            DataverseLinkingDataverse convterted = (DataverseLinkingDataverse) o;
            retList.add(convterted.getLinkingDataverse());
        }
        return retList;
    }
    
    public void save(DataverseLinkingDataverse dataverseLinkingDataverse) {
        if (dataverseLinkingDataverse.getId() == null) {
            em.persist(dataverseLinkingDataverse);
        } else {
            em.merge(dataverseLinkingDataverse);
        }
    }
    
    public DataverseLinkingDataverse findDataverseLinkingDataverse(Long dataverseId, Long linkingDataverseId) {
        DataverseLinkingDataverse foundDataverseLinkingDataverse = null;
        try {
            foundDataverseLinkingDataverse = em.createQuery("SELECT OBJECT(o) FROM DataverseLinkingDataverse AS o WHERE o.linkingDataverse.id = :linkingDataverseId AND o.dataverse.id = :dataverseId", DataverseLinkingDataverse.class)
                    .setParameter("dataverseId", dataverseId)
                    .setParameter("linkingDataverseId", linkingDataverseId)
                    .getSingleResult();
        } catch (javax.persistence.NoResultException e) {
            logger.fine("No DataverseLinkingDataverse found for dataverseId " + dataverseId + " and linkedDataverseId " + linkingDataverseId);        
        }
        return foundDataverseLinkingDataverse;
    }

    public boolean alreadyLinked(Dataverse definitionPoint, Dataverse dataverseToLinkTo) {
        TypedQuery<DataverseLinkingDataverse> typedQuery = em.createQuery("SELECT OBJECT(o) FROM DataverseLinkingDataverse AS o WHERE o.linkingDataverse.id = :dataverseId AND o.dataverse.id = :dataverseToLinkTo", DataverseLinkingDataverse.class);
        typedQuery.setParameter("dataverseId", definitionPoint.getId());
        typedQuery.setParameter("dataverseToLinkTo", dataverseToLinkTo.getId());
        return !typedQuery.getResultList().isEmpty();
    }
}
