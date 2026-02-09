/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.Template;
import edu.harvard.iq.dataverse.TermsOfUseAndAccess;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.InvalidCommandArgumentsException;
import edu.harvard.iq.dataverse.license.License;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.util.List;

/**
 *
 * @author stephenkraffmiller
 */
@RequiredPermissions(Permission.EditDataverse)
public class UpdateTemplateLicenseCommand extends AbstractCommand<Template>{
    
    private License license = null;
    private TermsOfUseAndAccess customTermsOfUseAndAccess = null;
    private Template template;

    public UpdateTemplateLicenseCommand(DataverseRequest request, Template template, Dataverse dataverse, License license) {
        super(request, dataverse);
        this.template = template;
        this.license = license;
    }
    
    public UpdateTemplateLicenseCommand(DataverseRequest request, Template template, Dataverse dataverse, TermsOfUseAndAccess customTermsOfUseAndAccess) {
        super(request, dataverse);
        this.template = template;
        this.customTermsOfUseAndAccess = customTermsOfUseAndAccess;
    }

    @Override
    public Template execute(CommandContext ctxt) throws CommandException {
        Template savedTemplate;
        
        if (license == null && customTermsOfUseAndAccess == null) {
            throw new InvalidCommandArgumentsException(BundleUtil.getStringFromBundle("updateDatasetLicenseCommand.errors.customTermsOfUseNotProvided"), this);
        }

        if (license != null) {
            if (!license.isActive()) {
                throw new InvalidCommandArgumentsException(BundleUtil.getStringFromBundle("updateDatasetLicenseCommand.errors.licenseNotActive", List.of(license.getName())), this);
            }
            TermsOfUseAndAccess termsOfUseAndAccess = template.getTermsOfUseAndAccess();
            termsOfUseAndAccess.setLicense(license);
            savedTemplate = ctxt.templates().save(template);

        } else  {
            if (customTermsOfUseAndAccess.getTermsOfUse() == null || customTermsOfUseAndAccess.getTermsOfUse().isBlank()) {
                throw new InvalidCommandArgumentsException(BundleUtil.getStringFromBundle("updateDatasetLicenseCommand.errors.customTermsOfUseNotProvided"), this);
            }
            TermsOfUseAndAccess termsToUpdate = template.getTermsOfUseAndAccess();
            applyCustomTerms(termsToUpdate, customTermsOfUseAndAccess);
            template.setTermsOfUseAndAccess(termsToUpdate);
            savedTemplate = ctxt.templates().save(template);
        }
        
        return savedTemplate;
        
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
