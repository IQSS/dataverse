
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.TermsOfUseAndAccess;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;


/**
 *
 * @author stephenkraffmiller
 */
@RequiredPermissions(Permission.EditDataset)
public class UpdateDatasetTermsOfAccessCommand  extends AbstractDatasetCommand<Dataset>{
    
    
    private final Dataset dataset;
    private final TermsOfUseAndAccess termsOfUseAndAccess;
    private final UpdateDatasetVersionCommand updateDatasetVersionCommand;
    
    public UpdateDatasetTermsOfAccessCommand(Dataset dataset, TermsOfUseAndAccess termsOfUseAndAccess,  DataverseRequest request) {
        this(dataset, termsOfUseAndAccess,  request, null);
    }

    //Command included for testing purposes
    public UpdateDatasetTermsOfAccessCommand( Dataset dataset, TermsOfUseAndAccess termsOfUseAndAccess, DataverseRequest aRequest, UpdateDatasetVersionCommand updateDatasetVersionCommand ) {
        super(aRequest, dataset);
        this.dataset = dataset;
        this.termsOfUseAndAccess = termsOfUseAndAccess;
        this.updateDatasetVersionCommand = updateDatasetVersionCommand;
    }

    @Override
    public Dataset execute(CommandContext ctxt) throws CommandException {
        DatasetVersion datasetVersion = dataset.getOrCreateEditVersion();
   
        datasetVersion.setTermsOfUseAndAccess(merge(datasetVersion,termsOfUseAndAccess));
         
        datasetVersion.setVersionState(DatasetVersion.VersionState.DRAFT);
        return ctxt.engine().submit(updateDatasetVersionCommand == null ? new UpdateDatasetVersionCommand(this.dataset, getRequest()) : updateDatasetVersionCommand);
    }
    
    private TermsOfUseAndAccess merge(DatasetVersion editVersion, TermsOfUseAndAccess incoming) {
        //only update the access parts
        TermsOfUseAndAccess termsToUpdate = editVersion.getTermsOfUseAndAccess();
        termsToUpdate.setFileAccessRequest(incoming.isFileAccessRequest());
        termsToUpdate.setTermsOfAccess(incoming.getTermsOfAccess());
        termsToUpdate.setDataAccessPlace(incoming.getDataAccessPlace());
        termsToUpdate.setOriginalArchive(incoming.getOriginalArchive());
        termsToUpdate.setAvailabilityStatus(incoming.getAvailabilityStatus());
        termsToUpdate.setContactForAccess(incoming.getContactForAccess());
        termsToUpdate.setSizeOfCollection(incoming.getSizeOfCollection());
        termsToUpdate.setStudyCompletion(incoming.getStudyCompletion());
        return termsToUpdate;
    }
    
    
}
