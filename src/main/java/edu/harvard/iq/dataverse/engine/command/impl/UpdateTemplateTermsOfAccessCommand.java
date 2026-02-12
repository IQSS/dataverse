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
import edu.harvard.iq.dataverse.util.BundleUtil;


/**
 *
 * @author stephenkraffmiller
 */
@RequiredPermissions(Permission.EditDataverse)
public class UpdateTemplateTermsOfAccessCommand extends AbstractCommand<Template> {
    
    private TermsOfUseAndAccess customTermsOfUseAndAccess = null;
    private Template template;
   
    public UpdateTemplateTermsOfAccessCommand(DataverseRequest request, Template template, Dataverse dataverse, TermsOfUseAndAccess customTermsOfUseAndAccess) {
        super(request, dataverse);
        this.template = template;
        this.customTermsOfUseAndAccess = customTermsOfUseAndAccess;
    }

    @Override
    public Template execute(CommandContext ctxt) throws CommandException {
        Template savedTemplate;

        if (customTermsOfUseAndAccess == null) {
            throw new InvalidCommandArgumentsException(BundleUtil.getStringFromBundle("updateDatasetLicenseCommand.errors.customTermsOfUseNotProvided"), this);
        }

        TermsOfUseAndAccess termsToUpdate = template.getTermsOfUseAndAccess();
        applyCustomTerms(termsToUpdate, customTermsOfUseAndAccess);
        template.setTermsOfUseAndAccess(termsToUpdate);
        savedTemplate = ctxt.templates().save(template);

        return savedTemplate;
    }
    
    private void applyCustomTerms(TermsOfUseAndAccess target, TermsOfUseAndAccess source) {
        
        target.setFileAccessRequest(source.isFileAccessRequest());
        target.setTermsOfAccess(source.getTermsOfAccess());
        target.setDataAccessPlace(source.getDataAccessPlace());
        target.setOriginalArchive(source.getOriginalArchive());
        target.setAvailabilityStatus(source.getAvailabilityStatus());
        target.setContactForAccess(source.getContactForAccess());
        target.setSizeOfCollection(source.getSizeOfCollection());
        target.setStudyCompletion(source.getStudyCompletion());
    }
    
}
