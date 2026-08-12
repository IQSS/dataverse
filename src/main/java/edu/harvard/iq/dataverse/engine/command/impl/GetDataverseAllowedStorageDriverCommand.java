package edu.harvard.iq.dataverse.engine.command.impl;


import static edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder.jsonObjectBuilder;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import jakarta.json.JsonObjectBuilder;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;

@RequiredPermissions(Permission.AddDataverse)
public class GetDataverseAllowedStorageDriverCommand extends AbstractCommand<JsonObjectBuilder> {

    public GetDataverseAllowedStorageDriverCommand(DataverseRequest aRequest, Dataverse dv) {
        super(aRequest, dv);
    }

    @Override
    public JsonObjectBuilder execute(CommandContext ctxt) {

        JsonObjectBuilder bld = jsonObjectBuilder();
        DataAccess.getStorageDriverLabels().entrySet().forEach(s -> bld.add(s.getKey(), s.getValue()));
        return bld;
    }
    
}
