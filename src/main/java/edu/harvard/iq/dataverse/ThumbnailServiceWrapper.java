/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.dataaccess.ImageThumbConverter;
import edu.harvard.iq.dataverse.dataset.DatasetUtil;
import edu.harvard.iq.dataverse.search.SolrSearchResult;
import edu.harvard.iq.dataverse.util.FileUtil;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author Leonid Andreev
 */
@ViewScoped
@Named
public class ThumbnailServiceWrapper implements java.io.Serializable  {
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

    private String getAssignedDatasetImage(Dataset dataset) {
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

            String imageSourceBase64 = ImageThumbConverter.getImageThumbnailAsBase64(
                    assignedThumbnailFile,
                    ImageThumbConverter.DEFAULT_CARDIMAGE_SIZE);

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
                    && dataFileService.isThumbnailAvailable((DataFile) result.getEntity())) {
                
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

    // it's the responsibility of the user - to make sure the search result
    // passed to this method is of the Dataset type!
    public String getDatasetCardImageAsBase64Url(SolrSearchResult result) {
        // Before we do anything else, check if it's a harvested dataset; 
        // no need to check anything else if so (harvested datasets never have 
        // thumbnails)
        
        if (result.isHarvested()) {
            return null; 
        }
        
        
        Dataset dataset = (Dataset)result.getEntity();
        Long versionId = result.getDatasetVersionId();

        return getDatasetCardImageAsBase64Url(dataset, versionId);
    }
    public String getDatasetCardImageAsBase64Url(Dataset dataset, Long versionId) {
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
        
        String cardImageUrl = null;
        
        Path path = Paths.get(dataset.getFileSystemDirectory() + File.separator + DatasetUtil.datasetLogoThumbnail + DatasetUtil.thumb48addedByImageThumbConverter);
        if (Files.exists(path)) {
            try {
                byte[] bytes = Files.readAllBytes(path);
                String base64image = Base64.getEncoder().encodeToString(bytes);
                cardImageUrl = FileUtil.DATA_URI_SCHEME + base64image;
                this.dvobjectThumbnailsMap.put(datasetId, cardImageUrl);
                return cardImageUrl;
            } catch (IOException ex) {
                this.dvobjectThumbnailsMap.put(datasetId, "");
                return null; 
                // (alternatively, we could ignore the exception, and proceed with the 
                // regular process of selecting the thumbnail from the available 
                // image files - ?)
            }
        } 

        if (dataset != null) {
            cardImageUrl = this.getAssignedDatasetImage(dataset);

            if (cardImageUrl != null) {
                //logger.info("dataset id " + result.getEntity().getId() + " has a dedicated image assigned; returning " + cardImageUrl);
                return cardImageUrl;
            }
        }

        Long thumbnailImageFileId = datasetVersionService.getThumbnailByVersionId(versionId);

        if (thumbnailImageFileId != null) {
            //cardImageUrl = FILE_CARD_IMAGE_URL + thumbnailImageFileId;
            if (this.dvobjectThumbnailsMap.containsKey(thumbnailImageFileId)) {
                // Yes, return previous answer
                //logger.info("using cached result for ... "+datasetId);
                if (!"".equals(this.dvobjectThumbnailsMap.get(thumbnailImageFileId))) {
                    return this.dvobjectThumbnailsMap.get(thumbnailImageFileId);
                }
                return null;
            }

            DataFile thumbnailImageFile = null;

            if (dvobjectViewMap.containsKey(thumbnailImageFileId)
                    && dvobjectViewMap.get(thumbnailImageFileId).isInstanceofDataFile()) {
                thumbnailImageFile = (DataFile) dvobjectViewMap.get(thumbnailImageFileId);
            } else {
                thumbnailImageFile = dataFileService.findCheapAndEasy(thumbnailImageFileId);
                if (thumbnailImageFile != null) {
                    // TODO:
                    // do we need this file on the map? - it may not even produce
                    // a thumbnail!
                    dvobjectViewMap.put(thumbnailImageFileId, thumbnailImageFile);
                } else {
                    this.dvobjectThumbnailsMap.put(thumbnailImageFileId, "");
                    return null;
                }
            }

            if (dataFileService.isThumbnailAvailable(thumbnailImageFile)) {
                cardImageUrl = ImageThumbConverter.getImageThumbnailAsBase64(
                        thumbnailImageFile,
                        ImageThumbConverter.DEFAULT_CARDIMAGE_SIZE);
            }

            if (cardImageUrl != null) {
                this.dvobjectThumbnailsMap.put(thumbnailImageFileId, cardImageUrl);
            } else {
                this.dvobjectThumbnailsMap.put(thumbnailImageFileId, "");
            }
        }

        //logger.info("dataset id " + result.getEntityId() + ", returning " + cardImageUrl);

        return cardImageUrl;
    }
    
    // it's the responsibility of the user - to make sure the search result
    // passed to this method is of the Dataverse type!
    public String getDataverseCardImageAsBase64Url(SolrSearchResult result) {
        return dataverseService.getDataverseLogoThumbnailAsBase64ById(result.getEntityId());
    }
    
    public void resetObjectMaps() {
        dvobjectThumbnailsMap = new HashMap<>();
        dvobjectViewMap = new HashMap<>();
    }

    
}
