/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.trsa.registry;

import edu.harvard.iq.dataverse.trsa.TrsaRegistry;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author asone
 */
@Stateless
public class TrsaRegistryFacade extends AbstractFacade<TrsaRegistry> {

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public TrsaRegistryFacade() {
        super(TrsaRegistry.class);
    }
    
}
