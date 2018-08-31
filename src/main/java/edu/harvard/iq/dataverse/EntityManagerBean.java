package edu.harvard.iq.dataverse;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

public class EntityManagerBean implements java.io.Serializable	{

    @Inject
    DataverseSession session;

    @PersistenceContext(unitName = "masterPU")
    protected EntityManager em;
    
    @PersistenceContext(unitName = "slavePU")
    protected EntityManager em2;
    
    public EntityManager getMasterEM()    {
        return em;
    }
    
    public EntityManager getEntityManager() {
        if(session.getUser().isAuthenticated())  {
            return em;
        }
        return em2;
    }
}
