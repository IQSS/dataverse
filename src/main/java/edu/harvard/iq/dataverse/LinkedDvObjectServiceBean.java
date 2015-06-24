/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.util.ArrayList;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

/**
 *
 * @author skraffmi
 */
@Stateless
public class LinkedDvObjectServiceBean implements java.io.Serializable {

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    @EJB
    DataverseServiceBean dataverseService;

    public List<DvObject> findLinkedDvObjects(Long linkingDataverseId) {
        List<DvObject> retList = new ArrayList();
        Query query = em.createQuery("select object(o) from LinkedDvObject as o where o.owner.id =:linkingDataverseId order by o.id");
        query.setParameter("linkingDataverseId", linkingDataverseId);
        for (Object o : query.getResultList()) {
            LinkedDvObject convterted = (LinkedDvObject) o;
            retList.add(convterted.getDvObject());
        }
        return retList;
    }

    public List<Dataverse> findLinkingDataverses(Long dvObjectId) {
        List<Dataverse> retList = new ArrayList();
        Query query = em.createQuery("select object(o) from LinkedDvObject as o where o.dvObject.id =:dvObjectId order by o.id");
        query.setParameter("dvObjectId", dvObjectId);
        for (Object o : query.getResultList()) {
            LinkedDvObject convterted = (LinkedDvObject) o;
            retList.add(convterted.getOwner());
        }
        return retList;
    }

    public void save(LinkedDvObject linkedDvObject) {
        if (linkedDvObject.getId() == null) {
            em.persist(linkedDvObject);
        } else {
            em.merge(linkedDvObject);
        }
    }

    public boolean alreadyLinked(Dataverse definitionPoint, Dataverse dataverseToLinkTo) {
        TypedQuery<DataverseLinkingDataverse> typedQuery = em.createQuery("SELECT OBJECT(o) FROM DataverseLinkingDataverse AS o WHERE o.linkingDataverse.id = :dataverseId AND o.dataverse.id = :dataverseToLinkTo", DataverseLinkingDataverse.class);
        typedQuery.setParameter("dataverseId", definitionPoint.getId());
        typedQuery.setParameter("dataverseToLinkTo", dataverseToLinkTo.getId());
        return !typedQuery.getResultList().isEmpty();
    }

}
