package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.authorization.Permission;

import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.DatasetFieldUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author skraffmiller
 * Creates a template {@link Template} for a {@link Dataverse}.
 */
@RequiredPermissions(Permission.EditDataverse)
public class CreateTemplateCommand extends AbstractCommand<Template> {
    private final Template template;
    private final Dataverse dataverse;

    private boolean initialize = false;

    public CreateTemplateCommand(Template template, DataverseRequest request, Dataverse dataverse) {
        this(template, request, dataverse, false);
    }

    public CreateTemplateCommand(Template template, DataverseRequest request, Dataverse dataverse, boolean initialize) {
        super(request, dataverse);
        this.template = template;
        this.dataverse = dataverse;
        this.initialize = initialize;
    }

    @Override
    public Template execute(CommandContext ctxt) throws CommandException {
        if (initialize) {
            template.setMetadataValueBlocks(getSystemMetadataBlocks(ctxt));

            updateTermsOfUseAndAccess(ctxt, template);
            updateDatasetFieldInputLevels(template, ctxt);

            DatasetFieldUtil.tidyUpFields(template.getDatasetFields(), false);
        }

        return ctxt.templates().save(template);
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
