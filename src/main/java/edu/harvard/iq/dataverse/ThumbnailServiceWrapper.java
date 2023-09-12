/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.api.Datasets;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import edu.harvard.iq.dataverse.dataset.DatasetUtil;
import edu.harvard.iq.dataverse.dataaccess.ImageThumbConverter;

import static edu.harvard.iq.dataverse.dataset.DatasetUtil.datasetLogoThumbnail;
import edu.harvard.iq.dataverse.search.SolrSearchResult;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.commons.io.IOUtils;

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

    private String getAssignedDatasetImage(Dataset dataset, int size) {
        if (dataset == null) {
            return null;
        }

        DataFile assignedThumbnailFile = dataset.getThumbnailFile();

        if (assignedThumbnailFile != null) {
            Long assignedThumbnailFileId = assignedThumbnailFile.getId();

            if (this.dvobjectThumbnailsMap.containsKey(assignedThumbnailFileId)) {
                // Yes, return previous answer
                //logger.info("using cached result for ... "+assignedThumbnailFileId);
                if (!"".equals(this.dvobjectThumbnailsMap.get(assignedThumbnailFileId))) {
                    return this.dvobjectThumbnailsMap.get(assignedThumbnailFileId);
                }
                return null;
            }

            String imageSourceBase64 = ImageThumbConverter.getImageThumbnailAsBase64(assignedThumbnailFile,
                    size);
                    //ImageThumbConverter.DEFAULT_CARDIMAGE_SIZE);

            if (imageSourceBase64 != null) {
                this.dvobjectThumbnailsMap.put(assignedThumbnailFileId, imageSourceBase64);
                return imageSourceBase64;
            }

            // OK - we can't use this "assigned" image, because of permissions, or because 
            // the thumbnail failed to generate, etc... in this case we'll 
            // mark this dataset in the lookup map - so that we don't have to
            // do all these lookups again...
            this.dvobjectThumbnailsMap.put(assignedThumbnailFileId, "");
            
            // TODO: (?)
            // do we need to cache this datafile object in the view map?
            // -- L.A., 4.2.2
        }

        return null;

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

            // We attempt to auto-select via the optimized, native query-based method
            // from the DatasetVersionService:
            if (datasetVersionService.getThumbnailByVersionId(versionId) == null) {
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
    
    public void resetObjectMaps() {
        dvobjectThumbnailsMap = new HashMap<>();
        dvobjectViewMap = new HashMap<>();
        hasThumbMap = new HashMap<>();
    }

    
}
