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
public class UpdateDatasetLicenseCommand extends AbstractVoidCommand {
    private final Dataset dataset;
    private License license = null;
    private TermsOfUseAndAccess customTermsOfUseAndAccess = null;

    public UpdateDatasetLicenseCommand(DataverseRequest aRequest, Dataset dataset, License license) {
        super(aRequest, dataset);
        this.dataset = dataset;
        this.license = license;
    }

    public UpdateDatasetLicenseCommand(DataverseRequest aRequest, Dataset dataset, TermsOfUseAndAccess customTermsOfUseAndAccess) {
        super(aRequest, dataset);
        this.dataset = dataset;
        this.customTermsOfUseAndAccess = customTermsOfUseAndAccess;
    }


    @Override
    protected void executeImpl(CommandContext ctxt) throws CommandException {
        DatasetVersion datasetVersion = dataset.getOrCreateEditVersion();
        datasetVersion.setVersionState(DatasetVersion.VersionState.DRAFT);

        if (license != null) {
            if (!license.isActive()) {
                throw new InvalidCommandArgumentsException(BundleUtil.getStringFromBundle("updateDatasetLicenseCommand.errors.licenseNotActive", List.of(license.getName())), this);
            }
            TermsOfUseAndAccess termsOfUseAndAccess = datasetVersion.getTermsOfUseAndAccess();
            termsOfUseAndAccess.setLicense(license);

            ctxt.engine().submit(new UpdateDatasetVersionCommand(this.dataset, getRequest()));
        } else if (customTermsOfUseAndAccess != null) {
            if (customTermsOfUseAndAccess.getTermsOfUse() == null || customTermsOfUseAndAccess.getTermsOfUse().isBlank()) {
                throw new InvalidCommandArgumentsException(BundleUtil.getStringFromBundle("updateDatasetLicenseCommand.errors.customTermsOfUseNotProvided"), this);
            }
            TermsOfUseAndAccess termsToUpdate = datasetVersion.getTermsOfUseAndAccess();
            applyCustomTerms(termsToUpdate, customTermsOfUseAndAccess);
            termsToUpdate.setLicense(null);
            datasetVersion.setTermsOfUseAndAccess(termsToUpdate);
            ctxt.engine().submit(new UpdateDatasetVersionCommand(this.dataset, getRequest()));
        }
    }

    /**
     * Copies all custom term-related fields from the 'source' object
     * to the 'target' object.
     *
     * @param target The TermsOfUseAndAccess object to be modified
     * @param source The TermsOfUseAndAccess object containing the new data
     */
    private void applyCustomTerms(TermsOfUseAndAccess target, TermsOfUseAndAccess source) {
        target.setTermsOfUse(source.getTermsOfUse());
        target.setConfidentialityDeclaration(source.getConfidentialityDeclaration());
        target.setSpecialPermissions(source.getSpecialPermissions());
        target.setRestrictions(source.getRestrictions());
        target.setCitationRequirements(source.getCitationRequirements());
        target.setDepositorRequirements(source.getDepositorRequirements());
        target.setConditions(source.getConditions());
        target.setDisclaimer(source.getDisclaimer());
    }
}
