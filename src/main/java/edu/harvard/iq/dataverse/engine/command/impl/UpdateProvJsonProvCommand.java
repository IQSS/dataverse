package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;

@RequiredPermissions(Permission.EditDataset)
public class UpdateProvJsonProvCommand extends AbstractCommand<JsonObjectBuilder> {

    private static final Logger logger = Logger.getLogger(UpdateProvJsonProvCommand.class.getCanonicalName());

    private final DataFile dataFile;
    private final String userInput;

    public UpdateProvJsonProvCommand(DataverseRequest aRequest, DataFile dataFile, String userInput) {
        super(aRequest, dataFile);
        this.dataFile = dataFile;
        this.userInput = userInput;
    }

    //Calls the already existing delete and create commands for updating
    @Override
    public JsonObjectBuilder execute(CommandContext ctxt) throws CommandException {
        //MAD: Do/Can I catch the errors from these and throw them in a way specific to update?
        //MAD: If the first one fails will the second one go
        
        ctxt.engine().submit(new DeleteProvJsonProvCommand(getRequest(), dataFile));
        return ctxt.engine().submit(new PersistProvJsonProvCommand(getRequest(), dataFile, userInput));
        
    }

}
