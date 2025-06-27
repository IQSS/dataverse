package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddress;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;

import java.util.List;
import java.util.logging.Logger;

import static edu.harvard.iq.dataverse.util.json.JsonPrinter.json;

@RequiredPermissions({})
public class GetUserPermittedCollectionsCommand extends AbstractCommand<JsonObjectBuilder> {
    private static final Logger logger = Logger.getLogger(GetUserPermittedCollectionsCommand.class.getCanonicalName());

    private DataverseRequest request;
    private AuthenticatedUser user;
    private String permission;
    public GetUserPermittedCollectionsCommand(DataverseRequest request, AuthenticatedUser user, String permission) {
        super(request, (DvObject) null);
        this.request = request;
        this.user = user;
        this.permission = permission;
    }

    @Override
    public JsonObjectBuilder execute(CommandContext ctxt) throws CommandException {
        if (user == null) {
            throw new CommandException("User not found.", this);
        }
        int permissionBit;
        try {
            permissionBit = permission.equalsIgnoreCase("any") ?
                    Integer.MAX_VALUE : (1 << Permission.valueOf(permission).ordinal());
        } catch (IllegalArgumentException e) {
            throw new CommandException("Permission not valid.", this);
        }
        List<Dataverse> collections = ctxt.permissions().findPermittedCollections(request, user, permissionBit);
        if (collections != null) {
            JsonObjectBuilder job = Json.createObjectBuilder();
            JsonArrayBuilder jab = Json.createArrayBuilder();
            for (Dataverse dv : collections) {
                jab.add(json(dv));
            }
            job.add("count", collections.size());
            job.add("items", jab);
            return job;
        }
        return null;
    }
}
