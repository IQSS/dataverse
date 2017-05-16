package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder;
import static edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder.jsonObjectBuilder;
import java.util.Collections;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObjectBuilder;

/**
 * Always catch a RuntimeException when calling this command, which may occur on
 * any problem contacting the Data Capture Module! We have to throw a
 * RuntimeException because otherwise ctxt.engine().submit() will put "OK" for
 * "actiontype" in the actionlogrecord rather than "InternalError" if you throw
 * a CommandExecutionException.
 *
 * @todo Who is responsible for knowing when it's appropriate to create an rsync
 * script for a dataset, Dataverse or the Data Capture Module? For now the DCM
 * will always create an rsync script, which may not be what we want.
 */
@RequiredPermissions(Permission.AddDataset)
public class RequestRsyncScriptCommand extends AbstractCommand<JsonObjectBuilder> {

    private static final Logger logger = Logger.getLogger(RequestRsyncScriptCommand.class.getCanonicalName());

    private final Dataset dataset;
    private final DataverseRequest request;

    public RequestRsyncScriptCommand(DataverseRequest requestArg, Dataset datasetArg) {
        super(requestArg, datasetArg);
        request = requestArg;
        dataset = datasetArg;
    }

    @Override
    public JsonObjectBuilder execute(CommandContext ctxt) throws CommandException {
        // {"dep_email": "bob.smith@example.com", "uid": 42, "depositor_name": ["Smith", "Bob"], "lab_email": "john.doe@example.com", "datacite.resourcetype": "X-Ray Diffraction"}
        User user = request.getUser();
        if (!(user instanceof AuthenticatedUser)) {
            /**
             * @todo get Permission.AddDataset from above somehow rather than
             * duplicating it here.
             */
            throw new PermissionException("This command can only be called by an AuthenticatedUser, not " + user,
                    this, Collections.singleton(Permission.AddDataset), dataset);
        }
        AuthenticatedUser au = (AuthenticatedUser) user;
        HttpResponse<JsonNode> response;
        /**
         * @todo Refactor this building of JSON to make it testable.
         */
        JsonObjectBuilder jab = Json.createObjectBuilder();
        // The general rule should be to always pass the user id and dataset id to the DCM.
        jab.add("userId", au.getId());
        jab.add("datasetId", dataset.getId());
        String errorPreamble = "User id " + au.getId() + " had a problem retrieving rsync script for dataset id " + dataset.getId() + " from Data Capture Module. ";
        try {
            response = ctxt.dataCaptureModule().requestRsyncScriptCreation(au, dataset, jab);
        } catch (Exception ex) {
            throw new RuntimeException(errorPreamble + ex.getLocalizedMessage(), ex);
        }
        int statusCode = response.getStatus();
        /**
         * @todo Since we're creating something, maybe a 201 response would be
         * more appropriate.
         */
        if (statusCode != 200) {
            /**
             * @todo is the body too big to fit in the actionlogrecord? The
             * column length on "info" is 1024. See also
             * https://github.com/IQSS/dataverse/issues/2669
             */
            throw new RuntimeException(errorPreamble + "Rather than 200 the status code was " + statusCode + ". The body was \'" + response.getBody() + "\'.");
        }
        String message = response.getBody().getObject().getString("status");
        logger.info("Message from Data Caputure Module upload request endpoint: " + message);
        /**
         * @todo Should we persist to the database the fact that we have
         * requested a script? That way we could avoid hitting ur.py (upload
         * request) over and over since it is preferred that we only hit it
         * once.
         */
        /**
         * @todo Don't expect to get the script from ur.py (upload request). Go
         * fetch it from sr.py (script request) after a minute or so. (Cron runs
         * every minute.) Wait 90 seconds to be safe.
         */
        long millisecondsToSleep = 0;
        try {
            Thread.sleep(millisecondsToSleep);
        } catch (InterruptedException ex) {
            throw new RuntimeException(errorPreamble + "Unable to wait " + millisecondsToSleep + " milliseconds: " + ex.getLocalizedMessage());
        }
        try {
            response = ctxt.dataCaptureModule().retreiveRequestedRsyncScript(au, dataset);
        } catch (Exception ex) {
            throw new RuntimeException(errorPreamble + "Problem retrieving rsync script: " + ex.getLocalizedMessage());
        }
        statusCode = response.getStatus();
        if (statusCode != 200) {
            throw new RuntimeException(errorPreamble + "Rather than 200 the status code was " + statusCode + ". The body was \'" + response.getBody() + "\'.");
        }
        /**
         * @todo What happens when no datasetId is in the JSON?
         */
        long datasetId = response.getBody().getObject().getLong("datasetId");
        String script = response.getBody().getObject().getString("script");
        if (script == null || script.isEmpty()) {
            throw new RuntimeException(errorPreamble + "The script was null or empty.");
        }
        logger.fine("script for dataset " + datasetId + ": " + script);
        Dataset updatedDataset = ctxt.dataCaptureModule().persistRsyncScript(dataset, script);
        NullSafeJsonBuilder nullSafeJsonBuilder = jsonObjectBuilder()
                .add("datasetId", datasetId)
                .add("script", script);
        return nullSafeJsonBuilder;
    }

}
