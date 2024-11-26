/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.dataaccess.ImageThumbConverter;
import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import edu.harvard.iq.dataverse.dataset.DatasetUtil;
import edu.harvard.iq.dataverse.search.SolrSearchResult;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 *
 * @author Leonid Andreev
 */
//@ViewScoped
@RequestScoped
@Named
public class ThumbnailServiceWrapper implements java.io.Serializable  {
    
    private static final Logger logger = Logger.getLogger(ThumbnailServiceWrapper.class.getCanonicalName());
    
    @Inject
    PermissionsWrapper permissionsWrapper;
    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    DatasetServiceBean datasetService;
    @EJB
    DatasetVersionServiceBean datasetVersionService;
    @EJB
    DataFileServiceBean dataFileService;
    
    private Map<Long, String> dvobjectThumbnailsMap = new HashMap<>();
    private Map<Long, DvObject> dvobjectViewMap = new HashMap<>();
    private Map<Long, Boolean> hasThumbMap = new HashMap<>();

    public String getFileCardImageAsUrl(SolrSearchResult result) {
        DataFile dataFile = result != null && result.getEntity() != null ? ((DataFile) result.getEntity()) : null;
        if (dataFile == null || result.isHarvested()
                || !isThumbnailAvailable(dataFile)
                || dataFile.isRestricted()
                || !dataFile.isReleased()
                || FileUtil.isActivelyEmbargoed(dataFile)
                || FileUtil.isRetentionExpired(dataFile)) {
            return null;
        }
        return SystemConfig.getDataverseSiteUrlStatic() + "/api/access/datafile/" + dataFile.getId() + "?imageThumb=true";
    }

    // it's the responsibility of the user - to make sure the search result
    // passed to this method is of the Datafile type!
    public String getFileCardImageAsBase64Url(SolrSearchResult result) {
        // Before we do anything else, check if it's a harvested dataset; 
        // no need to check anything else if so (harvested objects never have 
        // thumbnails)
        
        if (result.isHarvested()) {
            return null; 
        }

        if (result.getEntity() == null) {
            return null;
        }
        
        Long imageFileId = result.getEntity().getId();

        if (imageFileId != null) {
            if (this.dvobjectThumbnailsMap.containsKey(imageFileId)) {
                // Yes, return previous answer
                //logger.info("using cached result for ... "+datasetId);
                if (!"".equals(this.dvobjectThumbnailsMap.get(imageFileId))) {
                    return this.dvobjectThumbnailsMap.get(imageFileId);
                }
                return null;
            }

            String cardImageUrl = null;
            
            if (result.getTabularDataTags() != null) {
                for (String tabularTagLabel : result.getTabularDataTags()) {
                    DataFileTag tag = new DataFileTag();
                    try {
                        tag.setTypeByLabel(tabularTagLabel);
                        tag.setDataFile((DataFile) result.getEntity());
                        ((DataFile) result.getEntity()).addTag(tag);
                    } catch (IllegalArgumentException iax) {
                        // ignore 
                    }
                }
            }

            if ((!((DataFile)result.getEntity()).isRestricted()
                        || permissionsWrapper.hasDownloadFilePermission(result.getEntity()))
                    && isThumbnailAvailable((DataFile) result.getEntity())) {
                
                cardImageUrl = ImageThumbConverter.getImageThumbnailAsBase64(
                        (DataFile) result.getEntity(),
                        ImageThumbConverter.DEFAULT_CARDIMAGE_SIZE);
            }

            if (cardImageUrl != null) {
                this.dvobjectThumbnailsMap.put(imageFileId, cardImageUrl);
                //logger.info("datafile id " + imageFileId + ", returning " + cardImageUrl);

                if (!(dvobjectViewMap.containsKey(imageFileId)
                        && dvobjectViewMap.get(imageFileId).isInstanceofDataFile())) {

                    dvobjectViewMap.put(imageFileId, result.getEntity());

                }

                return cardImageUrl;
            } else {
                this.dvobjectThumbnailsMap.put(imageFileId, "");
            }
        }
        return null;
    }

    public boolean isThumbnailAvailable(DataFile entity) {
        if(!hasThumbMap.containsKey(entity.getId())) {
            hasThumbMap.put(entity.getId(), dataFileService.isThumbnailAvailable(entity));
        }
        return hasThumbMap.get(entity.getId());
    }

    // it's the responsibility of the user - to make sure the search result
    // passed to this method is of the Dataset type!
    public String getDatasetCardImageAsUrl(SolrSearchResult result) {
        // Before we do anything else, check if it's a harvested dataset; 
        // no need to check anything else if so (harvested datasets never have 
        // thumbnails)

        if (result.isHarvested()) {
            return null; 
        }
        
        // Check if the search result ("card") contains an entity, before 
        // attempting to convert it to a Dataset. It occasionally happens that 
        // solr has indexed datasets that are no longer in the database. If this
        // is the case, the entity will be null here; and proceeding any further
        // results in a long stack trace in the log file. 
        if (result.getEntity() == null) {
            return null;
        }
        Dataset dataset = (Dataset)result.getEntity();
        dataset.setId(result.getEntityId());
        
        Long versionId = result.getDatasetVersionId();

        return getDatasetCardImageAsUrl(dataset, versionId, result.isPublishedState(), ImageThumbConverter.DEFAULT_CARDIMAGE_SIZE);
    }
    
    public String getDatasetCardImageAsUrl(Dataset dataset, Long versionId, boolean autoselect, int size) {
        Long datasetId = dataset.getId();
        if (datasetId != null) {
            if (this.dvobjectThumbnailsMap.containsKey(datasetId)) {
                // Yes, return previous answer
                // (at max, there could only be 2 cards for the same dataset
                // on the page - the draft, and the published version; but it's 
                // still nice to try and cache the result - especially if it's an
                // uploaded logo - we don't want to read it off disk twice). 
                
                if (!"".equals(this.dvobjectThumbnailsMap.get(datasetId))) {
                    return this.dvobjectThumbnailsMap.get(datasetId);
                }
                return null;
            }
        }

        if (dataset.isUseGenericThumbnail()) {
            this.dvobjectThumbnailsMap.put(datasetId, "");
            return null; 
        }
        DataFile thumbnailFile = dataset.getThumbnailFile();

        if (thumbnailFile == null) {

            boolean hasDatasetLogo = false;
            StorageIO<DvObject> storageIO = null;
            try {
                storageIO = DataAccess.getStorageIO(dataset);
                if (storageIO != null && storageIO.isAuxObjectCached(DatasetUtil.datasetLogoFilenameFinal)) {
                    // If not, return null/use the default, otherwise pass the logo URL
                    hasDatasetLogo = true;
                }
            } catch (IOException ioex) {
                logger.warning("getDatasetCardImageAsUrl(): Failed to initialize dataset StorageIO for "
                        + dataset.getStorageIdentifier() + " (" + ioex.getMessage() + ")");
            }
            // If no other logo we attempt to auto-select via the optimized, native
            // query-based method
            // from the DatasetVersionService:
            if (!hasDatasetLogo && datasetVersionService.getThumbnailByVersionId(versionId) == null) {
                return null;
            }
        }
        String url = SystemConfig.getDataverseSiteUrlStatic() + "/api/datasets/" + dataset.getId() + "/logo";
        logger.fine("getDatasetCardImageAsUrl: " + url);
        this.dvobjectThumbnailsMap.put(datasetId,url);
        return url;
        
    }
    
    // it's the responsibility of the user - to make sure the search result
    // passed to this method is of the Dataverse type!
    public String getDataverseCardImageAsBase64Url(SolrSearchResult result) {
        return dataverseService.getDataverseLogoThumbnailAsBase64ById(result.getEntityId());
    }

    // it's the responsibility of the user - to make sure the search result
    // passed to this method is of the Dataverse type!
    public String getDataverseCardImageAsUrl(SolrSearchResult result) {
        return dataverseService.getDataverseLogoThumbnailAsUrl(result.getEntityId());
    }

    public void resetObjectMaps() {
        dvobjectThumbnailsMap = new HashMap<>();
        dvobjectViewMap = new HashMap<>();
        hasThumbMap = new HashMap<>();
    }

    
}
