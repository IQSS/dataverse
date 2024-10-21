package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.Dataverse.DataverseType;
import edu.harvard.iq.dataverse.authorization.Permission;

import static edu.harvard.iq.dataverse.dataverse.DataverseUtil.validateDataverseMetadataExternally;

import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;

import java.util.ArrayList;
import java.util.List;

/**
 * Update an existing dataverse.
 *
 * @author michael
 */
@RequiredPermissions(Permission.EditDataverse)
public class UpdateDataverseCommand extends AbstractWriteDataverseCommand {
    private final List<Dataverse> featuredDataverseList;

    private boolean datasetsReindexRequired = false;

    public UpdateDataverseCommand(Dataverse editedDv,
                                  List<DatasetFieldType> facets,
                                  List<Dataverse> featuredDataverses,
                                  DataverseRequest request,
                                  List<DataverseFieldTypeInputLevel> inputLevels) {
        this(editedDv, facets, featuredDataverses, request, inputLevels, null);
    }

    public UpdateDataverseCommand(Dataverse editedDv,
                                  List<DatasetFieldType> facets,
                                  List<Dataverse> featuredDataverses,
                                  DataverseRequest request,
                                  List<DataverseFieldTypeInputLevel> inputLevels,
                                  List<MetadataBlock> metadataBlocks) {
        super(editedDv, editedDv, request, facets, inputLevels, metadataBlocks);
        if (featuredDataverses != null) {
            this.featuredDataverseList = new ArrayList<>(featuredDataverses);
        } else {
            this.featuredDataverseList = null;
        }
    }

    @Override
    protected Dataverse innerExecute(CommandContext ctxt) throws IllegalCommandException {
        // Perform any optional validation steps, if defined:
        if (ctxt.systemConfig().isExternalDataverseValidationEnabled()) {
            // For admins, an override of the external validation step may be enabled:
            if (!(getUser().isSuperuser() && ctxt.systemConfig().isExternalValidationAdminOverrideEnabled())) {
                String executable = ctxt.systemConfig().getDataverseValidationExecutable();
                boolean result = validateDataverseMetadataExternally(dataverse, executable, getRequest());

                if (!result) {
                    String rejectionMessage = ctxt.systemConfig().getDataverseUpdateValidationFailureMsg();
                    throw new IllegalCommandException(rejectionMessage, this);
                }
            }
        }

        Dataverse oldDv = ctxt.dataverses().find(dataverse.getId());

        DataverseType oldDvType = oldDv.getDataverseType();
        String oldDvAlias = oldDv.getAlias();
        String oldDvName = oldDv.getName();

        // We don't want to reindex the children datasets unnecessarily:
        // When these values are changed we need to reindex all children datasets
        // This check is not recursive as all the values just report the immediate parent
        if (!oldDvType.equals(dataverse.getDataverseType())
                || !oldDvName.equals(dataverse.getName())
                || !oldDvAlias.equals(dataverse.getAlias())) {
            datasetsReindexRequired = true;
        }

        if (featuredDataverseList != null) {
            ctxt.featuredDataverses().deleteFeaturedDataversesFor(dataverse);
            int i = 0;
            for (Object obj : featuredDataverseList) {
                Dataverse dv = (Dataverse) obj;
                ctxt.featuredDataverses().create(i++, dv.getId(), dataverse.getId());
            }
        }

        return dataverse;
    }

    @Override
    public boolean onSuccess(CommandContext ctxt, Object r) {

        // first kick of async index of datasets
        // TODO: is this actually needed? Is there a better way to handle
        // It appears that we at some point lost some extra logic here, where
        // we only reindex the underlying datasets if one or more of the specific set
        // of fields have been changed (since these values are included in the
        // indexed solr documents for datasets). So I'm putting that back. -L.A.
        Dataverse result = (Dataverse) r;

        if (datasetsReindexRequired) {
            List<Dataset> datasets = ctxt.datasets().findByOwnerId(result.getId());
            ctxt.index().asyncIndexDatasetList(datasets, true);
        }

        return ctxt.dataverses().index((Dataverse) r);
    }
}
