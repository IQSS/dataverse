package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.Template;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import static edu.harvard.iq.dataverse.util.StringUtil.nonEmpty;
import java.util.logging.Logger;
import edu.harvard.iq.dataverse.GlobalIdServiceBean;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import java.util.Collections;
import java.util.HashSet;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
    private static final Logger logger = Logger.getLogger(CreateNewDatasetCommand.class.getName());
    
    private final Template template;
    private final Dataverse dv;

    public CreateNewDatasetCommand(Dataset theDataset, DataverseRequest aRequest) {
        this( theDataset, aRequest, false); 
    }
    
    public CreateNewDatasetCommand(Dataset theDataset, DataverseRequest aRequest, boolean registrationRequired) {
        this( theDataset, aRequest, registrationRequired, null);
    }
    
    public CreateNewDatasetCommand(Dataset theDataset, DataverseRequest aRequest, boolean registrationRequired, Template template) {
        super(theDataset, aRequest, registrationRequired);
        this.template = template;
        dv = theDataset.getOwner();
    }
    
    /**
     * A new dataset must have a new identifier.
     * @param ctxt
     * @throws CommandException 
     */
    @Override
    protected void additionalParameterTests(CommandContext ctxt) throws CommandException {
        if ( nonEmpty(getDataset().getIdentifier()) ) {
            GlobalIdServiceBean idServiceBean = GlobalIdServiceBean.getBean(getDataset().getProtocol(), ctxt);
            if (!ctxt.datasets().isIdentifierUnique(getDataset().getIdentifier(), getDataset(), idServiceBean) ) {
                throw new IllegalCommandException(String.format("Dataset with identifier '%s', protocol '%s' and authority '%s' already exists",
                                                                 getDataset().getIdentifier(), getDataset().getProtocol(), getDataset().getAuthority()), 
                    this);
           }
        }
    }
    
    @Override
    protected DatasetVersion getVersionToPersist( Dataset theDataset ) {
        return theDataset.getEditVersion();
    }

    @Override
    protected void handlePid(Dataset theDataset, CommandContext ctxt) throws CommandException {
        GlobalIdServiceBean idServiceBean = GlobalIdServiceBean.getBean(ctxt);
        if ( !idServiceBean.registerWhenPublished() ) {
            // pre-register a persistent id
            registerExternalIdentifier(theDataset, ctxt);
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
            // In that scenario, an additional UpdateDatasetCommand is exectued on the
            // dataset returned by the Create command. That issue was fixed by adding 
            // a full refresh of the datast with datasetService.find() between the 
            // two commands. But it may be a good idea to make sure they are properly
            // linked here (?)
            theDataset.setPermissionModificationTime(getTimestamp());
        }
        
        if ( template != null ) {
            ctxt.templates().incrementUsageCount(template.getId());
        }
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
