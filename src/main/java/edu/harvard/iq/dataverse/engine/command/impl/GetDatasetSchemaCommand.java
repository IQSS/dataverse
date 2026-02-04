
package edu.harvard.iq.dataverse.engine.command.impl;


import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;


import java.util.logging.Logger;

/**
 *
 * @author stephenkraffmiller
 */
@RequiredPermissions(Permission.AddDataset)
public class GetDatasetSchemaCommand extends AbstractCommand<String> {
    
    private static final Logger logger = Logger.getLogger(GetDatasetSchemaCommand.class.getCanonicalName());
    
    private final Dataverse dataverse;
    
    public GetDatasetSchemaCommand(DataverseRequest aRequest, Dataverse target) {
        super(aRequest, target);
        dataverse = target;
    }

    @Override
    public String execute(CommandContext ctxt) throws CommandException {            
            return ctxt.dataverses().getCollectionDatasetSchema(dataverse.getAlias());                   
    }
    
}
