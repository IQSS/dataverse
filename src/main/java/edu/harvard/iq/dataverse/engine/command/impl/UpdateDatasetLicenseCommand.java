package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.TermsOfUseOrLicense;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.*;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.InvalidCommandArgumentsException;
import edu.harvard.iq.dataverse.license.License;
import edu.harvard.iq.dataverse.util.BundleUtil;

import java.util.List;

@RequiredPermissions(Permission.EditDataset)
public class UpdateDatasetLicenseCommand extends AbstractDatasetCommand<Dataset> {
    private License license = null;
    private TermsOfUseOrLicense customTermsOfUseOrLicense = null;

    public UpdateDatasetLicenseCommand(DataverseRequest aRequest, Dataset dataset, License license) {
        super(aRequest, dataset);
        this.license = license;
    }

    public UpdateDatasetLicenseCommand(DataverseRequest aRequest, Dataset dataset, TermsOfUseOrLicense customTermsOfUseOrLicense) {
        super(aRequest, dataset);
        this.customTermsOfUseOrLicense = customTermsOfUseOrLicense;
    }


    @Override
    public Dataset execute(CommandContext ctxt) throws CommandException {
        DatasetVersion datasetVersion = getDataset().getOrCreateEditVersion();
        datasetVersion.setVersionState(DatasetVersion.VersionState.DRAFT);
        Dataset savedDataset = null;

        if (license != null) {
            if (!license.isActive()) {
                throw new InvalidCommandArgumentsException(BundleUtil.getStringFromBundle("updateDatasetLicenseCommand.errors.licenseNotActive", List.of(license.getName())), this);
            }
            TermsOfUseOrLicense termsOfUseOrLicense = datasetVersion.getTermsOfUseOrLicense();
            termsOfUseOrLicense.setLicense(license);

            savedDataset = ctxt.engine().submit(new UpdateDatasetVersionCommand(getDataset(), getRequest()));
        } else if (customTermsOfUseOrLicense != null) {
            if (customTermsOfUseOrLicense.getTermsOfUse() == null || customTermsOfUseOrLicense.getTermsOfUse().isBlank()) {
                throw new InvalidCommandArgumentsException(BundleUtil.getStringFromBundle("updateDatasetLicenseCommand.errors.customTermsOfUseNotProvided"), this);
            }
            TermsOfUseOrLicense termsToUpdate = datasetVersion.getTermsOfUseOrLicense();
            applyCustomTerms(termsToUpdate, customTermsOfUseOrLicense);
            termsToUpdate.setLicense(null);
            datasetVersion.setTermsOfUseOrLicense(termsToUpdate);
            savedDataset = ctxt.engine().submit(new UpdateDatasetVersionCommand(getDataset(), getRequest()));
        }
        return savedDataset;
    }

    /**
     * Copies all custom term-related fields from the 'source' object
     * to the 'target' object.
     *
     * @param target The TermsOfUseOrLicense object to be modified
     * @param source The TermsOfUseOrLicense object containing the new data
     */
    private void applyCustomTerms(TermsOfUseOrLicense target, TermsOfUseOrLicense source) {
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
