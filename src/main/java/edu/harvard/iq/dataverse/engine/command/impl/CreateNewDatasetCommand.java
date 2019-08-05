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
import java.util.Collections;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

/**
 * Creates a new {@link Dataset}, used to store unpublished data. This is as opposed to 
 * a harvested or imported datasets, which may contain data that was already published 
 * when they are created.
 * 
 * @author michael
 */
//@RequiredPermissions(Permission.AddDataset)
// Changing the permission setup to dynamic, to accommodate the case of creating 
// a dataset in an unpublished dataverse; only users with the permission to view it 
// should be allowed to create child datasets. Otherwise any users with an account 
// can create a dataset in a dataverse before it's even published, if "Anyone with a 
// Dataverse account can add datasets" option is chosen. 
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
            if ( ctxt.datasets().isIdentifierUnique(getDataset().getIdentifier(), getDataset(), idServiceBean) ) {
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
    
    @Override
    public Map<String, Set<Permission>> getRequiredPermissions() {
        return Collections.singletonMap("",
                dv.isReleased() ? Collections.singleton(Permission.AddDataset)
                : new HashSet<>(Arrays.asList(Permission.AddDataset,Permission.ViewUnpublishedDataverse)));
    }
        
}
