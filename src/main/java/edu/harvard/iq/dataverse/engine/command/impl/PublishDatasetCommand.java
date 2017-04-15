package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersionUser;
import edu.harvard.iq.dataverse.DvObject;
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
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

/**
 *
 * @author skraffmiller
 * @author michbarsinai
 */
@RequiredPermissions(Permission.PublishDataset)
public class PublishDatasetCommand extends AbstractCommand<Dataset> {
    private static final String DEFAULT_DOI_PROVIDER_KEY = "";
    boolean minorRelease = false;
    Dataset theDataset;

    public PublishDatasetCommand(Dataset datasetIn, DataverseRequest aRequest, boolean minor) {
        super(aRequest, datasetIn);
        minorRelease = minor;
        theDataset = datasetIn;
    }

    @Override
    public Dataset execute(CommandContext ctxt) throws CommandException {

        verifyCommandArguments();

        String doiProvider = ctxt.settings().getValueForKey(SettingsServiceBean.Key.DoiProvider, DEFAULT_DOI_PROVIDER_KEY);
        registerExternalIdentifier(theDataset, ctxt, doiProvider);
        
        if (theDataset.getPublicationDate() == null) {
            // First Release
            // Send notifications to users with download file permission
            notifyUsers(ctxt, theDataset, UserNotification.Type.ASSIGNROLE);
            
            theDataset.setReleaseUser((AuthenticatedUser) getUser());
            theDataset.setPublicationDate(new Timestamp(new Date().getTime()));
            theDataset.getEditVersion().setVersionNumber(new Long(1)); // minor release is blocked by #verifyCommandArguments
            theDataset.getEditVersion().setMinorVersionNumber(new Long(0));
            
        } else if ( minorRelease ) {
            theDataset.getEditVersion().setVersionNumber(new Long(theDataset.getVersionNumber()));
            theDataset.getEditVersion().setMinorVersionNumber(new Long(theDataset.getMinorVersionNumber() + 1));
            
        } else /* major, non-first, release */ {
            theDataset.getEditVersion().setVersionNumber(new Long(theDataset.getVersionNumber() + 1));
            theDataset.getEditVersion().setMinorVersionNumber(new Long(0));
        }

        // update metadata
        Timestamp updateTime = new Timestamp(new Date().getTime());
        theDataset.getEditVersion().setReleaseTime(updateTime);
        theDataset.getEditVersion().setLastUpdateTime(updateTime);
        theDataset.setModificationTime(updateTime);
        theDataset.getEditVersion().setInReview(false); // TODO SBG replace by updating lock reason
        theDataset.setFileAccessRequest(theDataset.getLatestVersion().getTermsOfUseAndAccess().isFileAccessRequest());

        updateFiles(updateTime, ctxt);
        
        theDataset = ctxt.em().merge(theDataset);

        DatasetVersionUser ddu = ctxt.datasets().getDatasetVersionUser(theDataset.getLatestVersion(), getUser());

        if (ddu == null) {
            ddu = new DatasetVersionUser();
            ddu.setDatasetVersion(theDataset.getLatestVersion());
            ddu.setAuthenticatedUser((AuthenticatedUser) getUser()); // safe, as user is verified as authenticated in #verifyCommandArguments
        }
        ddu.setLastUpdateDate((Timestamp) updateTime);
        ctxt.em().merge(ddu);
        
        // -- workflow may go here
       
        theDataset = ctxt.engine().submit( new FinalizeDatasetPublicationCommand(theDataset, doiProvider, getRequest()));
        return ctxt.em().merge(theDataset);
    }

    private void updateFiles(Timestamp updateTime, CommandContext ctxt) {
        for (DataFile dataFile : theDataset.getFiles()) {
            if (dataFile.getPublicationDate() == null) {
                // this is a new, previously unpublished file, so publish by setting date
                dataFile.setPublicationDate(updateTime);
                
                // check if any prexisting roleassignments have file download and send notifications
                notifyUsers(ctxt, dataFile, UserNotification.Type.GRANTFILEACCESS);
            }
            
            // set the files restriction flag to the same as the latest version's
            if (dataFile.getFileMetadata() != null && dataFile.getFileMetadata().getDatasetVersion().equals(theDataset.getLatestVersion())) {
                dataFile.setRestricted(dataFile.getFileMetadata().isRestricted());
            }
        }
    }

    /**
     * See that publishing the dataset in the requested manner makes sense, at
     * the given state of the dataset.
     * 
     * @throws IllegalCommandException if the publication request is invalid.
     */
    private void verifyCommandArguments() throws IllegalCommandException {
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
        
        if ( ! getUser().isAuthenticated() ) {
            throw new IllegalCommandException("Only authenticated users can release a Dataset. Please authenticate and try again.", this);
        }
    }

    
    private void notifyUsers(CommandContext ctxt, DvObject subject, UserNotification.Type messageType ) {
        List<RoleAssignment> ras = ctxt.roles().directRoleAssignments(subject);
        for (RoleAssignment ra : ras) {
            if (ra.getRole().permissions().contains(Permission.DownloadFile)) {
                for (AuthenticatedUser au : ctxt.roleAssignees().getExplicitUsers(ctxt.roleAssignees().getRoleAssignee(ra.getAssigneeIdentifier()))) {
                    ctxt.notifications().sendNotification(au, new Timestamp(new Date().getTime()), messageType, theDataset.getId());
                }
            }
        }
    }
    
    private void registerExternalIdentifier(Dataset theDataset, CommandContext ctxt, String doiProvider) throws CommandException {
        String protocol = theDataset.getProtocol();
        String authority = theDataset.getAuthority();
        if (theDataset.getGlobalIdCreateTime() == null) {
            if (protocol.equals("doi")) {
                switch( doiProvider ) {
                    case "EZID":
                        String doiRetString = ctxt.doiEZId().createIdentifier(theDataset);
                        if (doiRetString.contains(theDataset.getIdentifier())) {
                            theDataset.setGlobalIdCreateTime(new Timestamp(new Date().getTime()));
                        } else if (doiRetString.contains("identifier already exists")) {
                            theDataset.setIdentifier(ctxt.datasets().generateIdentifierSequence(protocol, authority, theDataset.getDoiSeparator()));
                            doiRetString = ctxt.doiEZId().createIdentifier(theDataset);
                            if (!doiRetString.contains(theDataset.getIdentifier())) {
                                throw new IllegalCommandException("This dataset may not be published because its identifier is already in use by another dataset. Please contact Dataverse Support for assistance.", this);
                            } else {
                                theDataset.setGlobalIdCreateTime(new Timestamp(new Date().getTime()));
                            }
                        } else {
                            throw new IllegalCommandException("This dataset may not be published because it has not been registered. Please contact Dataverse Support for assistance.", this);
                        }
                        break;
                        
                    case "DataCite":
                        try {
                            if (!ctxt.doiDataCite().alreadyExists(theDataset)) {
                                ctxt.doiDataCite().createIdentifier(theDataset);
                                theDataset.setGlobalIdCreateTime(new Timestamp(new Date().getTime()));
                            } else {
                                theDataset.setIdentifier(ctxt.datasets().generateIdentifierSequence(protocol, authority, theDataset.getDoiSeparator()));
                                if (!ctxt.doiDataCite().alreadyExists(theDataset)) {
                                    ctxt.doiDataCite().createIdentifier(theDataset);
                                    theDataset.setGlobalIdCreateTime(new Timestamp(new Date().getTime()));
                                } else {
                                    throw new IllegalCommandException("This dataset may not be published because its identifier is already in use by another dataset. Please contact Dataverse Support for assistance.", this);
                                }
                            }
                        } catch (Exception e) {
                            throw new CommandException(ResourceBundle.getBundle("Bundle").getString("dataset.publish.error.datacite"), this);
                        }
                        break;
                        
                    default:
                        throw new IllegalCommandException("This dataset may not be published because its DOI provider is not supported. Please contact Dataverse Support for assistance.", this);
                        
                }
            } else {
                throw new IllegalCommandException("This dataset may not be published because its external ID provider is not supported. Please contact Dataverse Support for assistance.", this);
            }
        }
    }

}
