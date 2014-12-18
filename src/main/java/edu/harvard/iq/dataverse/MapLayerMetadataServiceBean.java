/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.User;
import java.util.Arrays;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

/**
 *
 * @author raprasad
 */
@Stateless
@Named
public class MapLayerMetadataServiceBean {
    
   
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    
    @EJB
    PermissionServiceBean permissionService;
   
    
    public MapLayerMetadata find(Object pk) {
        if (pk==null){
            return null;
        }
        return (MapLayerMetadata) em.find(MapLayerMetadata.class, pk);
    }
    
    public MapLayerMetadata save( MapLayerMetadata layer_metadata) {
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
    
    
    
    /*
        Given a datafile id, return the associated MapLayerMetadata object
    
    */
    public MapLayerMetadata findMetadataByDatafileId(Long datafile){
        
        if (datafile == null){
            return null;
        }
     
        try{
            Query query = em.createQuery("select m from MapLayerMetadata m WHERE m.dataFile=:datafile",  MapLayerMetadata.class);
            query.setParameter("datafile", datafile);
            return (MapLayerMetadata) query.getSingleResult();
        } catch ( NoResultException nre ) {
            return null;
        }    
    }
    
    
    /*
        Delete a mapLayerMetadata object.
    
        First check if the given user has permission to edit this data.
        
    */
    public boolean deleteMapLayerMetadataObject(MapLayerMetadata mapLayerMetadata, User user){
        if ((mapLayerMetadata == null)||(user==null)){
            return false;
        }
        
        if (!permissionService.userOn(user, mapLayerMetadata.getDataFile()).has(Permission.EditDataset)) { 
            em.remove(em.merge(mapLayerMetadata));
            return true;
        }
        return false;
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
    }
    
    
    
    public List<MapLayerMetadata> getMapLayerMetadataForDataset(Dataset dataset){
        if (dataset == null){
            return null;
        }
        Query query = em.createQuery("select object(o) from MapLayerMetadata as o where o.dataset=:dataset");// order by o.name");
        query.setParameter("dataset", dataset);
        return query.getResultList();
    }    
    
}

