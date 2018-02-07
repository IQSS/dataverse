/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
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

    public MoveDatasetCommand(DataverseRequest aRequest, Dataset moved, Dataverse destination) {
        super(aRequest, moved);
        this.moved = moved;
        this.destination = destination;
    }

    @Override
    public void executeImpl(CommandContext ctxt) throws CommandException {
        
       // first check if  user is a superuser
        if ( (!(getUser() instanceof AuthenticatedUser) || !getUser().isSuperuser() ) ) {      
            throw new PermissionException("Move Dataset can only be called by superusers.",
                this,  Collections.singleton(Permission.DeleteDatasetDraft), moved);                
        }
        
        
        // validate the move makes sense
        if (moved.getOwner().equals(destination)) {
            throw new IllegalCommandException("Dataset already in this Dataverse ", this);
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
                moved.setGuestbook(null);
            }
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
