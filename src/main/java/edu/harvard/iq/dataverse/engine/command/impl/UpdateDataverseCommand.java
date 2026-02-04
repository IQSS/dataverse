package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.Dataverse.DataverseType;
import edu.harvard.iq.dataverse.api.dto.DataverseDTO;
import edu.harvard.iq.dataverse.authorization.Permission;

import static edu.harvard.iq.dataverse.dataverse.DataverseUtil.validateDataverseMetadataExternally;

import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.util.BundleUtil;

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
    private final DataverseDTO updatedDataverseDTO;

    private boolean datasetsReindexRequired = false;

    public UpdateDataverseCommand(Dataverse dataverse,
                                  List<DatasetFieldType> facets,
                                  List<Dataverse> featuredDataverses,
                                  DataverseRequest request,
                                  List<DataverseFieldTypeInputLevel> inputLevels) {
        this(dataverse, facets, featuredDataverses, request, inputLevels, null, null);
    }

    public UpdateDataverseCommand(Dataverse dataverse,
                                  List<DatasetFieldType> facets,
                                  List<Dataverse> featuredDataverses,
                                  DataverseRequest request,
                                  List<DataverseFieldTypeInputLevel> inputLevels,
                                  List<MetadataBlock> metadataBlocks,
                                  DataverseDTO updatedDataverseDTO) {
        super(dataverse, dataverse, request, facets, inputLevels, metadataBlocks);
        if (featuredDataverses != null) {
            this.featuredDataverseList = new ArrayList<>(featuredDataverses);
        } else {
            this.featuredDataverseList = null;
        }
        this.updatedDataverseDTO = updatedDataverseDTO;
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
        if (!getUser().isSuperuser() && updatedDataverseDTO != null) {
            // default if not set
            if (updatedDataverseDTO.getDatasetFileCountLimit() == null) {
                updatedDataverseDTO.setDatasetFileCountLimit(dataverse.getDatasetFileCountLimit());
            } else if (updatedDataverseDTO.getDatasetFileCountLimit() != dataverse.getDatasetFileCountLimit()) {
                throw new IllegalCommandException(BundleUtil.getStringFromBundle("file.dataset.error.set.file.count.limit"), this);
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

        if (updatedDataverseDTO != null) {
            updateDataverseFromDTO(dataverse, updatedDataverseDTO);
        }

        return dataverse;
    }

    private void updateDataverseFromDTO(Dataverse dataverse, DataverseDTO dto) {
        if (dto.getAlias() != null) {
            dataverse.setAlias(dto.getAlias());
        }
        if (dto.getName() != null) {
            dataverse.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            dataverse.setDescription(dto.getDescription());
        }
        if (dto.getAffiliation() != null) {
            dataverse.setAffiliation(dto.getAffiliation());
        }
        if (dto.getDataverseContacts() != null) {
            dataverse.setDataverseContacts(dto.getDataverseContacts());
            for (DataverseContact dc : dataverse.getDataverseContacts()) {
                dc.setDataverse(dataverse);
            }
        }
        if (dto.getDataverseType() != null) {
            dataverse.setDataverseType(dto.getDataverseType());
        }
        if (dto.getDatasetFileCountLimit() != null) {
            dataverse.setDatasetFileCountLimit(dto.getDatasetFileCountLimit());
        }
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
