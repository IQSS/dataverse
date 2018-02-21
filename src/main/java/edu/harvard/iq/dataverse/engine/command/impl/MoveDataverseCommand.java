package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.Guestbook;
import static edu.harvard.iq.dataverse.IdServiceBean.logger;
import edu.harvard.iq.dataverse.MetadataBlock;
import edu.harvard.iq.dataverse.Template;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissionsMap;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

/**
 * A command to move a {@link Dataverse} between two {@link Dataverse}s.
 * @author michael
 */

//@todo We will need to revist the permissions for move, once we add this 
//(will probably need different move commands for unplublished which checks add,
//versus published which checks publish 

// since the current implementation is superuser only, we can ignore these permission
// checks that would need to be revisited if regular users were able to use this
@RequiredPermissionsMap({
	@RequiredPermissions( dataverseName = "moved",       value = {Permission.ManageDataversePermissions, Permission.EditDataverse} ),
	@RequiredPermissions( dataverseName = "source",      value = Permission.DeleteDataverse ),
	@RequiredPermissions( dataverseName = "destination", value = Permission.AddDataverse )
})
public class MoveDataverseCommand extends AbstractVoidCommand {

    final Dataverse moved;
    final Dataverse destination;
    final Boolean force;

    public MoveDataverseCommand( DataverseRequest aRequest, Dataverse moved, Dataverse destination, Boolean force ) {
        super(aRequest, dv("moved", moved),
                                 dv("source",moved.getOwner()),
                                 dv("destination",destination) );
        this.moved = moved;
        this.destination = destination;
        this.force = force;
    }

    @Override
    public void executeImpl(CommandContext ctxt) throws CommandException {
        // first check if user is a superuser
        if ( (!(getUser() instanceof AuthenticatedUser) || !getUser().isSuperuser() ) ) {      
            throw new PermissionException("Move Dataset can only be called by superusers.",
                this,  Collections.singleton(Permission.DeleteDataverse), moved);                
        }

        // validate the move makes sense
        if ( destination.getOwners().contains(moved) ) {
                throw new IllegalCommandException("Can't move a Dataverse to its descendant", this);
        }
        if (moved.getOwner().equals(destination)) {
            throw new IllegalCommandException("Dataverse already in this Dataverse ", this);
        }
        if (moved.equals(destination)) {
            throw new IllegalCommandException("Cannot move a Dataverse into itself", this);
        }
        // if dataverse is published make sure that its destination is published
        if (moved.isReleased() && !destination.isReleased()){
            throw new IllegalCommandException("Published Dataverse may not be moved to unpublished Dataverse. You may publish " + destination.getDisplayName() + " and re-try the move.", this);
        }

        // if all the dataverses GUESTBOOKS are not contained in the new dataverse then remove the
        // ones that aren't
        if (moved.getGuestbooks() != null) {
            List<Guestbook> movedGbs = moved.getGuestbooks();
            List<Guestbook> desintationGbs = destination.getGuestbooks();
            boolean inheritGuestbooksValue = !destination.isGuestbookRoot();
            if (inheritGuestbooksValue && destination.getOwner() != null) {
                for (Guestbook pg : destination.getParentGuestbooks()) {
                    desintationGbs.add(pg);
                }
            }
            
            Iterator<Guestbook> iter = movedGbs.iterator();
            while (iter.hasNext()) {
                Guestbook gb = iter.next();
                if (desintationGbs == null || !desintationGbs.contains(gb)) {
                    if (force == null  || !force) {
                        throw new IllegalCommandException("Dataverse guestbook is not in target dataverse. Please use the parameter ?forceMove=true to complete the move. This will delete the guestbook from the Dataverse", this);
                    }
                    logger.info("Attempting to remove guestbook: " + gb.getName());
                    try {
                        ctxt.engine().submit(new DeleteGuestbookCommand(getRequest(), moved, gb));
                        moved.getGuestbooks().remove(gb);
                    } catch (CommandException ex) {
                        throw new CommandException("Failed to delete guestbook: "+ gb.getName(), this);
                    }
                }
            }
        }
        
        // metadata blocks
        if (moved.getMetadataBlocks() != null) {
            List<MetadataBlock> movedMbs = moved.getMetadataBlocks();
            List<MetadataBlock> destinationMbs = destination.getMetadataBlocks();
            boolean inheritMetadataBlockValue = destination.isMetadataBlockRoot();
            if (inheritMetadataBlockValue && destination.getOwner() != null) {
                // todo
            }
            
        }
        
        // if all the dataverses TEMPLATES are not contained in the new dataverse then remove the
        // ones that aren't
        if (moved.getTemplates() != null) {
            List<Template> movedTemplates = moved.getTemplates();
            List<Template> destinationTemplates = destination.getTemplates();
            boolean inheritTemplateValue = destination.isTemplateRoot();
            if (inheritTemplateValue && destination.getOwner() != null) {
                for (Template t : destination.getParentTemplates()) {
                    destinationTemplates.add(t);
                }
            }
            Iterator<Template> iter = movedTemplates.iterator();
            while (iter.hasNext()) {
                Template t = iter.next();
                if (destinationTemplates == null || !destinationTemplates.contains(t)) {
                    if (force == null  || !force) {
                        throw new IllegalCommandException("Dataverse template is not in target dataverse. Please use the parameter ?forceMove=true to complete the move. This will delete the template from the Dataverse", this);
                    }
                    logger.info("Attempting to remove template: " + t.getName());
                    try {
                        List<Dataverse> dataverseWDefaultTemplate = ctxt.templates().findDataversesByDefaultTemplateId(t.getId());
                        ctxt.engine().submit(new DeleteTemplateCommand(getRequest(), moved, t, dataverseWDefaultTemplate));
                        moved.getTemplates().remove(t);
                    } catch (CommandException ex) {
                        throw new CommandException("Failed to delete template: "+ t.getName(), this);
                    }
                } 
            }
        }
        // what else?
        
        // OK, move
        moved.setOwner(destination);
        ctxt.dataverses().save(moved);        
        try {
            boolean doNormalSolrDocCleanUp = true;
            ctxt.index().indexDataverseRecursively(moved, doNormalSolrDocCleanUp);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Exception while indexing:" + e.getMessage()); //, e);
            throw new CommandException("Dataverse could not be moved. Indexing failed: (" + e.getMessage() + ")", this);
        }
    }
}
