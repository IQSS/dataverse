/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.util.List;
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
public class DatasetVersionServiceBean implements java.io.Serializable {

    @EJB
    DatasetServiceBean datasetService;
    
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    public DatasetVersion find(Object pk) {
        return (DatasetVersion) em.find(DatasetVersion.class, pk);
    }

    public DatasetVersion findByFriendlyVersionNumber(Long datasetId, String friendlyVersionNumber) {

        Long majorVersionNumber = null;
        Long minorVersionNumber = null;

        String[] versions = friendlyVersionNumber.split("\\.");
        try {
            if (versions.length == 1) {
                majorVersionNumber = Long.parseLong(versions[0]);
            } else if (versions.length == 2) {
                majorVersionNumber = Long.parseLong(versions[0]);
                minorVersionNumber = Long.parseLong(versions[1]);
            } else {
                return null;
            }
        } catch (NumberFormatException n) {
            return null;
        }

        if (majorVersionNumber != null && minorVersionNumber != null) {
            String queryStr = "SELECT v from DatasetVersion v where v.dataset.id = :datasetId  and v.versionNumber= :majorVersionNumber and v.minorVersionNumber= :minorVersionNumber";
            DatasetVersion foundDatasetVersion = null;
            try {
                Query query = em.createQuery(queryStr);
                query.setParameter("datasetId", datasetId);
                query.setParameter("majorVersionNumber", majorVersionNumber);
                query.setParameter("minorVersionNumber", minorVersionNumber);
                foundDatasetVersion = (DatasetVersion) query.getSingleResult();
            } catch (javax.persistence.NoResultException e) {
                System.out.print("no ds version found: " + datasetId + " " + friendlyVersionNumber);
                // DO nothing, just return null.
            }
            return foundDatasetVersion;

        }
        
        if (majorVersionNumber == null && minorVersionNumber == null) {

            return null;

        }

        if (majorVersionNumber != null && minorVersionNumber == null) {

            try {
                TypedQuery<DatasetVersion> typedQuery = em.createQuery("SELECT v from DatasetVersion v where v.dataset.id = :datasetId  and v.versionNumber= :majorVersionNumber", DatasetVersion.class);
                typedQuery.setParameter("datasetId", datasetId);
                typedQuery.setParameter("majorVersionNumber", majorVersionNumber);
                DatasetVersion retVal = null;
                List<DatasetVersion> versionsList = typedQuery.getResultList();
                for (DatasetVersion dsvTest : versionsList) {
                    if (retVal == null) {
                        retVal = dsvTest;
                    } else {
                        if (retVal.getMinorVersionNumber().intValue() < dsvTest.getMinorVersionNumber().intValue()) {
                            retVal = dsvTest;
                        }
                    }
                }

                return retVal;

            } catch (javax.persistence.NoResultException e) {
                System.out.print("no ds version found: " + datasetId + " " + friendlyVersionNumber);
                // DO nothing, just return null.
            }

        }

        return null;
    }

}
