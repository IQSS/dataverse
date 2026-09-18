package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.datasetrelation.DatasetRelation;
import edu.harvard.iq.dataverse.datasetrelation.DatasetRelationType;
import edu.harvard.iq.dataverse.datasetrelation.DatasetRelationTypeException;
import edu.harvard.iq.dataverse.engine.command.*;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.engine.command.exception.InvalidCommandArgumentsException;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import jakarta.ejb.EJBException;
import jakarta.json.JsonObject;
import jakarta.persistence.PersistenceException;
import jakarta.ws.rs.core.Response;

import java.text.MessageFormat;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;

/**
 * A superuser-only command for deleting dataset relation types.
 *
 * @author Vera Clemens (ZB MED)
 */
@RequiredPermissions({})
public class DeleteDatasetRelationTypeCommand extends AbstractVoidCommand {
    private final DatasetRelationType relationType;

    public DeleteDatasetRelationTypeCommand(DataverseRequest request, DatasetRelationType relationType) {
        super(request, (DvObject) null);
        this.relationType = relationType;
    }

    @Override
    protected void executeImpl(CommandContext ctxt) throws IllegalCommandException, InvalidCommandArgumentsException, CommandException {
        if (!getUser().isSuperuser()) {
            throw new IllegalCommandException(BundleUtil.getStringFromBundle("datasets.api.datasetRelationType.error.delete.superuserOnly"), this);
        }

        try {
            ctxt.datasetRelationTypes().delete(relationType);
        } catch (DatasetRelationTypeException ex) {
            throw new InvalidCommandArgumentsException(ex.getMessage(), this);
        } catch (EJBException ex) {
            throw new CommandException(BundleUtil.getStringFromBundle("datasets.api.datasetRelationType.error.delete"), ex, this);
        }
    }
}
