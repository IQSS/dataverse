package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.datacapturemodule.DataCaptureModuleServiceBean;
import edu.harvard.iq.dataverse.datacapturemodule.ScriptRequestResponse;
import edu.harvard.iq.dataverse.datacapturemodule.UploadRequestResponse;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * Always catch a RuntimeException when calling this command, which may occur on
 * any problem contacting the Data Capture Module! We have to throw a
 * RuntimeException because otherwise ctxt.engine().submit() will put "OK" for
 * "actiontype" in the actionlogrecord rather than "InternalError" if you throw
 * a CommandExecutionException.
 */
@RequiredPermissions(Permission.AddDataset)
public class RequestRsyncScriptCommand extends AbstractCommand<ScriptRequestResponse> {

    private static final Logger logger = Logger.getLogger(RequestRsyncScriptCommand.class.getCanonicalName());

    private final Dataset dataset;
    private final DataverseRequest request;

    public RequestRsyncScriptCommand(DataverseRequest requestArg, Dataset datasetArg) {
        super(requestArg, datasetArg);
        request = requestArg;
        dataset = datasetArg;
    }

    @Override
    public ScriptRequestResponse execute(CommandContext ctxt) throws CommandException {
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
        String errorPreamble = "User id " + au.getId() + " had a problem retrieving rsync script for dataset id " + dataset.getId() + " from Data Capture Module.";
        UploadRequestResponse uploadRequestResponse = ctxt.dataCaptureModule().requestRsyncScriptCreation(au, dataset);
        int statusCode = uploadRequestResponse.getHttpStatusCode();
        String response = uploadRequestResponse.getResponse();
        if (statusCode != 200) {
            throw new RuntimeException("When making the upload request, rather than 200 the status code was " + statusCode + ". The body was \'" + response + "\'. We cannont proceed. Returning.");
        }
        long millisecondsToSleep = DataCaptureModuleServiceBean.millisecondsToSleepBetweenUploadRequestAndScriptRequestCalls;
        logger.info("Message from Data Caputure Module upload request endpoint: " + response + ". Sleeping " + millisecondsToSleep + "milliseconds before making rsync script request.");
        try {
            Thread.sleep(millisecondsToSleep);
        } catch (InterruptedException ex) {
            throw new RuntimeException(errorPreamble + "Unable to wait " + millisecondsToSleep + " milliseconds: " + ex.getLocalizedMessage());
        }

        ScriptRequestResponse scriptRequestResponse = ctxt.dataCaptureModule().retreiveRequestedRsyncScript(dataset);
        String script = scriptRequestResponse.getScript();
        if (script == null || script.isEmpty()) {
            throw new RuntimeException(errorPreamble + "The script was null or empty.");
        }
        logger.fine("script for dataset " + dataset.getId() + ": " + script);
        return scriptRequestResponse;
    }

}
