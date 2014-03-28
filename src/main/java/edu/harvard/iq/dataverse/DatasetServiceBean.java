/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

/**
 *
 * @author skraffmiller
 */
@Stateless
@Named
public class DatasetServiceBean {

    private static final Logger logger = Logger.getLogger(DatasetServiceBean.class.getCanonicalName());
    @EJB
    IndexServiceBean indexService;

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    public Dataset CreateDatasetCommand(Dataset dataset) {
        return save(dataset);
    }

    public Dataset UpdateDatasetCommand(Dataset dataset) {
        return save(dataset);
    }

    public void saveDatasetAPI(Dataset dataset) {
        //Called by depricated method on API
        //Should put into command?
        save(dataset);
    }

    public Dataset save(Dataset dataset) {
        em.merge(removeBlankRows(dataset.getVersions().get(0)));
        Dataset savedDataset = em.merge(dataset);
        //TODO - Commented out indexing because 
        //Release dataset fails
        //String indexingResult = indexService.indexDataset(savedDataset);
        System.out.print("after indexing saved...");
        //logger.info("during dataset save, indexing result was: " + indexingResult);
        return savedDataset;
    }

    private DatasetVersion removeBlankRows(DatasetVersion version) {
        //Trim spaces from any input values
        //add any blank records to a "to Remove" list"

        List<Integer> toRemoveIndex = new ArrayList();
        int index = 0;

        for (DatasetFieldValue dsfv : version.getDatasetFieldValues()) {
            //Only want to drop if never saved previously
            // if in DB the blank will blank out the existing value
            if (dsfv.getId() == null) {
                if (dsfv.getStrValue() != null) {
                    dsfv.setStrValue(dsfv.getStrValue().trim());
                }

                //Single recs and child recs (with no controlled vocab)
                if ((!dsfv.getDatasetField().isHasChildren() && !dsfv.getDatasetField().isControlledVocabulary()) && (dsfv.getStrValue() == null || dsfv.getStrValue().trim().isEmpty())) {
                    toRemoveIndex.add(index);
                }
                //parent recs where all kids are empty.
                if (dsfv.getDatasetField().isHasChildren() && dsfv.isChildEmpty()) {
                    toRemoveIndex.add(index);
                }
                //controlled vocab recs where all kids are empty.
                if (dsfv.getDatasetField().isControlledVocabulary() && (dsfv.getControlledVocabularyValues() == null || dsfv.getControlledVocabularyValues().isEmpty())) {
                    toRemoveIndex.add(index);
                }

            }
            index++;
        }
        //Actually do the remove here
        // the adjustment takes into account the prior 
        //blank fields which have been removed.
        int adjustment = 0;
        if (!toRemoveIndex.isEmpty()) {
            for (Integer dsfvRI : toRemoveIndex) {
                version.getDatasetFieldValues().remove(dsfvRI.intValue() - adjustment);
                adjustment++;
            }
        }
        return version;
    }

    public Dataset removeRecs(Dataset dataset, List<DatasetFieldValue> toDelete) {
        for (DatasetFieldValue dsfv : toDelete) {
            deleteVal(dsfv);
        }
        return null;
    }

    private void deleteVal(DatasetFieldValue dsfv) {
        if (dsfv.getId() != null) {
            DatasetFieldValue p = em.find(DatasetFieldValue.class, dsfv.getId());
            em.remove(p);
        }
    }

    public Dataset find(Object pk) {
        return (Dataset) em.find(Dataset.class, pk);
    }

    public List<Dataset> findByOwnerId(Long ownerId) {
        Query query = em.createQuery("select object(o) from Dataset as o where o.owner.id =:ownerId order by o.id");
        query.setParameter("ownerId", ownerId);
        return query.getResultList();
    }

    public List<Dataset> findAll() {
        return em.createQuery("select object(o) from Dataset as o order by o.id").getResultList();
    }

    public void removeCollectionElement(Collection coll, Object elem) {
        coll.remove(elem);
        em.remove(elem);
    }

    public void removeCollectionElement(List list, int index) {
        System.out.println("index is " + index + ", list size is " + list.size());
        em.remove(list.get(index));
        list.remove(index);
    }

    public void removeCollectionElement(Iterator iter, Object elem) {
        iter.remove();
        em.remove(elem);
    }

    public String getDatasetVersionTitle(DatasetVersion version) {
        Long id = version.getId();
        Query query = em.createQuery("select v.strValue from DatasetFieldValue as v, DatasetVersion as dv, DatasetField as dsf where dsf.name ='title'"
                + " and dsf.id = v.datasetField.id and dv.id =:id ");
        query.setParameter("id", id);
        return (String) query.getSingleResult();
    }

    public void generateFileSystemName(DataFile dataFile) {
        String fileSystemName = null;
        Long result = (Long) em.createNativeQuery("select nextval('filesystemname_seq')").getSingleResult();
        dataFile.setFileSystemName(result.toString());

    }

}
