package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.Template;
import edu.harvard.iq.dataverse.UserNotification;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.pidproviders.PidProvider;
import edu.harvard.iq.dataverse.pidproviders.PidUtil;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

import static edu.harvard.iq.dataverse.util.StringUtil.nonEmpty;

import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;

import java.sql.Timestamp;
import java.time.Instant;

/**
 * Creates a new {@link Dataset}, used to store unpublished data. This is as opposed to 
 * a harvested or imported datasets, which may contain data that was already published 
 * when they are created.
 * 
 * @author michael
 */
@RequiredPermissions(Permission.AddDataset)
// I am reverting the PR #6061, going back to the fixed RequiredPermissions that 
// specifies the AddDataset to be the only permission needed. We tried to 
// replace it with a dynamic permissions map, with the extra ViewUnpublishedDataverse
// permission added when the Dataverse is unpublished (to prevent any user with a 
// Dataverse account from adding datasets to a dataverse so configured, before it's 
// published). This caused too many unexpected complications - the most notable
// one with the SWORD API (a bunch of RestAssured tests apparently rely on this 
// ability). 
// In order to re-enable the dynamic permissions, comment out the RequiredPermissions
// line above, AND un-comment out the getRequiredPermissions() method below. 

public class CreateNewDatasetCommand extends AbstractCreateDatasetCommand {

    private final Template template;
    private boolean allowSelfNotification = false;

    public CreateNewDatasetCommand(Dataset theDataset, DataverseRequest aRequest, boolean allowSelfNotification) {
        this(theDataset, aRequest, null);
        this.allowSelfNotification = allowSelfNotification;
    }

    public CreateNewDatasetCommand(Dataset theDataset, DataverseRequest aRequest) {
        this( theDataset, aRequest, null);
    }
    
    public CreateNewDatasetCommand(Dataset theDataset, DataverseRequest aRequest, Template template) {
        super(theDataset, aRequest);
        this.template = template;
    }
    
    public CreateNewDatasetCommand(Dataset theDataset, DataverseRequest aRequest, Template template, boolean validate) {
        super(theDataset, aRequest, false, validate);
        this.template = template;
    }
    
    /**
     * A new dataset must have a new identifier.
     * @param ctxt
     * @throws CommandException 
     */
    @Override
    protected void additionalParameterTests(CommandContext ctxt) throws CommandException {
        if (nonEmpty(getDataset().getIdentifier())) {
            GlobalId pid = getDataset().getGlobalId();
            if (pid != null) {
                PidProvider pidProvider = PidUtil.getPidProvider(pid.getProviderId());

                if (!pidProvider.isGlobalIdUnique(pid)) {
                    throw new IllegalCommandException(String.format(
                            "Dataset with identifier '%s', protocol '%s' and authority '%s' already exists",
                            getDataset().getIdentifier(), getDataset().getProtocol(), getDataset().getAuthority()),
                            this);
                }
            }
        }
    }
    
    @Override
    protected DatasetVersion getVersionToPersist( Dataset theDataset ) {
        return theDataset.getOrCreateEditVersion();
    }

    @Override
    protected void handlePid(Dataset theDataset, CommandContext ctxt) throws CommandException {
        PidProvider pidProvider = PidUtil.getPidProvider(theDataset.getGlobalId().getProviderId());
        if(!pidProvider.canManagePID()) {
            throw new IllegalCommandException("PID Provider " + pidProvider.getId() + " is not configured.", this);
        }
        if ( !pidProvider.registerWhenPublished() ) {
            // pre-register a persistent id
            registerExternalIdentifier(theDataset, ctxt, true);
        }
    }
    
    @Override
    protected void postPersist( Dataset theDataset, CommandContext ctxt ){
        // set the role to be default contributor role for its dataverse
        String privateUrlToken = null;
        if (theDataset.getOwner().getDefaultContributorRole() != null) {
            RoleAssignment roleAssignment = new RoleAssignment(theDataset.getOwner().getDefaultContributorRole(),
                    getRequest().getUser(), theDataset, privateUrlToken);
            ctxt.roles().save(roleAssignment, false);

            // TODO: the above may be creating the role assignments and saving them 
            // in the database, but without properly linking them to the dataset
            // (saveDataset, that the command returns). This may have been the reason 
            // for the github issue #4783 - where the users were losing their contributor
            // permissions, when creating datasets AND uploading files in one step. 
            // In that scenario, an additional UpdateDatasetCommand is executed on the
            // dataset returned by the Create command. That issue was fixed by adding 
            // a full refresh of the dataset with datasetService.find() between the
            // two commands. But it may be a good idea to make sure they are properly
            // linked here (?)
            theDataset.setPermissionModificationTime(getTimestamp());
        }
        
        if ( template != null ) {
            ctxt.templates().incrementUsageCount(template.getId());
        }
    }

    /**
     * Sends notifications to those able to publish the dataset upon the successful creation of a new dataset.
     * <p>
     * This method checks if dataset creation notifications are enabled. If so, it
     * notifies all users with {@code Permission.PublishDataset} on the new dataset.
     * The user who initiated the action can be included or excluded from this
     * notification based on the allowSelfNotification flag.
     *
     * @param dataset The newly created {@code Dataset}.
     * @param ctxt    The {@code CommandContext} providing access to application services.
     */
    protected void postDBFlush(Dataset dataset, CommandContext ctxt) {
        // 1. Exit early if the SendNotificationOnDatasetCreation setting is disabled.
        if (!ctxt.settings().isTrueForKey(SettingsServiceBean.Key.SendNotificationOnDatasetCreation, false)) {
            return;
        }

        // 2. Identify the user who initiated the action.
        final User sessionUser = getUser();
        final AuthenticatedUser requestor = sessionUser.isAuthenticated() ? (AuthenticatedUser) sessionUser : null;

        // 3. Get all users with publish permission and notify them.
        ctxt.permissions().getUsersWithPermissionOn(Permission.PublishDataset, dataset)
                .stream()
                .filter(recipient -> allowSelfNotification || !recipient.equals(requestor))
                .forEach(recipient -> ctxt.notifications().sendNotification(
                        recipient,
                        Timestamp.from(Instant.now()),
                        UserNotification.Type.DATASETCREATED,
                        dataset.getId(),
                        null,
                        requestor,
                        true
                ));
    }

    // Re-enabling the method below will change the permission setup to dynamic.
    // This will make it so that in an unpublished dataverse only users with the 
    // permission to view it will be allowed to create child datasets. 
    /*@Override
    public Map<String, Set<Permission>> getRequiredPermissions() {
        Map<String, Set<Permission>> ret = new HashMap<>();
        // NOTE: DO NOT use builtin methods such as 
        // Collections.singleton(Permission.AddDataset) in order to create 
        // permission HashSets. Collections.singleton() produces a set that is 
        // *unmutable* - and you should assume that the set may need to be 
        // modified later on. For example, as follows, in the 
        // hasGroupPermissionsFor() method in PermissionServiceBean:
        // 
        // for (RoleAssignment asmnt : assignmentsFor(ras, dvo)) {
        //    required.removeAll(asmnt.getRole().permissions());
        // }
        // return required.isEmpty();
        ret.put("", new HashSet<>(Arrays.asList(Permission.AddDataset)));
        if (!dv.isReleased()) {
            ret.get("").add(Permission.ViewUnpublishedDataverse);
        }
        return ret;
    }*/

}
