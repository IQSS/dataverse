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
import org.eclipse.persistence.exceptions.DatabaseException;

/**
 * A superuser-only command for creating new dataset relation types.
 *
 * @author Vera Clemens (ZB MED)
 */
@RequiredPermissions({})
public class CreateDatasetRelationTypeCommand extends AbstractVoidCommand {
    private final DatasetRelationType relationType;

    public CreateDatasetRelationTypeCommand(DataverseRequest request, DatasetRelationType relationType) {
        super(request, (DvObject) null);
        this.relationType = relationType;
    }

    @Override
    protected void executeImpl(CommandContext ctxt) throws CommandException, InvalidCommandArgumentsException, IllegalCommandException {
        if (!getUser().isSuperuser()) {
            throw new IllegalCommandException(BundleUtil.getStringFromBundle("datasets.api.datasetRelationType.error.create.superuserOnly"), this);
        }

        try {
            ctxt.datasetRelationTypes().save(relationType);
        } catch (DatasetRelationTypeException ex) {
            throw new InvalidCommandArgumentsException(ex.getMessage(), this);
        } catch (EJBException ex) {
            throw new CommandException(BundleUtil.getStringFromBundle("datasets.api.datasetRelationType.error.create"), ex, this);
        }
    }
}
