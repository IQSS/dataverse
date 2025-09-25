/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.TermsOfUseAndAccess;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.InvalidCommandArgumentsException;
import edu.harvard.iq.dataverse.util.BundleUtil;

/**
 *
 * @author stephenkraffmiller
 */
@RequiredPermissions(Permission.EditDataset)
public class UpdateDatasetTermsOfUseCommand  extends AbstractDatasetCommand<Dataset>{
    
    
    private final Dataset dataset;
    private final TermsOfUseAndAccess termsOfUseAndAccess;
    private final UpdateDatasetVersionCommand updateDatasetVersionCommand;
    
    public UpdateDatasetTermsOfUseCommand(Dataset dataset, TermsOfUseAndAccess termsOfUseAndAccess,  DataverseRequest request) {
        this(dataset, termsOfUseAndAccess,  request, null);
    }

    //Command included for testing purposes
    public UpdateDatasetTermsOfUseCommand( Dataset dataset, TermsOfUseAndAccess termsOfUseAndAccess, DataverseRequest aRequest, UpdateDatasetVersionCommand updateDatasetVersionCommand ) {
        super(aRequest, dataset);
        this.dataset = dataset;
        this.termsOfUseAndAccess = termsOfUseAndAccess;
        this.updateDatasetVersionCommand = updateDatasetVersionCommand;
    }

    @Override
    public Dataset execute(CommandContext ctxt) throws CommandException {
        DatasetVersion datasetVersion = dataset.getOrCreateEditVersion();

        
        datasetVersion.setTermsOfUseAndAccess(termsOfUseAndAccess);
         
        datasetVersion.setVersionState(DatasetVersion.VersionState.DRAFT);
        termsOfUseAndAccess.setDatasetVersion(datasetVersion);
        System.out.print("From input....");
        System.out.print(termsOfUseAndAccess.getConfidentialityDeclaration());
        System.out.print("From DatasetVersion....");
        System.out.print(datasetVersion.getTermsOfUseAndAccess().getConfidentialityDeclaration());
        System.out.print("$$$$$$$$$$$$$$$$$$$$$$$$");
        return ctxt.engine().submit(updateDatasetVersionCommand == null ? new UpdateDatasetVersionCommand(this.dataset, getRequest()) : updateDatasetVersionCommand);
    }
    
}
