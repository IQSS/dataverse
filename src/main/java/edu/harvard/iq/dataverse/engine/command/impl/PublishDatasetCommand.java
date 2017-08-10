package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetLock;
import edu.harvard.iq.dataverse.DatasetVersionUser;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.UserNotification;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.workflow.Workflow;
import edu.harvard.iq.dataverse.workflow.WorkflowContext.TriggerType;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 *
 * @author skraffmiller
 * @author michbarsinai
 */
@RequiredPermissions(Permission.PublishDataset)
public class PublishDatasetCommand extends AbstractPublishDatasetCommand<PublishDatasetResult> {

    private static final int FOOLPROOF_RETRIAL_ATTEMPTS_LIMIT = 2 ^ 8;

    private static final String DEFAULT_DOI_PROVIDER_KEY = "";

    boolean minorRelease;
    
    public PublishDatasetCommand(Dataset datasetIn, DataverseRequest aRequest, boolean minor) {
        super(datasetIn, aRequest);
        minorRelease = minor;
        theDataset = datasetIn;
    }

    @Override
    public PublishDatasetResult execute(CommandContext ctxt) throws CommandException {

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
            
        } else /* major, non-first release */ {
            theDataset.getEditVersion().setVersionNumber(new Long(theDataset.getVersionNumber() + 1));
            theDataset.getEditVersion().setMinorVersionNumber(new Long(0));
        }

        // update metadata
        Timestamp updateTime = new Timestamp(new Date().getTime());
        theDataset.getEditVersion().setReleaseTime(updateTime);
        theDataset.getEditVersion().setLastUpdateTime(updateTime);
        theDataset.setModificationTime(updateTime);
        theDataset.setFileAccessRequest(theDataset.getLatestVersion().getTermsOfUseAndAccess().isFileAccessRequest());
        
        ctxt.engine().submit( new RemoveLockCommand(getRequest(), theDataset));

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
        
        Optional<Workflow> prePubWf = ctxt.workflows().getDefaultWorkflow(TriggerType.PrePublishDataset);
        if ( prePubWf.isPresent() ) {
            // We start a workflow
            ctxt.engine().submit( new AddLockCommand(getRequest(), theDataset, new DatasetLock(DatasetLock.Reason.Workflow, getRequest().getAuthenticatedUser())));
            ctxt.workflows().start(prePubWf.get(), buildContext(doiProvider, TriggerType.PrePublishDataset) );
            return new PublishDatasetResult(theDataset, false);
            
        } else {
            // Synchronous publishing (no workflow involved)
            theDataset = ctxt.engine().submit( new FinalizeDatasetPublicationCommand(theDataset, doiProvider, getRequest()) );
            return new PublishDatasetResult(ctxt.em().merge(theDataset), true);
        }
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
    
    /**
     * Whether it's EZID or DataCiteif, if the registration is 
     * refused because the identifier already exists, we'll generate another one
     * and try to register again... but only up to some
     * reasonably high number of times - so that we don't 
     * go into an infinite loop here, if EZID is giving us 
     * these duplicate messages in error. 
     * 
     * (and we do want the limit to be a "reasonably high" number! 
     * true, if our identifiers are randomly generated strings, 
     * then it is highly unlikely that we'll ever run into a 
     * duplicate race condition repeatedly; but if they are sequential
     * numeric values, than it is entirely possible that a large
     * enough number of values will be legitimately registered 
     * by another entity sharing the same authority...)
     * @param theDataset
     * @param ctxt
     * @param doiProvider
     * @throws CommandException 
     */
    private void registerExternalIdentifier(Dataset theDataset, CommandContext ctxt, String doiProvider) throws CommandException {
        String protocol = theDataset.getProtocol();
        String authority = theDataset.getAuthority();
        int attempts = 0;
        if (theDataset.getGlobalIdCreateTime() == null) {
            if (protocol.equals("doi")) {
                switch( doiProvider ) {
                    case "EZID":
                        String doiRetString = ctxt.doiEZId().createIdentifier(theDataset);
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
                        break;
                        
                    case "DataCite":
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
