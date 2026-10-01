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

        return findLinkingDataverses(dataverseId, "");
    }
    
    public List<Dataverse> findLinkingDataverses(Long dataverseId, String searchTerm) {
        List<Dataverse> retList = new ArrayList<>();
        if (searchTerm == null || searchTerm.isEmpty()) {
            TypedQuery<DataverseLinkingDataverse> typedQuery = em.createNamedQuery("DataverseLinkingDataverse.findByDataverseId", DataverseLinkingDataverse.class)
                    .setParameter("dataverseId", dataverseId);
            for (DataverseLinkingDataverse dataverseLinkingDataverse : typedQuery.getResultList()) {
                retList.add(dataverseLinkingDataverse.getLinkingDataverse());
            }

        } else {
            
            String pattern = searchTerm.toLowerCase();

            String pattern1 = pattern + "%";
            String pattern2 = "% " + pattern + "%";

            // Adjust the queries for very short, 1 and 2-character patterns:
            if (pattern.length() == 1) {
                pattern1 = pattern;
                pattern2 = pattern + " %";
            }
            TypedQuery<Long> typedQuery
                    = em.createNamedQuery("DataverseLinkingDataverse.findByDataverseIdAndLinkingDataverseName", Long.class)
                            .setParameter(1, dataverseId).setParameter(2, "%dataverse").setParameter(3, pattern1)
                            .setParameter(4, pattern2).setParameter(5, "%dataverse").setParameter(6, pattern1).setParameter(7, pattern2);

            for (Long id : typedQuery.getResultList()) {
                retList.add(dataverseService.find(id));
            }
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
