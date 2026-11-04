package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.Template;
import edu.harvard.iq.dataverse.TermsOfAccess;
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
    
    private TermsOfAccess customTermsOfAccess = null;
    private Template template;
   
    public UpdateTemplateTermsOfAccessCommand(DataverseRequest request, Template template, Dataverse dataverse, TermsOfAccess customTermsOfAccess) {
        super(request, dataverse);
        this.template = template;
        this.customTermsOfAccess = customTermsOfAccess;
    }

    @Override
    public Template execute(CommandContext ctxt) throws CommandException {
        Template savedTemplate;

        if (customTermsOfAccess == null) {
            throw new InvalidCommandArgumentsException(BundleUtil.getStringFromBundle("updateDatasetLicenseCommand.errors.customTermsOfUseNotProvided"), this);
        }

        TermsOfAccess termsToUpdate = template.getTermsOfAccess();
        applyCustomTermsOfAccess(termsToUpdate, customTermsOfAccess);
        template.setTermsOfAccess(termsToUpdate);
        savedTemplate = ctxt.templates().save(template);

        return savedTemplate;
    }
    
    private void applyCustomTermsOfAccess(TermsOfAccess target, TermsOfAccess source) {
        
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
