package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetLinkingDataverse;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseFeaturedDataverse;
import edu.harvard.iq.dataverse.DataverseLinkingDataverse;
import edu.harvard.iq.dataverse.Guestbook;
import edu.harvard.iq.dataverse.MetadataBlock;
import edu.harvard.iq.dataverse.Template;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.batch.util.LoggingUtil;
import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissionsMap;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import org.apache.solr.client.solrj.SolrServerException;

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
    @RequiredPermissions(dataverseName = "moved", value = {Permission.ManageDataversePermissions, Permission.EditDataverse}),
	@RequiredPermissions(dataverseName = "source", value = Permission.DeleteDataverse),
	@RequiredPermissions(dataverseName = "destination", value = Permission.AddDataverse)
})
public class MoveDataverseCommand extends AbstractVoidCommand {
    private static final Logger logger = Logger.getLogger(MoveDataverseCommand.class.getName());
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
        long moveDvStart = System.currentTimeMillis();
        logger.info("Starting dataverse move...");
        boolean removeGuestbook = false, removeTemplate = false, removeFeatDv = false, removeMetadataBlock = false, removeLinkDv = false, removeLinkDs = false;
        
        // first check if user is a superuser
        if ((!(getUser() instanceof AuthenticatedUser) || !getUser().isSuperuser())) {
            throw new PermissionException(BundleUtil.getStringFromBundle("command.exception.only.superusers", Arrays.asList(this.toString())),
                    this, Collections.singleton(Permission.DeleteDataverse), moved);
        }

        // validate the move makes sense
        if (destination.getOwners().contains(moved)) {
            throw new IllegalCommandException(BundleUtil.getStringFromBundle("dataverses.api.move.dataverse.failure.descendent"), this);
        }
        if (moved.getOwner().equals(destination)) {
            throw new IllegalCommandException(BundleUtil.getStringFromBundle("dataverses.api.move.dataverse.failure.already.member"), this);
        }
        if (moved.equals(destination)) {
            throw new IllegalCommandException(BundleUtil.getStringFromBundle("dataverses.api.move.dataverse.failure.itself"), this);
        }
        // if dataverse is published make sure that its destination is published
        if (moved.isReleased() && !destination.isReleased()) {
            throw new IllegalCommandException(BundleUtil.getStringFromBundle("dataverses.api.move.dataverse.failure.not.published", Arrays.asList(destination.getDisplayName())), this);
        }
        
        logger.info("Getting dataset children of dataverse...");
        List<Dataset> datasetChildren = new ArrayList<>();
        List<Long> datasetChildrenIds = ctxt.dataverses().findAllDataverseDatasetChildren(moved.getId());
        datasetChildrenIds.forEach( (dsId) -> datasetChildren.add(ctxt.datasets().find(dsId)) );

        logger.info("Getting dataverse children of dataverse...");
        List<Dataverse> dataverseChildren = new ArrayList<>();
        List<Long> dataverseChildrenIds = ctxt.dataverses().findAllDataverseDataverseChildren(moved.getId());
        dataverseChildrenIds.forEach( (dvId) -> dataverseChildren.add(ctxt.dataverses().find(dvId)) );

        dataverseChildren.add(moved); // include the root of the children

        
        // generate list of all possible parent dataverses to check against
        List<Dataverse> ownersToCheck = new ArrayList<>();
        ownersToCheck.add(destination);
        ownersToCheck.add(moved);
        if (destination.getOwners() != null) {
            ownersToCheck.addAll(destination.getOwners());
        }
        
        // generate list of destination guestbooks to check against
        List<Guestbook> destinationGbs = null;
        if (moved.getGuestbooks() != null) {
            List<Guestbook> movedGbs = moved.getGuestbooks();
            destinationGbs = destination.getGuestbooks();
            boolean inheritGuestbooksValue = !destination.isGuestbookRoot();
            if (inheritGuestbooksValue && destination.getOwner() != null) {
                destinationGbs.addAll(destination.getParentGuestbooks());
            }
            // include guestbooks in moved dataverse since they will also be there
            // in the destination
            destinationGbs.addAll(movedGbs);
        }

        // if the dataverse is FEATURED by its parent, remove it
        List<DataverseFeaturedDataverse> ownerFeaturedDv = moved.getOwner().getDataverseFeaturedDataverses();
        if (ownerFeaturedDv != null) {
            logger.info("Checking featured dataverses...");
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
        
        // generate a list of templates in destination to check against
        List<Template> destinationTemplates = null;
        if (moved.getTemplates() != null) {
            List<Template> movedTemplates = moved.getTemplates();
            destinationTemplates = destination.getTemplates();
            boolean inheritTemplateValue = !destination.isTemplateRoot();
            if (inheritTemplateValue && destination.getOwner() != null) {
                destinationTemplates.addAll(destination.getParentTemplates());
            }
            // include templates in moved dataverse since they will also be there
            // in the destination
            destinationTemplates.addAll(movedTemplates);
        }

        // generate a list of metadatablocks in destination to check against
        Boolean inheritMbValue = null;
        List<Dataverse> mbParentsToCheck = new ArrayList<>();
        mbParentsToCheck.addAll(ownersToCheck);
        mbParentsToCheck.addAll(dataverseChildren);
        if (moved.getMetadataBlocks() != null) {
            inheritMbValue = !destination.isMetadataBlockRoot();
        }
                
        List<DataverseLinkingDataverse> linkingDataverses = new ArrayList();
    
        logger.info("Checking templates and metadata blocks");
        for (Dataverse dv : dataverseChildren) {
            // if the dataverses default TEMPLATE is not contained in the 
            // destination dataverse, remove it
            if (destinationTemplates != null) {
                Template dvt = dv.getDefaultTemplate();
                if (dvt != null && !destinationTemplates.contains(dvt)) {
                    if (force == null || !force) {
                        removeTemplate = true;
                        break;
                    }
                    dv.setDefaultTemplate(null);
                }
            }

            // if all the dataverses METADATA BLOCKS are not contained in the new dataverse then remove the
            // ones that aren't available in the destination
            // i.e. the case where a custom metadata block is available through a parent 
            // but then the dataverse is moved outside of that parent-child structure
            if (inheritMbValue != null) {
                List<MetadataBlock> metadataBlocksToKeep = new ArrayList<>();
                List<MetadataBlock> movedMbs = dv.getMetadataBlocks(true);
                Iterator<MetadataBlock> iter = movedMbs.iterator();
                while (iter.hasNext()) {
                    MetadataBlock mb = iter.next();
                    // if the owner is null, it means that the owner is the root dataverse
                    // because technically only custom metadata blocks have owners
                    Dataverse mbOwner = (mb.getOwner() != null) ? mb.getOwner() : ctxt.dataverses().findByAlias(":root");
                    if (!mbParentsToCheck.contains(mbOwner)) {
                        if (force == null || !force) {
                            removeMetadataBlock = true;
                            break;
                        }
                    } else if (mbParentsToCheck.contains(mbOwner) || inheritMbValue) {
                        // only keep metadata block if
                        // it is being inherited from its parent
                        metadataBlocksToKeep.add(mb);
                    }
                }
                if (force != null && force) {
                    dv.setMetadataBlocks(metadataBlocksToKeep);
                }
            }
            
            // get list of dataverses each child links to
            if (dv.getDataverseLinkingDataverses() != null) {
                linkingDataverses.addAll(dv.getDataverseLinkingDataverses());
            }
        }
        
        List<DatasetLinkingDataverse> linkingDatasets = new ArrayList();
        logger.info("Checking guestbooks...");
        for (Dataset ds : datasetChildren) {
            // if all the dataverse's datasets GUESTBOOKS are not 
            //contained in the new dataverse, then remove them
            Guestbook dsgb = ds.getGuestbook();
            if (dsgb != null && (destinationGbs == null || !destinationGbs.contains(dsgb))) {
                if (force == null || !force) {
                    removeGuestbook = true;
                    break;
                }
                ds.setGuestbook(null);
            }
            
            // get list of dataverses each child dataset links to
            if (ds.getDatasetLinkingDataverses() != null) {
                linkingDatasets.addAll(ds.getDatasetLinkingDataverses());
            }
        }
        
        // if a dataverse links to its destination dataverse or any of 
        // its destinations owners, remove the link
        logger.info("Checking linked dataverses....");
        for (DataverseLinkingDataverse dvld : linkingDataverses) {
            for (Dataverse owner : ownersToCheck){
                if ((dvld.getLinkingDataverse()).equals(owner)){
                    if (force == null || !force) {
                        removeLinkDv = true;
                        break;
                    }
                    boolean index = false;
                    ctxt.engine().submit(new DeleteDataverseLinkingDataverseCommand(getRequest(), dvld.getDataverse(), dvld, index));
                    (dvld.getDataverse()).getDataverseLinkingDataverses().remove(dvld);
                }
            }
        }
        
        // if a dataset links to its destination dataverse or any of 
        // its destinations owners, remove the link
        logger.info("Checking linked datasets...");
        for (DatasetLinkingDataverse dsld : linkingDatasets) {
            for (Dataverse owner : ownersToCheck) {
                if ((dsld.getLinkingDataverse()).equals(owner)) {
                    if (force == null || !force) {
                        removeLinkDs = true;
                        break;
                    }
                    boolean index = false;
                    ctxt.engine().submit(new DeleteDatasetLinkingDataverseCommand(getRequest(), dsld.getDataset(), dsld, index));
                    (dsld.getDataset()).getDatasetLinkingDataverses().remove(dsld);
                }
            }
        }

        if (removeGuestbook || removeTemplate || removeFeatDv || removeMetadataBlock || removeLinkDv || removeLinkDs) {
            StringBuilder errorString = new StringBuilder();
            if (removeGuestbook) {
                errorString.append(BundleUtil.getStringFromBundle("dataverses.api.move.dataverse.error.guestbook")).append(" ");
            }
            if (removeTemplate) {
                errorString.append(BundleUtil.getStringFromBundle("dataverses.api.move.dataverse.error.template")).append(" ");
            } 
            if (removeFeatDv) {               
               errorString.append(BundleUtil.getStringFromBundle("dataverses.api.move.dataverse.error.featured")).append(" ");
            }
            if (removeMetadataBlock) {                
               errorString.append(BundleUtil.getStringFromBundle("dataverses.api.move.dataverse.error.metadataBlock")).append(" ");
            }
            if (removeLinkDv) {                
                errorString.append(BundleUtil.getStringFromBundle("dataverses.api.move.dataverse.error.dataverseLink")).append(" ");
            }
            if (removeLinkDs) {
                errorString.append(BundleUtil.getStringFromBundle("dataverses.api.move.dataverse.error.datasetLink")).append(" ");
            }            
            errorString.append(BundleUtil.getStringFromBundle("dataverses.api.move.dataverse.error.forceMove")).append(" ");
            throw new IllegalCommandException(errorString.toString(), this);
        }
        // OK, move
        moved.setOwner(destination);
        ctxt.dataverses().save(moved);
        
        long moveDvEnd = System.currentTimeMillis();
        logger.info("Dataverse move took " + (moveDvEnd - moveDvStart) + " milliseconds");
        
	//TODO: indexing should be moved to an on Success method
        ctxt.indexBatch().indexDataverseRecursively(moved);
        
        //REindex datasets linked to moved dv
        if (moved.getDatasetLinkingDataverses() != null && !moved.getDatasetLinkingDataverses().isEmpty()) {
            for (DatasetLinkingDataverse dld : moved.getDatasetLinkingDataverses()) {
                Dataset linkedDS = ctxt.datasets().find(dld.getDataset().getId());
                ctxt.index().asyncIndexDataset(linkedDS, true);

            }
        }
    }
}
