/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetLinkingDataverse;
import edu.harvard.iq.dataverse.DatasetLock;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.Guestbook;
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
import edu.harvard.iq.dataverse.engine.command.exception.UnforcedCommandException;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Moves Dataset from one dataverse to another
 *
 * @author skraffmi
 */
@RequiredPermissionsMap({
    @RequiredPermissions(dataverseName = "moved", value = {Permission.PublishDataset})
    ,	@RequiredPermissions(dataverseName = "destination", value = {Permission.AddDataset, Permission.PublishDataset})
})
public class MoveDatasetCommand extends AbstractVoidCommand {

    private static final Logger logger = Logger.getLogger(MoveDatasetCommand.class.getCanonicalName());
    // FIXME: "toMove" would be a better name than "moved".
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
        this.force= force;
    }

    @Override
    public void executeImpl(CommandContext ctxt) throws CommandException {
        boolean removeGuestbook = false, removeLinkDs = false;
        if (!(getUser() instanceof AuthenticatedUser)) {
            /**
             * This English wasn't moved to the bundle because it is impossible
             * to exercise it via both API and UI. See also the note in in the
             * PermissionException catch in AbstractApiBean.
             */
            throw new PermissionException("Move Dataset can only be called by authenticated users.", this, Collections.singleton(Permission.DeleteDatasetDraft), moved);
        }

        // validate the move makes sense
        if (moved.getOwner().equals(destination)) {
            throw new IllegalCommandException(BundleUtil.getStringFromBundle("dashboard.move.dataset.command.error.targetDataverseSameAsOriginalDataverse"), this);
        }
        
        // if dataset is published make sure that its target is published
        
        if (moved.isReleased() && !destination.isReleased()){
            throw new IllegalCommandException(BundleUtil.getStringFromBundle("dashboard.move.dataset.command.error.targetDataverseUnpublishedDatasetPublished", Arrays.asList(destination.getDisplayName())), this);
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
                errorString.append(BundleUtil.getStringFromBundle("dashboard.move.dataset.command.error.unforced.datasetGuestbookNotInTargetDataverse"));
            }
            if (removeLinkDs) {
                errorString.append(BundleUtil.getStringFromBundle("dashboard.move.dataset.command.error.unforced.linkedToTargetDataverseOrOneOfItsParents"));
            }
            throw new UnforcedCommandException(errorString.toString(), this);
        }
        
        // 6575 if dataset is submitted for review and the default contributor
        // role includes dataset publish then remove the lock
        
        if (moved.isLockedFor(DatasetLock.Reason.InReview)
                && destination.getDefaultContributorRole().permissions().contains(Permission.PublishDataset)) {
            ctxt.datasets().removeDatasetLocks(moved, DatasetLock.Reason.InReview);
        }

        // OK, move
        moved.setOwner(destination);
        ctxt.em().merge(moved);

        boolean doNormalSolrDocCleanUp = true;
        ctxt.index().asyncIndexDataset(moved, doNormalSolrDocCleanUp);

    }

}
