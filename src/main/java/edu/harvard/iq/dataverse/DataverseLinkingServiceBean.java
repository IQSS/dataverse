/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

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
        TypedQuery<DataverseLinkingDataverse> typedQuery = em.createNamedQuery("DataverseLinkingDataverse.findByLinkingDataverseId", DataverseLinkingDataverse.class)
            .setParameter("linkingDataverseId", linkingDataverseId);
        for (DataverseLinkingDataverse dataverseLinkingDataverse : typedQuery.getResultList()) {
            retList.add(dataverseLinkingDataverse.getDataverse());
        }
        return retList;
    }

    public List<Dataverse> findLinkingDataverses(Long dataverseId) {
        List<Dataverse> retList = new ArrayList<>();
        TypedQuery<DataverseLinkingDataverse> typedQuery = em.createNamedQuery("DataverseLinkingDataverse.findByDataverseId", DataverseLinkingDataverse.class)
            .setParameter("dataverseId", dataverseId);
        for (DataverseLinkingDataverse dataverseLinkingDataverse : typedQuery.getResultList()) {
            retList.add(dataverseLinkingDataverse.getLinkingDataverse());
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
        try {
            return em.createNamedQuery("DataverseLinkingDataverse.findByDataverseIdAndLinkingDataverseId", DataverseLinkingDataverse.class)
                .setParameter("dataverseId", dataverseId)
                .setParameter("linkingDataverseId", linkingDataverseId)
                .getSingleResult();
        } catch (jakarta.persistence.NoResultException e) {
            logger.fine("No DataverseLinkingDataverse found for dataverseId " + dataverseId + " and linkedDataverseId " + linkingDataverseId);        
            return null;
        }
    }

    public boolean alreadyLinked(Dataverse definitionPoint, Dataverse dataverseToLinkTo) {
        return findDataverseLinkingDataverse(dataverseToLinkTo.getId(), definitionPoint.getId()) != null;
    }
}
