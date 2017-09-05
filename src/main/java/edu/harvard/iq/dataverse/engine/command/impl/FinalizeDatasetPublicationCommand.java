package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldConstant;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.IdServiceBean;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.export.ExportException;
import edu.harvard.iq.dataverse.export.ExportService;
import edu.harvard.iq.dataverse.privateurl.PrivateUrl;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.workflow.WorkflowContext.TriggerType;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * Takes the last internal steps in publishing a dataset.
 *
 * @author michael
 */
@RequiredPermissions(Permission.PublishDataset)
public class FinalizeDatasetPublicationCommand extends AbstractPublishDatasetCommand<Dataset> {

    private static final Logger logger = Logger.getLogger(FinalizeDatasetPublicationCommand.class.getName());
    
    String doiProvider;
    
    public FinalizeDatasetPublicationCommand(Dataset aDataset, String aDoiProvider, DataverseRequest aRequest) {
        super(aDataset, aRequest);
        theDataset = aDataset;
        doiProvider = aDoiProvider;
    }

    @Override
    public Dataset execute(CommandContext ctxt) throws CommandException {
        updateParentDataversesSubjectsField(theDataset, ctxt);
        publicizeExternalIdentifier(theDataset, ctxt);

        PrivateUrl privateUrl = ctxt.engine().submit(new GetPrivateUrlCommand(getRequest(), theDataset));
        if (privateUrl != null) {
            ctxt.engine().submit(new DeletePrivateUrlCommand(getRequest(), theDataset));
        }
        theDataset.getEditVersion().setVersionState(DatasetVersion.VersionState.RELEASED);
        // moved the following line from PublishDatasetCommand -- L.A. Sep. 5 2017
        theDataset.setPublicationDate(new Timestamp(new Date().getTime()));

        exportMetadata();
        boolean doNormalSolrDocCleanUp = true;
        ctxt.index().indexDataset(theDataset, doNormalSolrDocCleanUp);
        ctxt.solrIndex().indexPermissionsForOneDvObject(theDataset);

        ctxt.engine().submit(new RemoveLockCommand(getRequest(), theDataset));
        
        ctxt.workflows().getDefaultWorkflow(TriggerType.PostPublishDataset)
                .ifPresent(wf -> ctxt.workflows().start(wf, buildContext(doiProvider, TriggerType.PostPublishDataset)));
        
        return ctxt.em().merge(theDataset);
    }

    /**
     * Attempting to run metadata export, for all the formats for which we have
     * metadata Exporters.
     */
    private void exportMetadata() {

        try {
            ExportService instance = ExportService.getInstance();
            instance.exportAllFormats(theDataset);

        } catch (ExportException ex) {
            // Something went wrong!
            // Just like with indexing, a failure to export is not a fatal
            // condition. We'll just log the error as a warning and keep
            // going:
            logger.log(Level.WARNING, "Dataset publication finalization: exception while exporting:{0}", ex.getMessage());
        }
    }

    /**
     * add the dataset subjects to all parent dataverses.
     */
    private void updateParentDataversesSubjectsField(Dataset savedDataset, CommandContext ctxt) {
        for (DatasetField dsf : savedDataset.getLatestVersion().getDatasetFields()) {
            if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.subject)) {
                Dataverse dv = savedDataset.getOwner();
                while (dv != null) {
                    if (dv.getDataverseSubjects().addAll(dsf.getControlledVocabularyValues())) {
                        Dataverse dvWithSubjectJustAdded = ctxt.em().merge(dv);
                        ctxt.em().flush();
                        ctxt.index().indexDataverse(dvWithSubjectJustAdded); // need to reindex to capture the new subjects
                    }
                    dv = dv.getOwner();
                }
                break;
            }
        }
    }

    private void publicizeExternalIdentifier(Dataset dataset, CommandContext ctxt) throws CommandException {
        String protocol = theDataset.getProtocol();
        IdServiceBean idServiceBean = IdServiceBean.getBean(protocol, ctxt);
        if (idServiceBean!= null )
        try {
            idServiceBean.publicizeIdentifier(dataset);
        } catch (Throwable e) {
            throw new CommandException(BundleUtil.getStringFromBundle("dataset.publish.error", idServiceBean.getProviderInformation()),this); 
        }
    }
}
