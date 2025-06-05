package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import static edu.harvard.iq.dataverse.dataset.DatasetUtil.deleteDatasetLogo;
import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.pidproviders.PidProvider;
import edu.harvard.iq.dataverse.pidproviders.PidUtil;
import edu.harvard.iq.dataverse.search.IndexResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.harvard.iq.dataverse.batch.util.LoggingUtil;
import java.io.IOException;
import java.util.concurrent.Future;
import org.apache.solr.client.solrj.SolrServerException;

/**
 * Same as {@link DeleteDatasetCommand}, but does not stop if the dataset is
 * published. This command is reserved for super-users, if at all.
 *
 * @author michael
 */
// Since this is used by DeleteDatasetCommand, must have at least that permission
// (for released, user is checked for superuser)
@RequiredPermissions( Permission.DeleteDatasetDraft )
public class DestroyDatasetCommand extends AbstractVoidCommand {

    private static final Logger logger = Logger.getLogger(DestroyDatasetCommand.class.getCanonicalName());

    private final Dataset doomed;
    
    private List<String> datasetAndFileSolrIdsToDelete; 
    
    private List<Dataverse> toReIndex;

    public DestroyDatasetCommand(Dataset doomed, DataverseRequest aRequest) {
        super(aRequest, doomed);
        this.doomed = doomed;
        datasetAndFileSolrIdsToDelete = new ArrayList<>();
        toReIndex = new ArrayList<>();
    }

    @Override
    protected void executeImpl(CommandContext ctxt) throws CommandException {

        // first check if dataset is released, and if so, if user is a superuser
        if ( doomed.isReleased() && (!(getUser() instanceof AuthenticatedUser) || !getUser().isSuperuser() ) ) {      
            throw new PermissionException("Destroy can only be called by superusers.",
                this,  Collections.singleton(Permission.DeleteDatasetDraft), doomed);                
        }
        Dataset managedDoomed = ctxt.em().merge(doomed);
        
        // If there is a dedicated thumbnail DataFile, it needs to be reset
        // explicitly, or we'll get a constraint violation when deleting:
        managedDoomed.setThumbnailFile(null);

        // files need to iterate through and remove 'by hand' to avoid
        // optimistic lock issues... (plus the physical files need to be 
        // deleted too!)
        DatasetVersion dv = managedDoomed.getLatestVersion();
        Iterator <DataFile> dfIt = managedDoomed.getFiles().iterator();
        while (dfIt.hasNext()){
            DataFile df = dfIt.next();
            // Gather potential Solr IDs of files. As of this writing deaccessioned files are never indexed.
            String solrIdOfPublishedFile = IndexServiceBean.solrDocIdentifierFile + df.getId();
            datasetAndFileSolrIdsToDelete.add(solrIdOfPublishedFile);
            String solrIdOfDraftFile = IndexServiceBean.solrDocIdentifierFile + df.getId() + IndexServiceBean.draftSuffix;
            datasetAndFileSolrIdsToDelete.add(solrIdOfDraftFile);
            ctxt.engine().submit(new DeleteDataFileCommand(df, getRequest(), true));
            dfIt.remove();
        }
        dv.setFileMetadatas(null);
        
        
        // ASSIGNMENTS
        for (RoleAssignment ra : ctxt.roles().directRoleAssignments(managedDoomed)) {
            ctxt.em().remove(ra);
        }
        // ROLES
        for (DataverseRole ra : ctxt.roles().findByOwnerId(managedDoomed.getId())) {
            ctxt.em().remove(ra);
        }   
        
        if (!managedDoomed.isHarvested()) {
            //also, lets delete the uploaded thumbnails!
            deleteDatasetLogo(managedDoomed);
            // and remove the PID (perhaps should be after the remove in case that causes a roll-back?)
            GlobalId pid = managedDoomed.getGlobalId();
            if (pid != null) {
                PidProvider pidProvider = PidUtil.getPidProvider(pid.getProviderId());
                try {
                    if (pidProvider.alreadyRegistered(managedDoomed)) {
                        pidProvider.deleteIdentifier(managedDoomed);
                        //Files are handled in DeleteDataFileCommand
                    }
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Identifier deletion was not successful:", e.getMessage());
                }
            }
        }
        
        toReIndex.add(managedDoomed.getOwner());
        managedDoomed.getDatasetLinkingDataverses().forEach(dld -> toReIndex.add(dld.getLinkingDataverse()));

        // add potential Solr IDs of datasets to list for deletion
        String solrIdOfPublishedDatasetVersion = IndexServiceBean.solrDocIdentifierDataset + managedDoomed.getId();
        datasetAndFileSolrIdsToDelete.add(solrIdOfPublishedDatasetVersion);
        String solrIdOfDraftDatasetVersion = IndexServiceBean.solrDocIdentifierDataset + managedDoomed.getId() + IndexServiceBean.draftSuffix;
        datasetAndFileSolrIdsToDelete.add(solrIdOfDraftDatasetVersion);
        String solrIdOfDraftDatasetVersionPermission = solrIdOfDraftDatasetVersion + IndexServiceBean.discoverabilityPermissionSuffix;
        datasetAndFileSolrIdsToDelete.add(solrIdOfDraftDatasetVersionPermission);
        String solrIdOfDeaccessionedDatasetVersion = IndexServiceBean.solrDocIdentifierDataset + managedDoomed.getId() + IndexServiceBean.deaccessionedSuffix;
        datasetAndFileSolrIdsToDelete.add(solrIdOfDeaccessionedDatasetVersion);
        
        // dataset
        ctxt.em().remove(managedDoomed);


    }

    @Override 
    public boolean onSuccess(CommandContext ctxt, Object r) {

        boolean retVal = true;
        
       // all the real Solr work is done here
       // delete orphaned Solr ids
        IndexResponse resultOfSolrDeletionAttempt = ctxt.solrIndex().deleteMultipleSolrIds(datasetAndFileSolrIdsToDelete);
        logger.log(Level.FINE, "Result of attempt to delete dataset and file IDs from the search index: {0}", resultOfSolrDeletionAttempt.getMessage());

        // reindex
        for (Dataverse dv : toReIndex) {
            try {
                    ctxt.index().indexDataverse(dv);
            } catch (IOException | SolrServerException e) {
                String failureLogText = "Post-destroy dataset indexing of an owning or linking dataverse failed. You can kickoff a re-index of this dataverse with: \r\n curl http://localhost:8080/api/admin/index/dataverses/" + dv.getId().toString();
                failureLogText += "\r\n" + e.getLocalizedMessage();
                LoggingUtil.writeOnSuccessFailureLog(this, failureLogText,  dv);
                retVal = false;
            }
        }
        
        return retVal;
    }

}
