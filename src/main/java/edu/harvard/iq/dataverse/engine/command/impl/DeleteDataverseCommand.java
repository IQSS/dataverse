package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.dataverse.featured.DataverseFeaturedItem;
import edu.harvard.iq.dataverse.DataverseFieldTypeInputLevel;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroup;
import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissionsMap;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.search.DvObjectSolrDoc;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import java.util.ArrayList;
import java.util.List;

/**
 * Deletes a {@link Dataverse} - but only if it is empty.
 *
 * @author michael
 */
@RequiredPermissionsMap({
    @RequiredPermissions(dataverseName = "doomed", value = Permission.DeleteDataverse)
})
public class DeleteDataverseCommand extends AbstractVoidCommand {

    private final Dataverse doomed;

    public DeleteDataverseCommand(DataverseRequest aRequest, Dataverse aDoomedDataverse) {
        super(aRequest, dv("doomed", aDoomedDataverse), dv("owner", aDoomedDataverse.getOwner()));
        doomed = aDoomedDataverse;
    }

    @Override
    protected void executeImpl(CommandContext ctxt) throws CommandException {
        // Make sure we don't delete root
        if (doomed.getOwner() == null) {
            throw new IllegalCommandException("Cannot delete the root dataverse", this);
        }
        
        // make sure the dataverse is emptyw
        if (ctxt.dvObjects().hasData(doomed)) {
            throw new IllegalCommandException("Cannot delete non-empty dataverses", this);
        }

	// if we got here, we can delete
	// Metadata blocks - cant delete metadatablocks
         /* Don't seem to need to do this SEK 10/23/14
         for (MetadataBlock block : doomed.getMetadataBlocks(true) ) {
         MetadataBlock merged =  ctxt.em().merge(block);
         ctxt.em().remove(merged);
         } */
        
        // ASSIGNMENTS
        for ( RoleAssignment ra : ctxt.roles().directRoleAssignments(doomed) ) {
            ctxt.em().remove(ra);
        }
        // ROLES
        for ( DataverseRole ra : ctxt.roles().findByOwnerId(doomed.getId()) ) {
            ctxt.em().remove(ra);
        }
        
        // EXPLICIT GROUPS
        for ( ExplicitGroup eg : ctxt.em().createNamedQuery("ExplicitGroup.findByOwnerId", ExplicitGroup.class)
                                          .setParameter("ownerId", doomed.getId())
                                          .getResultList() ) {
            ctxt.explicitGroups().removeGroup(eg);
        }
        // FACETS handled with cascade on dataverse

        // Input Level
        for (DataverseFieldTypeInputLevel inputLevel : doomed.getDataverseFieldTypeInputLevels()) {
            DataverseFieldTypeInputLevel merged = ctxt.em().merge(inputLevel);
            ctxt.em().remove(merged);
        }
        doomed.setDataverseFieldTypeInputLevels(new ArrayList<>());

        // Featured Items
        for (DataverseFeaturedItem featuredItem : ctxt.dataverseFeaturedItems().findAllByDataverseOrdered(doomed, false) ) {
            DataverseFeaturedItem merged = ctxt.em().merge(featuredItem);
            ctxt.em().remove(merged);
        }
        doomed.setDataverseFeaturedItems(new ArrayList<>());

        // DATAVERSE
        Dataverse doomedAndMerged = ctxt.em().merge(doomed);
        ctxt.em().remove(doomedAndMerged);
    }

    @Override 
    public boolean onSuccess(CommandContext ctxt, Object r) {

        // Remove from index        
        ctxt.index().delete(doomed);
        List<String> solrIdsToDelete = new ArrayList<>();
        List<DvObjectSolrDoc> definitionPoints = ctxt.solrIndex().determineSolrDocs(doomed);
        definitionPoints.forEach(dvObjectSolrDoc -> {
            boolean add = solrIdsToDelete.add(dvObjectSolrDoc.getSolrId() + IndexServiceBean.discoverabilityPermissionSuffix);
        });
        var deleteMultipleSolrIds = ctxt.solrIndex().deleteMultipleSolrIds(solrIdsToDelete);
        /**
        * @todo: this method currently always returns true because the 
        * underlying methods (already existing) handle exceptions and don't 
        * return a boolean value
        * we need to consider reworking the code such that methods throw
        * indexing exception to callers that may need to handle effects such
        * as on data integrity where related operations like database updates
        * or deletes are expected to be coordinated with indexing operations
        */
        return true;
    }

}
