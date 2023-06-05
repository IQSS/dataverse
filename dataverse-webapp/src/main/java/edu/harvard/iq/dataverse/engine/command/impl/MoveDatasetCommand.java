/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissionsMap;
import edu.harvard.iq.dataverse.engine.command.exception.move.AdditionalMoveStatus;
import edu.harvard.iq.dataverse.engine.command.exception.move.DatasetMoveStatus;
import edu.harvard.iq.dataverse.engine.command.exception.move.MoveException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.link.DatasetLinkingDataverse;
import edu.harvard.iq.dataverse.persistence.guestbook.Guestbook;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.Permission;

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
@RequiredPermissionsMap({
        @RequiredPermissions(dataverseName = "moved", value = {Permission.PublishDataset})
        , @RequiredPermissions(dataverseName = "destination", value = {Permission.AddDataset, Permission.PublishDataset})
})
public class MoveDatasetCommand extends AbstractVoidCommand {

    private static final Logger logger = Logger.getLogger(MoveDatasetCommand.class.getCanonicalName());
    final Dataset moved;
    final Dataverse destination;
    final Boolean force;

    public MoveDatasetCommand(DataverseRequest aRequest, Dataset moved, Dataverse destination, Boolean force) {
        super(
                aRequest,
                dv("moved", moved),
                dv("destination", destination)
        );
        this.moved = moved;
        this.destination = destination;
        this.force = force;
    }

    @Override
    public void executeImpl(CommandContext ctxt)  {
        boolean removeGuestbook = false, removeLinkDs = false;
        if (!(getUser() instanceof AuthenticatedUser)) {
            throw new PermissionException("Move Dataset can only be called by authenticated users.", this, Collections.singleton(Permission.DeleteDatasetDraft), moved);
        }

        // validate the move makes sense
        if (moved.getOwner().equals(destination)) {
            throw new MoveException("Dataset already in this Dataverse ", this,
                    DatasetMoveStatus.ALREADY_IN_TARGET_DATAVERSE);
        }

        // if dataset is published make sure that its target is published
        if (moved.isReleased() && !destination.isReleased()) {
            throw new MoveException("Published Dataset may not be moved to unpublished Dataverse. You may publish "
                    + destination.getDisplayName() + " and re-try the move.", this,
                    DatasetMoveStatus.UNPUBLISHED_TARGET_DATAVERSE);
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
                if (force == null || !force) {
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
            for (Dataverse owner : ownersToCheck) {
                if ((dsld.getLinkingDataverse()).equals(owner)) {
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
            List<AdditionalMoveStatus> exceptionDetails = new ArrayList<>();
            if (removeGuestbook) {
                errorString.append("Dataset guestbook is not in target dataverse. ");
                exceptionDetails.add(DatasetMoveStatus.NO_GUESTBOOK_IN_TARGET_DATAVERSE);
            }
            if (removeLinkDs) {
                errorString.append("Dataset is linked to target dataverse or one of its parents. ");
                exceptionDetails.add(DatasetMoveStatus.LINKED_TO_TARGET_DATAVERSE);
            }
            throw new MoveException(errorString
                    + "Please use the parameter ?forceMove=true to complete the move. This will remove anything from " +
                    "the dataset that is not compatible with the target dataverse.", this, exceptionDetails);
        }

        // OK, move
        moved.setOwner(destination);
        ctxt.em().merge(moved);

        try {
            boolean doNormalSolrDocCleanUp = true;
            ctxt.index().indexDataset(moved, doNormalSolrDocCleanUp);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Exception while indexing:" + e.getMessage());
            throw new MoveException("Dataset could not be moved. Indexing failed", this,
                    DatasetMoveStatus.INDEXING_ISSUE);
        }
    }
}