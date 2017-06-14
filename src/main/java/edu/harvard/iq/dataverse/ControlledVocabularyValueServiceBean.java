package edu.harvard.iq.dataverse;

import java.util.List;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author skraffmiller
 */
@Stateless
@Named
public class ControlledVocabularyValueServiceBean implements java.io.Serializable {

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    
    public List<ControlledVocabularyValue> findByDatasetFieldTypeId(Long dsftId) {
        return em.createNamedQuery("ControlledVocabularyValue.findByDatasetFieldTypeId")
                .setParameter("datasetFieldTypeId", dsftId)
                .getResultList();
    }
    
}
