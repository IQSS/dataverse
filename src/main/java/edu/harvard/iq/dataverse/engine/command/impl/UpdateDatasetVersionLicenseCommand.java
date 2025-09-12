package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.TermsOfUseAndAccess;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.*;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.InvalidCommandArgumentsException;
import edu.harvard.iq.dataverse.license.License;
import edu.harvard.iq.dataverse.util.BundleUtil;

import java.util.List;

@RequiredPermissions(Permission.EditDataset)
public class UpdateDatasetVersionLicenseCommand extends AbstractVoidCommand {
    private final Dataset dataset;
    private final License license;

    public UpdateDatasetVersionLicenseCommand(DataverseRequest aRequest, Dataset dataset, License license) {
        super(aRequest, dataset);
        this.dataset = dataset;
        this.license = license;
    }

    @Override
    protected void executeImpl(CommandContext ctxt) throws CommandException {
        if (!license.isActive()) {
            throw new InvalidCommandArgumentsException(BundleUtil.getStringFromBundle("updateDatasetVersionLicenseCommand.errors.licenseNotActive", List.of(license.getName())), this);
        }

        DatasetVersion datasetVersion = dataset.getOrCreateEditVersion();

        TermsOfUseAndAccess termsOfUseAndAccess = datasetVersion.getTermsOfUseAndAccess();
        termsOfUseAndAccess.setLicense(license);

        datasetVersion.setVersionState(DatasetVersion.VersionState.DRAFT);

        ctxt.engine().submit(new UpdateDatasetVersionCommand(this.dataset, getRequest()));
    }
}
