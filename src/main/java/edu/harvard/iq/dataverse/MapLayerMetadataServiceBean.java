/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.api.WorldMapRelatedData;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.worldmapauth.WorldMapToken;
import edu.harvard.iq.dataverse.worldmapauth.WorldMapTokenServiceBean;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.logging.Level;
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
    DataFileServiceBean dataFileService;
    
    @EJB
    PermissionServiceBean permissionService;
    
    @EJB
    SystemConfig systemConfig;
    
    @EJB
    WorldMapTokenServiceBean tokenServiceBean;
   
    private static final Logger logger = Logger.getLogger(MapLayerMetadataServiceBean.class.getCanonicalName());

    private static final String GEOCONNECT_MAP_DELETE_API = "/tabular/delete-map-no-ui/";
    
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

            // Remove thumbnails associated with the map metadata
            // (this also sets theto set the "preview image" flag to false)
            //
            boolean success = this.deleteOlderMapThumbnails(mapLayerMetadata.getDataFile());
            
            // Remove the actual map metadata
            //
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
    private boolean deleteOlderMapThumbnails(DataFile dataFile) {
        if (dataFile==null){
            logger.warning("dataFile is null");
            return false;
        }

        // Set the preview image available flag to false
        dataFile.setPreviewImageAvailable(false);
        dataFile = dataFileService.save(dataFile);

        
        try {
            StorageIO<DataFile> storageIO = dataFile.getStorageIO();

            if (storageIO == null) {
                logger.warning("Null storageIO in deleteOlderMapThumbnails()");
                return false;
            }
            storageIO.open();
            List<String> cachedObjectsTags = storageIO.listAuxObjects();
            
            if (cachedObjectsTags != null) {
                String iconBaseTag = "img";
                String iconThumbTagPrefix = "thumb";
                for (String cachedFileTag : cachedObjectsTags) {
                    logger.info("found AUX tag: "+cachedFileTag);
                    if (iconBaseTag.equals(cachedFileTag) || cachedFileTag.startsWith(iconThumbTagPrefix)) {
                        logger.info("deleting cached AUX object "+cachedFileTag);
                        storageIO.deleteAuxObject(cachedFileTag);
                    }
                }
            }
            /*
             * Below is the old-style code that was assuming that all the files are 
             * stored on a local filesystem. The StorageIO code, above, should 
             * be used instead for all the operations on the physical files associated
             * with DataFiles. 
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
                logger.warning("DataFile directory is not actually a directory.  Directory path: " + fileDirectory.toString());
                return false;            
            }
        
            /* Iterate through directory and delete any ".img" files for this DataFile
           
                Example:
                Datafile name: 14a5e4abf7d-e7eebfb6474d
                Types of files that would be deleted (if they exist):
                    14a5e4abf7d-e7eebfb6474d.img
                    14a5e4abf7d-e7eebfb6474d.img.thumb64
                    14a5e4abf7d-e7eebfb6474d.img.thumb400
            *//*
            String iconBaseFilename = dataAccess.getFileSystemPath().toString() +  ".img";
            String iconThumbFilename = dataAccess.getFileSystemPath().toString() +  ".thumb";
        
            for (File singleFile : fileDirectory.listFiles()) {
                if (singleFile.toString().startsWith(iconBaseFilename) || singleFile.toString().startsWith(iconThumbFilename)) {
                    //logger.info("file found: " + singleFile.toString());
                    singleFile.delete();
                    //results.add(file.getName());
                }
            }*/
        } catch (IOException ioEx) {
            logger.warning("IOException in deleteOlderMapThumbnails(): "+ioEx.getMessage());
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
        
        this.deleteOlderMapThumbnails(mapLayerMetadata.getDataFile());

        if ((mapLayerMetadata.getMapImageLink()==null)||mapLayerMetadata.getMapImageLink().isEmpty()){
            logger.warning("mapLayerMetadata does not have a 'map_image_link' attribute");
            return false;
        }
        
        String imageUrl = mapLayerMetadata.getMapImageLink();
        imageUrl = imageUrl.replace("https:", "http:");
        logger.info("Attempt to retrieve map image: " + imageUrl);
        
        StorageIO<DataFile> dataAccess = null;
        try {
            dataAccess = mapLayerMetadata.getDataFile().getStorageIO();
        } catch (IOException ioEx) {
            dataAccess = null;
        }

        if (dataAccess == null) {
            logger.warning("Failed to open Access IO on DataFile "+mapLayerMetadata.getDataFile().getId());
            return false;
        }

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
        
        try {
            dataAccess.saveInputStreamAsAux(is, "img");
        } catch (IOException ioex) {
            logger.warning("Failed to save WorldMap-generated image; "+ioex.getMessage());
            return false;
        }
        
        logger.info("Done");
        return true;
    }   
    
    /* 
     * This method calls GeoConnect and requests that a map layer for this 
     * DataFile is deleted, from WorldMap and GeoConnect itself.
     * The use case here is when a user restricts a tabular file for which 
     * a geospatial map has already been made. 
     * This process is initiated on the Dataverse side, without any GeoConnect
     * UI in the picture. (The way a user normally deletes a layer map is by 
     * clicking 'Delete' on the GeoConnect map page). 
     * Otherwise this call follows the scheme used when accessing the 
     * /shapefile/map-it on GeoConnect - we send along a WorldMap token and a 
     * callback url for GC to download the file metadata.metadata
     * Depending on how it goes we receive a yes or no response from the server.
    */
    public void deleteMapLayerFromWorldMap(DataFile dataFile, AuthenticatedUser user) throws IOException {

        
        if (dataFile == null){
            logger.severe("dataFile cannot be null");
            return;
        }
        
        if (user == null){
            logger.severe("user cannot be null");
            return;            
        }
        
        // Worldmap token: 
        WorldMapToken token = tokenServiceBean.getNewToken(dataFile, user);
        if (token == null){
            logger.severe("token should NOT be null");
            return;
        }
       
        logger.info("-- new token id: " + token.getId());
        // Callback url for geoConnect: 
        String callback_url = URLEncoder.encode(systemConfig.getDataverseSiteUrl() + WorldMapRelatedData.GET_WORLDMAP_DATAFILE_API_PATH);
        
        String geoConnectAddress = token.getApplication().getMapitLink();
        /* 
         * this is a bit of a hack - there should be a cleaner way to get the base 
         * geoconnect URL from the token!
        */
        geoConnectAddress = geoConnectAddress.replace("/shapefile/map-it", "");
        
        logger.log(Level.INFO, "callback_url: {0}", callback_url);
        
        //String geoConnectCommand = geoConnectAddress + GEOCONNECT_MAP_DELETE_API + token.getApplication().getMapitLink() + "/" + token.getToken() + "/?cb=" +  callback_url;
        String geoConnectCommand = geoConnectAddress + GEOCONNECT_MAP_DELETE_API + token.getToken() + "/?cb=" +  callback_url;
        logger.info("-- new token id 2: " + token.getId());

        
        logger.info("Attempting to call GeoConnect to request that the WorldMap layer for DataFile "+dataFile.getId()+":\n"+geoConnectCommand);
        URL geoConnectUrl = new URL(geoConnectCommand);
        
        HttpURLConnection geoConnectConnection = (HttpURLConnection)geoConnectUrl.openConnection();
        
        geoConnectConnection.setRequestMethod("GET");
        geoConnectConnection.connect();
        
        int code = geoConnectConnection.getResponseCode();
        logger.info("response code: " + code);
        if (code != 200) {
            throw new IOException ("Failed to delete Map Layer via GeoConnect. /tabular/delete-map HTTP code response: "+code+"");
        }
        logger.info("response :" + geoConnectConnection.getContent());
        
    }

    public List<MapLayerMetadata> findAll() {
        TypedQuery<MapLayerMetadata> typedQuery = em.createNamedQuery("MapLayerMetadata.findAll", MapLayerMetadata.class);
        List<MapLayerMetadata> mapLayerMetadatas = typedQuery.getResultList();
        return mapLayerMetadatas;
    }

}

