/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import java.util.Arrays;
import java.util.List;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

/**
 *
 * @author rmp553
 */
@Stateless
@Named
public class MapLayerMetadataServiceBean {
    

    public final static List<String> MANDATORY_JSON_FIELDS = Arrays.asList("layerName", "layerLink", "embedMapLink", "worldmapUsername");

    
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    
    
    public MapLayerMetadata find(Object pk) {
        if (pk==null){
            return null;
        }
        return (MapLayerMetadata) em.find(MapLayerMetadata.class, pk);
    }
    
    public MapLayerMetadata save( MapLayerMetadata layer_metadata ) {
        if (layer_metadata==null){
            return null;
        }
        if ( layer_metadata.getId() == null ) {
            em.persist(layer_metadata);
            return layer_metadata;
	} else {
            return em.merge( layer_metadata );
	}
    }
	        
   
    public MapLayerMetadata findMetadataByLayerNameAndDatafile(String layer_name){//, DataFile datafile) {
        if ((layer_name == null)){//||(datafile==null)){
            return null;
        }
        //Query query = em.createQuery("select o.id from MapLayerMetadta as o where o.layer_name =:layerName and o.datafile_id =:datafileID;");
        //Query query = em.createQuery("select m from MapLayerMetadata m where m.layer_name =:layerName ;");
        try{
            return em.createQuery("select m from MapLayerMetadata m WHERE m.layerName=:layerName", MapLayerMetadata.class)
					.setParameter("layerName", layer_name)
					.getSingleResult();
        } catch ( NoResultException nre ) {
            return null;
        }    
 //       "select d from Dataverse d WHERE d.alias=:alias",
        //query.setParameter("layerName", layerName);
       // query.setParameter("datafileID", datafile.getId());
        
        //return this.find(query.getFirstResult());
        //return query.getFirstResult();
       // return query.getResultList();
    }
    
}

