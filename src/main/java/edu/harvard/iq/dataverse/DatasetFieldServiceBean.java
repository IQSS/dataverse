/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import java.util.List;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author xyang
 */
@Stateless
@Named
public class DatasetFieldServiceBean {

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    
    public List<DatasetField> findAll() {
        return em.createQuery("select object(o) from DatasetField as o where o.advancedSearchField = true and o.title != '' order by o.id").getResultList();
    }
}
