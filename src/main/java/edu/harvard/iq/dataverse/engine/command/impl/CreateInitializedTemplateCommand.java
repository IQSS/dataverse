package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.api.dto.NewTemplateDTO;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.DatasetFieldUtil;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Creates a template {@link Template} for a {@link Dataverse}.
 * TODO: Proper docs
 */
@RequiredPermissions(Permission.EditDataverse)
public class CreateInitializedTemplateCommand extends AbstractCommand<Template> {

    private final Dataverse dataverse;
    private final NewTemplateDTO newTemplateDTO;

    public CreateInitializedTemplateCommand(DataverseRequest request,
                                            Dataverse dataverse,
                                            NewTemplateDTO newTemplateDTO) {
        super(request, dataverse);
        this.dataverse = dataverse;
        this.newTemplateDTO = newTemplateDTO;
    }

    @Override
    public Template execute(CommandContext ctxt) throws CommandException {
        Template template = new Template();

        template.setDataverse(dataverse);
        template.setDatasetFields(newTemplateDTO.getDatasetFields());
        template.setMetadataValueBlocks(getSystemMetadataBlocks(ctxt));
        template.setName(newTemplateDTO.getName());
        template.setInstructionsMap(newTemplateDTO.getInstructionsMap());
        template.updateInstructions();
        template.setCreateTime(new Timestamp(new Date().getTime()));
        template.setUsageCount(0L);

        updateTermsOfUseAndAccess(ctxt, template);
        updateDatasetFieldInputLevels(template, ctxt);

        DatasetFieldUtil.tidyUpFields(template.getDatasetFields(), false);

        dataverse.getTemplates().add(template);

        return ctxt.engine().submit(new CreateTemplateCommand(template, getRequest(), dataverse));
    }

    private static void updateTermsOfUseAndAccess(CommandContext ctxt, Template template) {
        TermsOfUseAndAccess terms = new TermsOfUseAndAccess();
        terms.setFileAccessRequest(true);
        terms.setTemplate(template);
        terms.setLicense(ctxt.licenses().getDefault());
        template.setTermsOfUseAndAccess(terms);
    }

    private void updateDatasetFieldInputLevels(Template template, CommandContext ctxt) {
        Long dvIdForInputLevel = this.dataverse.getId();
        if (!this.dataverse.isMetadataBlockRoot()) {
            dvIdForInputLevel = this.dataverse.getMetadataRootId();
        }

        for (DatasetField dsf : template.getFlatDatasetFields()) {
            DataverseFieldTypeInputLevel inputLevel = ctxt.fieldTypeInputLevels().findByDataverseIdDatasetFieldTypeId(
                    dvIdForInputLevel,
                    dsf.getDatasetFieldType().getId()
            );
            if (inputLevel != null) {
                dsf.setInclude(inputLevel.isInclude());
            } else {
                dsf.setInclude(true);
            }
        }
    }

    private static List<MetadataBlock> getSystemMetadataBlocks(CommandContext ctxt) {
        List<MetadataBlock> systemMetadataBlocks = new ArrayList<>();
        for (MetadataBlock mdb : ctxt.metadataBlocks().listMetadataBlocks()) {
            JvmSettings.MDB_SYSTEM_KEY_FOR.lookupOptional(mdb.getName()).ifPresent(smdbString -> systemMetadataBlocks.add(mdb));
        }
        return systemMetadataBlocks;
    }
}
