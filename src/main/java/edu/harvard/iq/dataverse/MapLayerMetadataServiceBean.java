/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.dataaccess.DataFileIO;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

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
   
    private static final Logger logger = Logger.getLogger(MapLayerMetadataServiceBean.class.getCanonicalName());

    
    public MapLayerMetadata find(Object pk) {
        if (pk==null){
            return null;
        }
        return em.find(MapLayerMetadata.class, pk);
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
    public MapLayerMetadata findMetadataByDatafile(DataFile datafile){
        
        if (datafile == null){
            return null;
        }
     
        try{
 //           String sqlStatement = 
            Query query = em.createQuery("select m from MapLayerMetadata m WHERE m.dataFile=:datafile",  MapLayerMetadata.class);
            query.setParameter("datafile", datafile);
            query.setMaxResults(1);
            //entityManager.createQuery(SQL_QUERY).setParameter(arg0,arg1).setMaxResults(10).getResultList();
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
        logger.info("deleteMapLayerMetadataObject");
        
        if ((mapLayerMetadata == null)||(user==null)){
            return false;
        }
        
        if (permissionService.userOn(user, mapLayerMetadata.getDataFile().getOwner()).has(Permission.EditDataset)) { 
            em.remove(em.merge(mapLayerMetadata));

            this.deleteOlderMapThumbnails(mapLayerMetadata);
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
        TypedQuery<MapLayerMetadata> query = em.createQuery("select object(o) from MapLayerMetadata as o where o.dataset=:dataset", MapLayerMetadata.class);// order by o.name");
        query.setParameter("dataset", dataset);
        return query.getResultList();
    }    
    
    
    /**
     * Before downloading a file for map icons (see "retrieveMapImageForIcon" below),
     * first remove any existing .img and .img.* files
     * 
     * e.g. delete all that start with (DataFile name) + ".img"
     * 
     * @param mapLayerMetadata
     * @return
     * @throws IOException 
     */
    private boolean deleteOlderMapThumbnails(MapLayerMetadata mapLayerMetadata) {
        if (mapLayerMetadata==null){
            logger.warning("mapLayerMetadata is null");
            return false;
        }

        // Retrieve the data file
        //
        DataFile df = mapLayerMetadata.getDataFile();
        
        try {
            DataFileIO dataAccess = df.getAccessObject();

            if (dataAccess == null || !dataAccess.isLocalFile()) {
                return false;
            }
            // Get the parent directory
            //
            Path fileDirname = dataAccess.getFileSystemPath().getParent();
            if (fileDirname == null){
                logger.warning("DataFile directory has null path.  Directory path: " + dataAccess.getFileSystemPath().toString());
                return false;
            }
        
            // Verify that the directory exists
            //
            File fileDirectory = new File(fileDirname.normalize().toString());
            if (!(fileDirectory.isDirectory())){
                logger.warning("DataFile directory is not actuall a directory.  Directory path: " + fileDirectory.toString());
                return false;            
            }
        
            /* Iterate through directory and delete any ".img" files for this DataFile
           
                Example:
                Datafile name: 14a5e4abf7d-e7eebfb6474d
                Types of files that would be deleted (if they exist):
                    14a5e4abf7d-e7eebfb6474d.img
                    14a5e4abf7d-e7eebfb6474d.img.thumb64
                    14a5e4abf7d-e7eebfb6474d.img.thumb400
            */
            String iconBaseFilename = dataAccess.getFileSystemPath().toString() +  ".img";
        
            for (File singleFile : fileDirectory.listFiles()) {
                if (singleFile.toString().startsWith(iconBaseFilename)) {
                    //logger.info("file found: " + singleFile.toString());
                    singleFile.delete();
                    //results.add(file.getName());
                }
            }
        } catch (IOException ioEx) {
            return false; 
        }
                
        return true;
    }
    
    /**
     * Use the mapLayerMetadata.mapImageLink to retrieve a PNG file directly from WorldMap
     * 
     * Next step: Save this image as the default icon
     * 
     * Example mapImageLink: http://worldmap.harvard.edu/download/wms/14708/png?layers=geonode:power_plants_enipedia_jan_2014_kvg&width=948&bbox=76.04800165,18.31860358,132.0322222,50.78441&service=WMS&format=image/png&srs=EPSG:4326&request=GetMap&height=550
     * 
     * Parameter by parameter (note width/height):
     * http://worldmap.harvard.edu/download/wms/14708/png?
     *           layers=geonode:power_plants_enipedia_jan_2014_kvg
     *           width=948
     *           bbox=76.04800165,18.31860358,132.0322222,50.78441
     *           service=WMS
     *           format=image/png
     *           srs=EPSG:4326
     *           request=GetMap
     *           height=550
     * 
     * @param mapLayerMetadata
     * @return boolean
     * @throws IOException 
     */
    public boolean retrieveMapImageForIcon(MapLayerMetadata mapLayerMetadata) throws IOException {
        if (mapLayerMetadata==null){
            logger.warning("mapLayerMetadata is null");
            return false;
        }
        
        this.deleteOlderMapThumbnails(mapLayerMetadata);
        if (true){
            // debug check
            // return false;
        }
        if ((mapLayerMetadata.getMapImageLink()==null)||mapLayerMetadata.getMapImageLink().isEmpty()){
            logger.warning("mapLayerMetadata does not have a 'map_image_link' attribute");
            return false;
        }
        
        String imageUrl = mapLayerMetadata.getMapImageLink();
        imageUrl = imageUrl.replace("https:", "http:");
        logger.info("Attempt to retrieve map image: " + imageUrl);
        
        DataFileIO dataAccess = null;
        try {
            dataAccess = mapLayerMetadata.getDataFile().getAccessObject();
        } catch (IOException ioEx) {
            dataAccess = null;
        }

        if (dataAccess == null || !dataAccess.isLocalFile()) {
            return false;
        }
        String destinationFile = dataAccess.getFileSystemPath().toString() +  ".img";
        logger.info("destinationFile: getFileSystemLocation()" + dataAccess.getFileSystemPath().toString());
        logger.info("destinationFile: " + destinationFile);
        
        URL url = new URL(imageUrl);
        logger.info("retrieve url : " + imageUrl);

        logger.info("try to open InputStream");
        InputStream is = null;
        
        try{
            is = url.openStream();
        }catch(IOException exio){
            logger.warning("Error when retrieving map icon image. Exception: " + exio.getMessage());
            if (is!=null){
                try { is.close(); } catch (IOException ignore) {}
            }
            return false;
            
        }
        
        OutputStream os = null;
        
        try{
            logger.info("try to start OutputStream");
            os = new FileOutputStream(destinationFile);
        } catch (Exception ex){
            logger.warning("Error when retrieving map icon image. Exception: " + ex.getMessage());
            if (os!=null){
                try { os.close(); } catch (IOException ignore) {}
            }
            return false;
        }
        
        byte[] b = new byte[2048];
        int length;

        logger.info("Writing file...");
        while ((length = is.read(b)) != -1) {
            os.write(b, 0, length);
        }

        logger.info("Closing streams...");
        is.close();
        os.close();
        
        logger.info("Done");
        return true;
    }   

    public List<MapLayerMetadata> findAll() {
        TypedQuery<MapLayerMetadata> typedQuery = em.createNamedQuery("MapLayerMetadata.findAll", MapLayerMetadata.class);
        List<MapLayerMetadata> mapLayerMetadatas = typedQuery.getResultList();
        return mapLayerMetadatas;
    }

}

