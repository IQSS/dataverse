/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.datasetutility;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.MapLayerMetadata;
import edu.harvard.iq.dataverse.MapLayerMetadataServiceBean;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class originally encapsulated display logic for the DatasetPage
 * 
 * It allows the following checks without redundantly querying the db to 
 * check permissions or if MapLayerMetadata exists
 * 
 * - canUserSeeMapDataButton (private)
 *      - canUserSeeMapDataButtonFromPage (public)
 *      - canUserSeeMapDataButtonFromAPI (public)
 * 
 * - canSeeMapButtonReminderToPublish (private)
 *      - canSeeMapButtonReminderToPublishFromPage (public)
 *      - canSeeMapButtonReminderToPublishFromAPI (public)
 * 
 * - canUserSeeExploreWorldMapButton (private)
 *      - canUserSeeExploreWorldMapButtonFromPage (public)
 *      - canUserSeeExploreWorldMapButtonFromAPI (public)
 * 
 * @author rmp553
 */
public class WorldMapPermissionHelper {
    
    private SettingsServiceBean settingsService;
    private MapLayerMetadataServiceBean mapLayerMetadataService;
    private PermissionServiceBean permissionService;

    private Dataset dataset;
    
    private final Map<Long, Boolean> fileMetadataWorldMapExplore = new HashMap<>(); // { FileMetadata.id : Boolean } 
    private final Map<Long, MapLayerMetadata> mapLayerMetadataLookup = new HashMap<>();

    
    public WorldMapPermissionHelper(SettingsServiceBean settingsService, MapLayerMetadataServiceBean mapLayerMetadataService,
                Dataset dataset, PermissionServiceBean permissionService){

        if (dataset == null){
            throw new NullPointerException("dataset cannot be null");
        }
        if (dataset.getId() == null){
            throw new NullPointerException("dataset must be saved! (have an id)");
        }
        
        if (settingsService == null){
            throw new NullPointerException("settingsService cannot be null");
        }
        if (mapLayerMetadataService == null){
            throw new NullPointerException("mapLayerMetadataService cannot be null");
        }
 
        this.dataset = dataset;
        
        this.settingsService = settingsService;
        this.mapLayerMetadataService = mapLayerMetadataService;
        this.permissionService = permissionService;
        
        loadMapLayerMetadataLookup();
    }
 
    
    /**
     * Convenience method for instantiating from dataset page or File page
     * 
     * Does NOT use PermissionServiceBean
     * 
     * @param settingsService
     * @param mapLayerMetadataService
     * @param dataset
     * @return 
     */
    public static WorldMapPermissionHelper getPermissionHelperForDatasetPage(
                SettingsServiceBean settingsService, MapLayerMetadataServiceBean mapLayerMetadataService,
                Dataset dataset){

       return new WorldMapPermissionHelper(settingsService, mapLayerMetadataService, dataset, null);        
    }
    
    /**
     * Convenience method for instantiating from the API
     * 
     *  REQUIRES PermissionServiceBean
     * 
     * @param settingsService
     * @param mapLayerMetadataService
     * @param dataset
     * @param permissionService
     * @return 
     */
    public static WorldMapPermissionHelper getPermissionHelperForAPI(
                SettingsServiceBean settingsService, 
                MapLayerMetadataServiceBean mapLayerMetadataService,
                Dataset dataset,
                PermissionServiceBean permissionService){

       if (permissionService == null){
           throw new NullPointerException("permissionService is required for API checks");
       }
       
       return new WorldMapPermissionHelper(settingsService, mapLayerMetadataService, dataset, permissionService);        
    }
    
    
    /**
     * Create a hashmap consisting of { DataFile.id : MapLayerMetadata object}
     *
     * Very few DataFiles will have associated MapLayerMetadata objects so only
     * use 1 query to get them
     */
    private void loadMapLayerMetadataLookup() {
        
        
        List<MapLayerMetadata> mapLayerMetadataList = mapLayerMetadataService.getMapLayerMetadataForDataset(this.dataset);
        if (mapLayerMetadataList == null) {
            return;
        }
        for (MapLayerMetadata layer_metadata : mapLayerMetadataList) {
            mapLayerMetadataLookup.put(layer_metadata.getDataFile().getId(), layer_metadata);
        }

    }// A DataFile may have a related MapLayerMetadata object

    
     /**
     * Using a DataFile id, retrieve an associated MapLayerMetadata object
     *
     * The MapLayerMetadata objects have been fetched at page inception by
     * "loadMapLayerMetadataLookup()"
     */
    public MapLayerMetadata getMapLayerMetadata(DataFile df) {
        if (df == null) {
            return null;
        }
        return this.mapLayerMetadataLookup.get(df.getId());
    }
    
 
    /*
     * Call this when using the API
     *   - calls private method canUserSeeExploreWorldMapButton
     */ 
    public boolean canUserSeeExploreWorldMapButtonFromAPI(FileMetadata fm, User user){
     
        if (fm == null){
            return false;
        }
        if (user==null){
            return false;
        }
        if (!this.permissionService.userOn(user, fm.getDataFile()).has(Permission.DownloadFile)){
            return false;
        }

        return this.canUserSeeExploreWorldMapButton(fm, true);    
    }
    
     /**
     * Call this from a Dataset or File page
     *   - calls private method canUserSeeExploreWorldMapButton
     * 
     *  WARNING: Before calling this, make sure the user has download
     *  permission for the file!!  (See DatasetPage.canDownloadFile())
     *   
     * @param FileMetadata fm
     * @return boolean
     */
    public boolean canUserSeeExploreWorldMapButtonFromPage(FileMetadata fm){
        
        if (fm==null){
            return false;
        }
        
        return this.canUserSeeExploreWorldMapButton(fm, true);
    }
    
    /**
     *  WARNING: Before calling this, make sure the user has download
     *  permission for the file!!  (See DatasetPage.canDownloadFile())
     * 
     * Should there be a Explore WorldMap Button for this file?
     *   See table in: https://github.com/IQSS/dataverse/issues/1618
     * 
     *  (1) Does the file have MapLayerMetadata?
     *  (2) Are the proper settings in place
     * 
     * @param fm FileMetadata
     * @return boolean
     */
    private boolean canUserSeeExploreWorldMapButton(FileMetadata fm, boolean permissionsChecked){

        if (fm==null){
            return false;
        }
        
        // This is only here to make the public method users think...
        if (!permissionsChecked){
            return false;
        }
        
        if (this.fileMetadataWorldMapExplore.containsKey(fm.getId())){
            // Yes, return previous answer
            //logger.info("using cached result for candownloadfile on filemetadata "+fid);
            return this.fileMetadataWorldMapExplore.get(fm.getId());
        }
        
        /* -----------------------------------------------------
           Does a Map Exist?
         ----------------------------------------------------- */
        if (!(this.hasMapLayerMetadata(fm))){
            // Nope: no button
            this.fileMetadataWorldMapExplore.put(fm.getId(), false);
            return false;
        }
              
        /*
            Is setting for GeoconnectViewMaps true?
            Nope? no button
        */
        if (!settingsService.isTrueForKey(SettingsServiceBean.Key.GeoconnectViewMaps, false)){
            this.fileMetadataWorldMapExplore.put(fm.getId(), false);
            return false;
        }        
        
        /* -----------------------------------------------------
             Yes: User can view button!
         ----------------------------------------------------- */                    
        this.fileMetadataWorldMapExplore.put(fm.getId(), true);
        return true;
    }

    
    /*
     Check if the FileMetadata.dataFile has an associated MapLayerMetadata object
    
     The MapLayerMetadata objects have been fetched at page inception by "loadMapLayerMetadataLookup()" 
     */
    public boolean hasMapLayerMetadata(FileMetadata fm) {
        if (fm == null) {
            return false;
        }
        if (fm.getDataFile() == null) {
            return false;
        }
        return doesDataFileHaveMapLayerMetadata(fm.getDataFile());
    }

    /**
     * Check if a DataFile has an associated MapLayerMetadata object
     *
     * The MapLayerMetadata objects have been fetched at page inception by
     * "loadMapLayerMetadataLookup()"
     */
    private boolean doesDataFileHaveMapLayerMetadata(DataFile df) {
        if (df == null) {
            return false;
        }
        if (df.getId() == null) {
            return false;
        }
        return this.mapLayerMetadataLookup.containsKey(df.getId());
    }

    
    
    /**
     * Check if this is a mappable file type.
     * 
     * Currently (2/2016)
     * - Shapefile (zipped shapefile)
     * - Tabular file with Geospatial Data tag
     * 
     * @param fm
     * @return 
     */
    private boolean isPotentiallyMappableFileType(FileMetadata fm){
        if (fm==null){
            return false;
        }
        
        // Yes, it's a shapefile
        //
        if (this.isShapefileType(fm)){
            return true;
        }
        
        // Yes, it's tabular with a geospatial tag
        //
        if (fm.getDataFile().isTabularData()){
            if (fm.getDataFile().hasGeospatialTag()){
                return true;
            } 
        }
        return false;
    }
    
    
    
    public boolean isShapefileType(FileMetadata fm) {
        if (fm == null) {
            return false;
        }
        if (fm.getDataFile() == null) {
            return false;
        }

        return fm.getDataFile().isShapefileType();
    }

    
    /**
     * Call this from a Dataset or File page
     *   - calls private method canSeeMapButtonReminderToPublish
     * 
     *  WARNING: Assumes user isAuthenicated AND has Permission.EditDataset
     *      - These checks should be made on the DatasetPage or FilePage which calls this method
     * 
     *   
     * @param FileMetadata fm
     * @return boolean
     */
    public boolean canSeeMapButtonReminderToPublishFromPage(FileMetadata fm){
        if (fm == null){
            return false;
        }
        
        return this.canSeeMapButtonReminderToPublish(fm, true);
        
    }


    /**
     * Call this when using the API
     *   - calls private method canSeeMapButtonReminderToPublish
     * 
     * @param fm
     * @param user
     * @return 
     */
    public boolean canSeeMapButtonReminderToPublishFromAPI(FileMetadata fm, User user){
        if (fm == null){
            return false;
        }
        if (user==null){
            return false;
        }
        
        if (!this.permissionService.userOn(user, this.dataset).has(Permission.EditDataset)){
            return false;
        }

        return this.canSeeMapButtonReminderToPublish(fm, true);
        
    }


    
    /**
     *   Assumes permissions have been checked!!
     *
     *  See table in: https://github.com/IQSS/dataverse/issues/1618
     * 
     *  Can the user see a reminder to publish button?
     *   (1) Is the view GeoconnectViewMaps
     *   (2) Is this file a Shapefile or a Tabular file tagged as Geospatial?
     *   (3) Is this DataFile released?  Yes, don't need reminder
     *   (4) Does a map already exist?  Yes, don't need reminder
     */
    private boolean canSeeMapButtonReminderToPublish(FileMetadata fm, boolean permissionsChecked){
        if (fm==null){
            return false;
        } 
        
        // This is only here as a reminder to the public method users 
        if (!permissionsChecked){
            return false;
        }
        
        //  (1) Is the view GeoconnectViewMaps 
        if (!settingsService.isTrueForKey(SettingsServiceBean.Key.GeoconnectCreateEditMaps, false)){
            return false;
        }
        

        // (2) Is this file a Shapefile or a Tabular file tagged as Geospatial?
        //
        if (!(this.isPotentiallyMappableFileType(fm))){
            return false;
        }

        // (3) Is this DataFile released?  Yes, don't need reminder
        //
        if (fm.getDataFile().isReleased()){
            return false;
        }
        
        // (4) Does a map already exist?  Yes, don't need reminder
        //
        if (this.hasMapLayerMetadata(fm)){
            return false;
        }

        // Looks good
        //
        return true;
    }
    
     /**
     * 
     *  WARNING: Assumes user isAuthenicated AND has Permission.EditDataset
     *      - These checks are made on the DatasetPage which calls this method
     * 
     */ 
    public boolean canUserSeeMapDataButtonFromPage(FileMetadata fm){

        if (fm==null){
            return false;
        }         
        return this.canUserSeeMapDataButton(fm, true);
    }
            

    
    /**
     * Call this when using the API
     *   - calls private method canUserSeeMapDataButton
     * 
     * @param fm
     * @param user
     * @return 
     */
    public boolean canUserSeeMapDataButtonFromAPI(FileMetadata fm, User user){
        if (fm == null){
            return false;
        }
        if (user==null){
            return false;
        }
        
        if (!this.permissionService.userOn(user, this.dataset).has(Permission.EditDataset)){
            return false;
        }

        return this.canUserSeeMapDataButton(fm, true);
        
    }
    
     /**
     * 
     *  WARNING: Assumes user isAuthenicated AND has Permission.EditDataset
     *      - These checks are made on the DatasetPage which calls this method
     * 
     * Should there be a Map Data Button for this file?
     *  see table in: https://github.com/IQSS/dataverse/issues/1618
     *  (1) Is the user logged in?
     *  (2) Is this file a Shapefile or a Tabular file tagged as Geospatial?
     *  (3) Does the logged in user have permission to edit the Dataset to which this FileMetadata belongs?
     *  (4) Is the create Edit Maps flag set to true?
     *  (5) Any of these conditions:
     *        9a) File Published 
     *        (b) Draft: File Previously published  
     * @param fm FileMetadata
     * @return boolean
     */
    private boolean canUserSeeMapDataButton(FileMetadata fm, boolean permissionsChecked){

        if (fm==null){
            return false;
        }         
                 
        // This is only here as a reminder to the public method users 
        if (!permissionsChecked){
            return false;
        }
        
        //  (1) Is this file a Shapefile or a Tabular file tagged as Geospatial?
        //  TO DO:  EXPAND FOR TABULAR FILES TAGGED AS GEOSPATIAL!
        //
        if (!(this.isPotentiallyMappableFileType(fm))){
            return false;
        }


        //  (2) Is the view GeoconnectViewMaps 
        if (!settingsService.isTrueForKey(SettingsServiceBean.Key.GeoconnectCreateEditMaps, false)){
            return false;
        }
                     
        //  (3) Is File released?
        //
        if (fm.getDataFile().isReleased()){
            return true;
        }
        
        // Nope
        return false;
    }
    
    
}
