/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.export.ExportException;
import edu.harvard.iq.dataverse.export.ExportService;
import edu.harvard.iq.dataverse.privateurl.PrivateUrl;
import edu.harvard.iq.dataverse.search.IndexResponse;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.jsonAsDatasetDto;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.JsonObjectBuilder;

/**
 *
 * @author skraffmiller
 */
@RequiredPermissions(Permission.PublishDataset)
public class PublishDatasetCommand extends AbstractCommand<Dataset> {
    private static final Logger logger = Logger.getLogger(PublishDatasetCommand.class.getCanonicalName());
    private static final int FOOLPROOF_RETRIAL_ATTEMPTS_LIMIT = 2 ^ 8;

    boolean minorRelease = false;
    Dataset theDataset;

    /**
     * @todo Is there any use case where this command should allow the
     * publication of a "V0" version? Shouldn't the first published version of a
     * dataset be "V1"? Before a fix/workaround was introduced, it was possible
     * to use this command to create a published "V0" version. For details, see
     * https://github.com/IQSS/dataverse/issues/1392
     */
    public PublishDatasetCommand(Dataset datasetIn, DataverseRequest aRequest, boolean minor) {
        super(aRequest, datasetIn);
        logger.log(Level.FINE,"Constructor");
        minorRelease = minor;
        theDataset = datasetIn;
    }

    @Override
    public Dataset execute(CommandContext ctxt) throws CommandException {
        logger.log(Level.FINE,"execute");

        if (!theDataset.getOwner().isReleased()) {
            throw new IllegalCommandException("This dataset may not be published because its host dataverse (" + theDataset.getOwner().getAlias() + ") has not been published.", this);
        }

        if (theDataset.isLocked()) {
            throw new IllegalCommandException("This dataset is locked due to files being ingested. Please try publishing later.", this);
        }

        if (theDataset.getLatestVersion().isReleased()) {
            throw new IllegalCommandException("Latest version of dataset " + theDataset.getIdentifier() + " is already released. Only draft versions can be released.", this);
        }

        // prevent publishing of 0.1 version
        if (minorRelease && theDataset.getVersions().size() == 1 && theDataset.getLatestVersion().isDraft()) {
            throw new IllegalCommandException("Cannot publish as minor version. Re-try as major release.", this);
        }

        if (minorRelease && !theDataset.getLatestVersion().isMinorUpdate()) {
            throw new IllegalCommandException("Cannot release as minor version. Re-try as major release.", this);
        }
        /* make an attempt to register if not registered */
        String nonNullDefaultIfKeyNotFound = "";
        String protocol = theDataset.getProtocol();
        String doiProvider = ctxt.settings().getValueForKey(SettingsServiceBean.Key.DoiProvider, nonNullDefaultIfKeyNotFound);
        String authority = theDataset.getAuthority();
        IdServiceBean idServiceBean = IdServiceBean.getBean(protocol, ctxt);
        logger.log(Level.FINE,"doiProvider={0} protocol={1} GlobalIdCreateTime=={2}", new Object[]{doiProvider, protocol, theDataset.getGlobalIdCreateTime()});
        if (theDataset.getGlobalIdCreateTime() == null) {
            if (idServiceBean!=null) {
                try {
                    if (!idServiceBean.alreadyExists(theDataset)) {
                        idServiceBean.createIdentifier(theDataset);
                        theDataset.setGlobalIdCreateTime(new Timestamp(new Date().getTime()));
                    } else {
                        int attempts = 0;
                       
                        while (idServiceBean.alreadyExists(theDataset) && attempts < FOOLPROOF_RETRIAL_ATTEMPTS_LIMIT) {
                            theDataset.setIdentifier(ctxt.datasets().generateDatasetIdentifier(theDataset, idServiceBean));
                            attempts++;
                        }
                        
                        if (idServiceBean.alreadyExists(theDataset)) {
                            throw new IllegalCommandException("This dataset may not be published because its identifier is already in use by another dataset;gave up after " + attempts + " attempts. Current (last requested) identifier: " + theDataset.getIdentifier(), this);
                        }
                        idServiceBean.createIdentifier(theDataset);
                        theDataset.setGlobalIdCreateTime(new Timestamp(new Date().getTime()));
                        
                        }
                   
                } catch (Throwable e) {
                    throw new CommandException(BundleUtil.getStringFromBundle("dataset.publish.error", idServiceBean.getProviderInformation()),this); 
                }
            } else {
                throw new IllegalCommandException("This dataset may not be published because its id registry service is not supported.", this);
            }
        }
        
        if (theDataset.getPublicationDate() == null) {
            //Before setting dataset to released send notifications to users with download file
            List<RoleAssignment> ras = ctxt.roles().directRoleAssignments(theDataset);
            for (RoleAssignment ra : ras) {
                if (ra.getRole().permissions().contains(Permission.DownloadFile)) {
                    for (AuthenticatedUser au : ctxt.roleAssignees().getExplicitUsers(ctxt.roleAssignees().getRoleAssignee(ra.getAssigneeIdentifier()))) {
                        ctxt.notifications().sendNotification(au, new Timestamp(new Date().getTime()), UserNotification.Type.ASSIGNROLE, theDataset.getId());
                    }
                }
            }
            theDataset.setPublicationDate(new Timestamp(new Date().getTime()));
            theDataset.setReleaseUser((AuthenticatedUser) getUser());
            if (!minorRelease) {
                theDataset.getEditVersion().setVersionNumber(new Long(1));
                theDataset.getEditVersion().setMinorVersionNumber(new Long(0));
            } else {
                theDataset.getEditVersion().setVersionNumber(new Long(0));
                theDataset.getEditVersion().setMinorVersionNumber(new Long(1));
            }
        } else if (!minorRelease) {
            theDataset.getEditVersion().setVersionNumber(new Long(theDataset.getVersionNumber() + 1));
            theDataset.getEditVersion().setMinorVersionNumber(new Long(0));
        } else {
            theDataset.getEditVersion().setVersionNumber(new Long(theDataset.getVersionNumber()));
            theDataset.getEditVersion().setMinorVersionNumber(new Long(theDataset.getMinorVersionNumber() + 1));
        }

        Timestamp updateTime = new Timestamp(new Date().getTime());
        theDataset.getEditVersion().setReleaseTime(updateTime);
        theDataset.getEditVersion().setLastUpdateTime(updateTime);
        theDataset.setModificationTime(updateTime);
        //set inReview to False because it is now published
        theDataset.getEditVersion().setInReview(false);
        // The reason for return is water under the bridge. Null it out.
        theDataset.getEditVersion().setReturnReason(null);
        theDataset.getEditVersion().setVersionState(DatasetVersion.VersionState.RELEASED);

        for (DataFile dataFile : theDataset.getFiles()) {
            if (dataFile.getPublicationDate() == null) {
                // this is a new, previously unpublished file, so publish by setting date
                dataFile.setPublicationDate(updateTime);

                // check if any prexisting roleassignments have file download and send notifications
                List<RoleAssignment> ras = ctxt.roles().directRoleAssignments(dataFile);
                for (RoleAssignment ra : ras) {
                    if (ra.getRole().permissions().contains(Permission.DownloadFile)) {
                        for (AuthenticatedUser au : ctxt.roleAssignees().getExplicitUsers(ctxt.roleAssignees().getRoleAssignee(ra.getAssigneeIdentifier()))) {
                            ctxt.notifications().sendNotification(au, new Timestamp(new Date().getTime()), UserNotification.Type.GRANTFILEACCESS, theDataset.getId());
                        }
                    }
                }
            }

            // set the files restriction flag to the same as the latest version's
            if (dataFile.getFileMetadata() != null && dataFile.getFileMetadata().getDatasetVersion().equals(theDataset.getLatestVersion())) {
                dataFile.setRestricted(dataFile.getFileMetadata().isRestricted());
            }
            
            
            if (dataFile.isRestricted()) {
                // A couple things need to happen if the file has been restricted: 
                // 1. If there's a map layer associated with this shape file, or 
                //    tabular-with-geo-tag file, all that map layer data (that 
                //    includes most of the actual data in the file!) need to be
                //    removed from WorldMap and GeoConnect, since anyone can get 
                //    download the data from there;
                // 2. If this (image) file has been assigned as the dedicated 
                //    thumbnail for the dataset, we need to remove that assignment, 
                //    now that the file is restricted. 

                // Map layer: 
                
                if (ctxt.mapLayerMetadata().findMetadataByDatafile(dataFile) != null) {
                    // (We need an AuthenticatedUser in order to produce a WorldMap token!)
                    String id = getUser().getIdentifier();
                    id = id.startsWith("@") ? id.substring(1) : id;
                    AuthenticatedUser authenticatedUser = ctxt.authentication().getAuthenticatedUser(id);
                    try {
                        logger.fine("(1 of 2) PublishDatasetCommand: delete MapLayer From *WorldMap*");
                        ctxt.mapLayerMetadata().deleteMapLayerFromWorldMap(dataFile, authenticatedUser);

                        // If that was successful, delete the layer on the Dataverse side as well:
                        //SEK 4/20/2017                
                        //Command to delete from Dataverse side
                        logger.fine("(2 of 2) PublishDatasetCommand: Delete MapLayerMetadata From *Dataverse*");
                        boolean deleteMapSuccess = ctxt.engine().submit(new DeleteMapLayerMetadataCommand(this.getRequest(), dataFile));

                        // RP - Bit of hack, update the datafile here b/c the reference to the datafile 
                        // is not being passed all the way up/down the chain.   
                        //
                        dataFile.setPreviewImageAvailable(false);

                    } catch (IOException ioex) {
                        // We are not going to treat it as a fatal condition and bail out, 
                        // but we will send a notification to the user, warning them about
                        // the layer still being out there, un-deleted:
                        ctxt.notifications().sendNotification(authenticatedUser, new Timestamp(new Date().getTime()), UserNotification.Type.MAPLAYERDELETEFAILED, dataFile.getFileMetadata().getId());
                    }

                }
                
                // Dataset thumbnail assignment: 
                
                if (dataFile.equals(theDataset.getThumbnailFile())) {
                    theDataset.setThumbnailFile(null);
                }
            }
        }

        theDataset.setFileAccessRequest(theDataset.getLatestVersion().getTermsOfUseAndAccess().isFileAccessRequest());
        
        
        /*
            Attempting to run metadata export, for all the formats for which 
            we have metadata Exporters:
        */
        
        try {
            ExportService instance = ExportService.getInstance(ctxt.settings());
            instance.exportAllFormats(theDataset);

        } catch (ExportException ex) {
            // Something went wrong!  
            // Just like with indexing, a failure to export is not a fatal 
            // condition. We'll just log the error as a warning and keep 
            // going: 
            logger.log(Level.WARNING, "Exception while exporting:" + ex.getMessage());
        }
        
        
        Dataset savedDataset = ctxt.em().merge(theDataset);

        // set the subject of the parent (all the way up) Dataverses
        DatasetField subject = null;
        for (DatasetField dsf : savedDataset.getLatestVersion().getDatasetFields()) {
            if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.subject)) {
                subject = dsf;
                break;
            }
        }
        if (subject != null) {
            Dataverse dv = savedDataset.getOwner();
            while (dv != null) {
                if (dv.getDataverseSubjects().addAll(subject.getControlledVocabularyValues())) {
                    Dataverse dvWithSubjectJustAdded = ctxt.em().merge(dv);
                    ctxt.em().flush();
                    ctxt.index().indexDataverse(dvWithSubjectJustAdded); // need to reindex to capture the new subjects
                }
                dv = dv.getOwner();
            }
        }

        DatasetVersionUser ddu = ctxt.datasets().getDatasetVersionUser(savedDataset.getLatestVersion(), this.getUser());

        if (ddu != null) {
            ddu.setLastUpdateDate(updateTime);
            ctxt.em().merge(ddu);
        } else {
            DatasetVersionUser datasetDataverseUser = new DatasetVersionUser();
            datasetDataverseUser.setDatasetVersion(savedDataset.getLatestVersion());
            datasetDataverseUser.setLastUpdateDate((Timestamp) updateTime);
            String id = getUser().getIdentifier();
            id = id.startsWith("@") ? id.substring(1) : id;
            AuthenticatedUser au = ctxt.authentication().getAuthenticatedUser(id);
            datasetDataverseUser.setAuthenticatedUser(au);
            ctxt.em().merge(datasetDataverseUser);
        }

        if (idServiceBean!= null )
            try{
                idServiceBean.publicizeIdentifier(savedDataset);
            }catch (Throwable e) {
                throw new CommandException(BundleUtil.getStringFromBundle("dataset.publish.error", idServiceBean.getProviderInformation()),this); 
            }
        PrivateUrl privateUrl = ctxt.engine().submit(new GetPrivateUrlCommand(getRequest(), savedDataset));
        if (privateUrl != null) {
            logger.fine("Deleting Private URL for dataset id " + savedDataset.getId());
            ctxt.engine().submit(new DeletePrivateUrlCommand(getRequest(), savedDataset));
        }

        /*
        MoveIndexing to after DOI update so that if command exception is thrown the re-index will not
        
         */
        
        boolean doNormalSolrDocCleanUp = true;
        ctxt.index().indexDataset(savedDataset, doNormalSolrDocCleanUp);
        /**
         * @todo consider also ctxt.solrIndex().indexPermissionsOnSelfAndChildren(theDataset);
         */
        /**
         * @todo what should we do with the indexRespose?
         */
        IndexResponse indexResponse = ctxt.solrIndex().indexPermissionsForOneDvObject(savedDataset);

        return savedDataset;
    }    

}
