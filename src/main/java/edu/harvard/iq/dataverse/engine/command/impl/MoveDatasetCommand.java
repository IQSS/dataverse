/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetLinkingDataverse;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.Guestbook;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Moves Dataset from one dataverse to another
 *
 * @author skraffmi
 */

// the permission annotation is open, since this is a superuser-only command - 
// and that's enforced in the command body:
@RequiredPermissions({})
public class MoveDatasetCommand extends AbstractVoidCommand {

    private static final Logger logger = Logger.getLogger(MoveDatasetCommand.class.getCanonicalName());
    final Dataset moved;
    final Dataverse destination;
    final Boolean force;

    public MoveDatasetCommand(DataverseRequest aRequest, Dataset moved, Dataverse destination, Boolean force) {
        super(aRequest, moved);
        this.moved = moved;
        this.destination = destination;
        this.force= force;
    }

    @Override
    public void executeImpl(CommandContext ctxt) throws CommandException {
        boolean removeGuestbook = false, removeLinkDs = false;
       // first check if  user is a superuser
        if ( (!(getUser() instanceof AuthenticatedUser) || !getUser().isSuperuser() ) ) {      
            throw new PermissionException("Move Dataset can only be called by superusers.",
                this,  Collections.singleton(Permission.DeleteDatasetDraft), moved);                
        }
        
        
        // validate the move makes sense
        if (moved.getOwner().equals(destination)) {
            throw new IllegalCommandException("Dataset already in this Dataverse ", this);
        }
        
        // if dataset is published make sure that its target is published
        
        if (moved.isReleased() && !destination.isReleased()){
            throw new IllegalCommandException("Published Dataset may not be moved to unpublished Dataverse. You may publish " + destination.getDisplayName() + " and re-try the move.", this);
        }
                
        //if the datasets guestbook is not contained in the new dataverse then remove it
        if (moved.getGuestbook() != null) {
            Guestbook gb = moved.getGuestbook();
            List<Guestbook> gbs = destination.getGuestbooks();
            boolean inheritGuestbooksValue = !destination.isGuestbookRoot();
            if (inheritGuestbooksValue && destination.getOwner() != null) {
                for (Guestbook pg : destination.getParentGuestbooks()) {
                    gbs.add(pg);
                }
            }
            if (gbs == null || !gbs.contains(gb)) {
                if (force == null  || !force){
                    removeGuestbook = true;
                } else {
                    moved.setGuestbook(null);
                }
            }
        }
        
        // generate list of all possible parent dataverses to check against
        List<Dataverse> ownersToCheck = new ArrayList<>();
        ownersToCheck.add(destination);
        if (destination.getOwners() != null) {
            ownersToCheck.addAll(destination.getOwners());
        }
        
        // if the dataset is linked to the new dataverse or any of 
        // its parent dataverses then remove the link
        List<DatasetLinkingDataverse> linkingDatasets = new ArrayList<>();
        if (moved.getDatasetLinkingDataverses() != null) {
            linkingDatasets.addAll(moved.getDatasetLinkingDataverses());
        }
        for (DatasetLinkingDataverse dsld : linkingDatasets) {
            for (Dataverse owner : ownersToCheck){
                if ((dsld.getLinkingDataverse()).equals(owner)){
                    if (force == null || !force) {
                        removeLinkDs = true;
                        break;
                    }
                    boolean index = false;
                    ctxt.engine().submit(new DeleteDatasetLinkingDataverseCommand(getRequest(), dsld.getDataset(), dsld, index));
                    moved.getDatasetLinkingDataverses().remove(dsld);
                }
            }
        }
        
        if (removeGuestbook || removeLinkDs) {
            StringBuilder errorString = new StringBuilder();
            if (removeGuestbook) {
                errorString.append("Dataset guestbook is not in target dataverse. ");
            }
            if (removeLinkDs) {
                errorString.append("Dataset is linked to target dataverse or one of its parents. ");
            }
            throw new IllegalCommandException(errorString + "Please use the parameter ?forceMove=true to complete the move. This will remove anything from the dataset that is not compatible with the target dataverse.", this);
        }


        // OK, move
        moved.setOwner(destination);
        ctxt.em().merge(moved);

        try {
            boolean doNormalSolrDocCleanUp = true;
            ctxt.index().indexDataset(moved, doNormalSolrDocCleanUp);

        } catch (Exception e) { // RuntimeException e ) {
            logger.log(Level.WARNING, "Exception while indexing:" + e.getMessage()); //, e);
            throw new CommandException("Dataset could not be moved. Indexing failed", this);

        }

    }

}
