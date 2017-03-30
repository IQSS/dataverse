/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldConstant;
import edu.harvard.iq.dataverse.DatasetVersionUser;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.UserNotification;
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
        minorRelease = minor;
        theDataset = datasetIn;
    }

    @Override
    public Dataset execute(CommandContext ctxt) throws CommandException {

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
        if (theDataset.getGlobalIdCreateTime() == null) {
            if (protocol.equals("doi")
                    && (doiProvider.equals("EZID") || doiProvider.equals("DataCite"))) {
                String doiRetString = "";
                
                // Whether it's EZID or DataCiteif, if the registration is 
                // refused because the identifier already exists, we'll generate another one
                // and try to register again... but only up to some
                // reasonably high number of times - so that we don't 
                // go into an infinite loop here, if EZID is giving us 
                // these duplicate messages in error. 
                // 
                // (and we do want the limit to be a "reasonably high" number! 
                // true, if our identifiers are randomly generated strings, 
                // then it is highly unlikely that we'll ever run into a 
                // duplicate race condition repeatedly; but if they are sequential
                // numeric values, than it is entirely possible that a large
                // enough number of values will be legitimately registered 
                // by another entity sharing the same authority...)
                
                int attempts = 0;
                
                if (doiProvider.equals("EZID")) {
                    doiRetString = ctxt.doiEZId().createIdentifier(theDataset);
                    
                    while (!doiRetString.contains(theDataset.getIdentifier())
                            && doiRetString.contains("identifier already exists")
                            && attempts < FOOLPROOF_RETRIAL_ATTEMPTS_LIMIT) {

                        theDataset.setIdentifier(ctxt.datasets().generateDatasetIdentifier(theDataset.getProtocol(), theDataset.getAuthority(), theDataset.getDoiSeparator()));
                        doiRetString = ctxt.doiEZId().createIdentifier(theDataset);

                        attempts++;

                    }

                    if (doiRetString.contains(theDataset.getIdentifier())) {
                        theDataset.setGlobalIdCreateTime(new Timestamp(new Date().getTime()));
                    } else if (doiRetString.contains("identifier already exists")) {
                        throw new IllegalCommandException("EZID refused registration, requested id(s) already in use; gave up after " + attempts + " attempts. Current (last requested) identifier: " + theDataset.getIdentifier(), this);
                    } else {
                        throw new IllegalCommandException("Failed to create identifier (" + theDataset.getIdentifier() + ") with EZID: " + doiRetString, this);
                    }                    
                }
                
                if (doiProvider.equals("DataCite")) {
                    try {
                        while (ctxt.doiDataCite().alreadyExists(theDataset) && attempts < FOOLPROOF_RETRIAL_ATTEMPTS_LIMIT) {
                            theDataset.setIdentifier(ctxt.datasets().generateDatasetIdentifier(protocol, authority, theDataset.getDoiSeparator()));
                        }
                        
                        if (ctxt.doiDataCite().alreadyExists(theDataset)) {
                            throw new IllegalCommandException("DataCite refused registration, requested id(s) already in use; gave up after " + attempts + " attempts. Current (last requested) identifier: " + theDataset.getIdentifier(), this);
                        }
                        ctxt.doiDataCite().createIdentifier(theDataset);
                        theDataset.setGlobalIdCreateTime(new Timestamp(new Date().getTime()));
                    } catch (Exception e) {
                        throw new CommandException(ResourceBundle.getBundle("Bundle").getString("dataset.publish.error.datacite"), this);
                    }
                }
            } else {
                throw new IllegalCommandException("This dataset may not be published because its DOI provider is not supported. Please contact Dataverse Support for assistance.", this);
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
        }

        theDataset.setFileAccessRequest(theDataset.getLatestVersion().getTermsOfUseAndAccess().isFileAccessRequest());
        
        
        /*
            Attempting to run metadata export, for all the formats for which 
            we have metadata Exporters:
        */
        
        try {
            ExportService instance = ExportService.getInstance();
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

        if (protocol.equals("doi")
                && doiProvider.equals("EZID")) {
            ctxt.doiEZId().publicizeIdentifier(savedDataset);
        }
        if (protocol.equals("doi")
                && doiProvider.equals("DataCite")) {
            try {
                ctxt.doiDataCite().publicizeIdentifier(savedDataset);
            } catch (IOException io) {
                throw new CommandException(ResourceBundle.getBundle("Bundle").getString("dataset.publish.error.datacite"), this);
            } catch (Exception e) {
                if (e.toString().contains("Internal Server Error")) {
                    throw new CommandException(ResourceBundle.getBundle("Bundle").getString("dataset.publish.error.datacite"), this);
                }
                throw new CommandException(ResourceBundle.getBundle("Bundle").getString("dataset.publish.error.datacite"), this);
            }
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
