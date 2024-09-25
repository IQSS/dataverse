/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.persistence.dataset.ControlledVocabularyValue;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import java.util.List;

/**
 * @author skraffmiller
 */
@Stateless
public class ControlledVocabularyValueServiceBean implements java.io.Serializable {

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    public List<ControlledVocabularyValue> findByDatasetFieldTypeId(Long dsftId) {

        String queryString = "select o from ControlledVocabularyValue as o where o.datasetFieldType.id = " + dsftId + " ";
        TypedQuery<ControlledVocabularyValue> query = em.createQuery(queryString, ControlledVocabularyValue.class);
        return query.getResultList();

    }
    public List<ControlledVocabularyValue> findByDatasetFieldTypeNameAndValueLike(String datasetFieldTypeName, String suggestionSourceFieldValue, int queryLimit) {

        String queryString = "select DISTINCT v from ControlledVocabularyValue as v " +
                "where UPPER(v.strValue) LIKE CONCAT('%', UPPER(:suggestionSourceFieldValue), '%') " +
                "and v.datasetFieldType.id = (select d.id from DatasetFieldType as d where d.name = :datasetFieldTypeName)";
        TypedQuery<ControlledVocabularyValue> query = em.createQuery(queryString, ControlledVocabularyValue.class);
        query.setParameter("suggestionSourceFieldValue", suggestionSourceFieldValue);
        query.setParameter("datasetFieldTypeName", datasetFieldTypeName);
        query.setMaxResults(queryLimit);
        query.setHint("eclipselink.QUERY_RESULTS_CACHE", "TRUE");
        return query.getResultList();
    }
}
