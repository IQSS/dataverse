/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.util.StringUtil;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.bean.ManagedProperty;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author rmp553
 */
@ViewScoped
@Named("DataFileMapPage")
public class DataFileMapPage implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(DataFileMapPage.class.getCanonicalName());

    @Inject
    DataverseSession session;

    @EJB
    MapLayerMetadataServiceBean mapLayerMetadataService;
    @EJB
    DataFileServiceBean datafileService;
    /*@EJB
    DatasetServiceBean datasetService;
    @EJB
    DatasetVersionServiceBean datasetVersionService;
    @EJB
    PermissionServiceBean permissionService;
    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    BuiltinUserServiceBean builtinUserService;
    */
    /*
    http://localhost:8080/datafilemap.xhtml?mapid=6
    */
    private Long mapId;// = new Long(5);
    
    private boolean mapLayerMetadataNotFound;

    private MapLayerMetadata mapLayerMetadata;
    private DataFile dataFile;
    private Dataset dataset;
    private Dataverse dataverse;
    private DatasetVersion datasetVersion;

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    
    public Long getMapId() {
        return this.mapId;
    }

    public void setMapId(Long id) {
        this.mapId = id;
    }

    
    public MapLayerMetadata getMapLayerMetadata() {
        return this.mapLayerMetadata;
    }

    public void setMapLayerMetadata(MapLayerMetadata mapLayerMetadata) {
        this.mapLayerMetadata = mapLayerMetadata;
    }

    // get/set dataFile
    //
    public DataFile getDataFile() {
        return this.dataFile;
    }

    public void setDataFile(DataFile datafile) {
        this.dataFile = dataFile;
    }

    // get/set dataset
    //
    public Dataset getDataset() {
        return this.dataset;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }
    
    
    // get/set datasetVersion
    //
    public DatasetVersion getDatasetVersion() {
        return this.datasetVersion;
    }

    public void setVersionDatasetVersion(DatasetVersion datasetVersion) {
        this.datasetVersion = datasetVersion;
    }

    // get/set dataverse
    //
    public Dataverse getDataverse() {
        return this.dataverse;
    }

    public void setDataverse(Dataverse dataverse) {
        this.dataverse = dataverse;
    }

        
    
    public DataFileMapPage() {
        logger.info("DataFileMapPage.constructor");
        this.name = "--dv user--";
        this.mapId = null;
        this.mapLayerMetadata = null;
        this.dataFile = null;
        this.dataset = null;
        this.datasetVersion = null;
        this.dataverse = null;
    }
    
    
    /**
     * Using the id in the url, retrieve the mapLayerMetadata
     *
     */
    private MapLayerMetadata getMapLayerMetadata(Long mapLayerMetadataID) {
       logger.info("a - getMapLayerMetadata, id: "+ mapLayerMetadataID);

       if (mapLayerMetadataID == null) {
            return null;
        }

       logger.info("b - getMapLayerMetadata, id: "+ mapLayerMetadataID);

        // Already retrieved, return it
        if (!(this.mapLayerMetadata==null)){
            return this.mapLayerMetadata;
        }

       logger.info("c - getMapLayerMetadata, id: "+ mapLayerMetadataID);

        // Attempt to retrieve mapLayerMetadata from database
        this.mapLayerMetadata = mapLayerMetadataService.find(mapLayerMetadataID);
       
        
        logger.info("d - getMapLayerMetadata, id: "+ mapLayerMetadataID);

        
        // Not found in database
        if (this.mapLayerMetadata == null) {
            this.mapLayerMetadataNotFound = true;
            return null;
        }
        
        logger.info("e - getMapLayerMetadata, id: "+ mapLayerMetadataID);

        // Found mapLayerMetadata
        this.mapLayerMetadataNotFound = false;
        this.dataFile = this.mapLayerMetadata.getDataFile();
        this.dataset = this.mapLayerMetadata.getDataset();
        this.datasetVersion = this.dataset.getLatestVersion();
        this.dataverse = this.dataset.getOwner();
        logger.info("f - getMapLayerMetadata, id: "+ mapLayerMetadataID);
        logger.info("mapLayerMetadata "+ mapLayerMetadata.getLayerName());


        return this.mapLayerMetadata;

    }// A DataFile may have a related MapLayerMetadata object

    public String init() {
       logger.info("1-DataFileMapPage.init");
       if (true){
       //    return null;
       }
       
        // No id specified in the url, raise a 404
        //
        if (this.getMapId() == null) {
           logger.info("2-no mapid");
            this.mapLayerMetadataNotFound = true;
            return "/404.xhtml";
        }

        // Retrieve the map layer based on the id
        logger.info("3-try retrieval");
        this.getMapLayerMetadata(this.getMapId());
        
        // If no map metadata is found for this id, throw a 404
        //
        if (this.mapLayerMetadataNotFound){
            logger.info("4-no maplayer");
            return "/404.xhtml";
        }
                
        return null;
    }
    
/*
    private String getUserName(User user) {
        if (user.isAuthenticated()) {
            AuthenticatedUser authUser = (AuthenticatedUser) user;
            return authUser.getName();
        }
        return "Guest";
    }
 */
}
