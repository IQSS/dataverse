package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseFeaturedDataverse;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

/**
 * A command to move a {@link Dataverse} between two {@link Dataverse}s.
 *
 * @author michael
 */
//@todo We will need to revist the permissions for move, once we add this 
//(will probably need different move commands for unplublished which checks add,
//versus published which checks publish 
// since the current implementation is superuser only, we can ignore these permission
// checks that would need to be revisited if regular users were able to use this
@RequiredPermissionsMap({
    @RequiredPermissions(dataverseName = "moved", value = {Permission.ManageDataversePermissions, Permission.EditDataverse})
    ,
	@RequiredPermissions(dataverseName = "source", value = Permission.DeleteDataverse)
    ,
	@RequiredPermissions(dataverseName = "destination", value = Permission.AddDataverse)
})
public class MoveDataverseCommand extends AbstractVoidCommand {

    final Dataverse moved;
    final Dataverse destination;
    final Boolean force;

    public MoveDataverseCommand(DataverseRequest aRequest, Dataverse moved, Dataverse destination, Boolean force) {
        super(aRequest, dv("moved", moved),
                dv("source", moved.getOwner()),
                dv("destination", destination));
        this.moved = moved;
        this.destination = destination;
        this.force = force;
    }

    @Override
    public void executeImpl(CommandContext ctxt) throws CommandException {
        boolean removeGuestbook = false, removeTemplate = false, removeFeatDv = false, removeMetadataBlock = false;
        
        // first check if user is a superuser
        if ((!(getUser() instanceof AuthenticatedUser) || !getUser().isSuperuser())) {
            throw new PermissionException("Move Dataset can only be called by superusers.",
                    this, Collections.singleton(Permission.DeleteDataverse), moved);
        }

        // validate the move makes sense
        if (destination.getOwners().contains(moved)) {
            throw new IllegalCommandException("Can't move a dataverse to its descendant", this);
        }
        if (moved.getOwner().equals(destination)) {
            throw new IllegalCommandException("Dataverse already in this dataverse ", this);
        }
        if (moved.equals(destination)) {
            throw new IllegalCommandException("Cannot move a dataverse into itself", this);
        }
        // if dataverse is published make sure that its destination is published
        if (moved.isReleased() && !destination.isReleased()) {
            throw new IllegalCommandException("Published dataverse may not be moved to unpublished dataverse. You may publish " + destination.getDisplayName() + " and re-try the move.", this);
        }

        List<Dataset> datasetChildren = ctxt.dataverses().findAllDataverseDatasetChildren(moved);

        List<Dataverse> dataverseChildren = ctxt.dataverses().findAllDataverseDataverseChildren(moved);
        dataverseChildren.add(moved); // include the root of the children

        // if all the dataverse's datasets GUESTBOOKS are not contained in the new dataverse then remove the
        // ones that aren't
        if (moved.getGuestbooks() != null) {
            List<Guestbook> movedGbs = moved.getGuestbooks();
            List<Guestbook> destinationGbs = destination.getGuestbooks();
            boolean inheritGuestbooksValue = !destination.isGuestbookRoot();
            if (inheritGuestbooksValue && destination.getOwner() != null) {
                destinationGbs.addAll(destination.getParentGuestbooks());
            }
            // include guestbooks in moved dataverse since they will also be there
            // in the destination
            destinationGbs.addAll(movedGbs);
            for (Dataset ds : datasetChildren) {
                Guestbook dsgb = ds.getGuestbook();
                if (dsgb != null && (destinationGbs == null || !destinationGbs.contains(dsgb))) {
                    if (force == null || !force) {
                        removeGuestbook = true;
                        break;
                    }
                    ds.setGuestbook(null);
                }
            }
        }

        // if the dataverses default TEMPLATE is not contained in the new dataverse then remove it
        if (moved.getTemplates() != null) {
            List<Template> movedTemplates = moved.getTemplates();
            List<Template> destinationTemplates = destination.getTemplates();
            boolean inheritTemplateValue = !destination.isTemplateRoot();
            if (inheritTemplateValue && destination.getOwner() != null) {
                destinationTemplates.addAll(destination.getParentTemplates());
            }
            // include templates in moved dataverse since they will also be there
            // in the destination
            destinationTemplates.addAll(movedTemplates);
            for (Dataverse dv : dataverseChildren) {
                Template dvt = dv.getDefaultTemplate();
                if (dvt != null && (destinationTemplates == null || !destinationTemplates.contains(dvt))) {
                    if (force == null || !force) {
                        removeTemplate = true;
                        break;
                    }
                    dv.setDefaultTemplate(null);
                }
            }
        }

        // if the dataverse is FEATURED by its parent, remove it
        List<DataverseFeaturedDataverse> ownerFeaturedDv = moved.getOwner().getDataverseFeaturedDataverses();
        if (ownerFeaturedDv != null) {
            for (DataverseFeaturedDataverse dfdv : ownerFeaturedDv) {
                if (moved.equals(dfdv.getFeaturedDataverse())) {
                    if (force == null || !force) {
                        removeFeatDv = true;
                        break;
                    }
                    ctxt.featuredDataverses().delete(dfdv);
                }
            }
        }

        // if all the dataverses METADATA BLOCKS are not contained in the new dataverse then remove the
        // ones that aren't available in the destination
        // i.e. the case where a custom metadata block is available through a parent 
        // but then the dataverse is moved outside of that parent-child structure
        if (moved.getMetadataBlocks() != null) {
            boolean inheritMbValue = !destination.isMetadataBlockRoot();
            // generate list of all possible metadata block owner dataverses to check against
            List<Dataverse> ownersToCheck = new ArrayList<>();
            ownersToCheck.add(destination);
            ownersToCheck.add(moved);
            ownersToCheck.addAll(dataverseChildren);
            if (destination.getOwners() != null) {
                ownersToCheck.addAll(destination.getOwners());
            }

            // determine which metadata blocks to keep selected 
            // on the moved dataverse and its children 
            for (Dataverse dv : dataverseChildren) {
                List<MetadataBlock> metadataBlocksToKeep = new ArrayList<>();
                List<MetadataBlock> movedMbs = dv.getMetadataBlocks(true);
                Iterator<MetadataBlock> iter = movedMbs.iterator();
                while (iter.hasNext()) {
                    MetadataBlock mb = iter.next();
                    // if the owner is null, it means that the owner is the root dataverse
                    // because technically only custom metadata blocks have owners
                    Dataverse mbOwner = (mb.getOwner() != null) ? mb.getOwner() : ctxt.dataverses().findByAlias(":root");
                    if (!ownersToCheck.contains(mbOwner)) {
                        if (force == null || !force) {
                            removeMetadataBlock = true;
                            break;
                        }
                    } else if (ownersToCheck.contains(mbOwner) || inheritMbValue) {
                        // only keep metadata block if
                        // it is being inherited from its parent
                        metadataBlocksToKeep.add(mb);
                    }
                }
                if (force != null && force) {
                    dv.setMetadataBlocks(metadataBlocksToKeep);
                }
            }
        }
        
        if (removeGuestbook || removeTemplate || removeFeatDv || removeMetadataBlock) {
            StringBuilder errorString = new StringBuilder();
            if (removeGuestbook) {
                errorString.append("Dataset guestbook is not in target dataverse. ");
            } 
            if (removeTemplate) {
                errorString.append("Dataverse template is not in target dataverse. ");
            } 
            if (removeFeatDv) {
                errorString.append("Dataverse is featured in current dataverse. ");
            }
            if (removeMetadataBlock) {
               errorString.append("Dataverse metadata block is not in target dataverse. ");
            }
            errorString.append("Please use the parameter ?forceMove=true to complete the move. This will remove anything from the dataverse that is not compatible with the target dataverse.");
            throw new IllegalCommandException(errorString.toString(), this);
        }
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
