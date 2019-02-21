package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.datacapturemodule.DataCaptureModuleException;
import edu.harvard.iq.dataverse.datacapturemodule.DataCaptureModuleServiceBean;
import edu.harvard.iq.dataverse.datacapturemodule.DataCaptureModuleUtil;
import edu.harvard.iq.dataverse.datacapturemodule.ScriptRequestResponse;
import edu.harvard.iq.dataverse.datacapturemodule.UploadRequestResponse;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.DataCaptureModuleUrl;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * Always catch a RuntimeException when calling this command, which may occur on
 * any problem contacting the Data Capture Module! We have to throw a
 * RuntimeException because otherwise ctxt.engine().submit() will put "OK" for
 * "actiontype" in the actionlogrecord rather than "InternalError" if you throw
 * a CommandExecutionException.
 */
@RequiredPermissions(Permission.EditDataset)
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
        if (request == null) {
            throw new IllegalCommandException("DataverseRequest cannot be null.", this);
        }
        if(!dataset.getFiles().isEmpty()){
            throw new IllegalCommandException("Cannot get script for a dataset that already has a file", this);
        }
        String dcmBaseUrl = ctxt.settings().getValueForKey(DataCaptureModuleUrl);
        if (dcmBaseUrl == null) {
            throw new RuntimeException(DataCaptureModuleUrl + " is null!");
        }
        User user = request.getUser();
        if (!(user instanceof AuthenticatedUser)) {
            /**
             * @todo get Permission.AddDataset from above somehow rather than
             * duplicating it here.
             */
            throw new PermissionException("This command can only be called by an AuthenticatedUser, not " + user,
                    this, Collections.singleton(Permission.AddDataset), dataset);
        }
        // We need an AuthenticatedUser so we can pass its database id to the DCM.
        AuthenticatedUser authenticatedUser = (AuthenticatedUser) user;
        String errorPreamble = "User id " + authenticatedUser.getId() + " had a problem retrieving rsync script for dataset id " + dataset.getId() + " from Data Capture Module.";
        String jsonString = DataCaptureModuleUtil.generateJsonForUploadRequest(authenticatedUser, dataset).toString();
        UploadRequestResponse uploadRequestResponse = null;
        try {
            uploadRequestResponse = ctxt.dataCaptureModule().requestRsyncScriptCreation(jsonString, dcmBaseUrl + DataCaptureModuleServiceBean.uploadRequestPath);
        } catch (DataCaptureModuleException ex) {
            throw new RuntimeException("Problem making upload request to Data Capture Module:  " + DataCaptureModuleUtil.getMessageFromException(ex));
        }
        int statusCode = uploadRequestResponse.getHttpStatusCode();
        String response = uploadRequestResponse.getResponse();
        if (statusCode != 200) {
            // TODO: replace with CommandExecutionException?
            throw new RuntimeException("When making the upload request, rather than 200 the status code was " + statusCode + ". The body was \'" + response + "\'. We cannot proceed. Returning.");
        }
        long millisecondsToSleep = DataCaptureModuleServiceBean.millisecondsToSleepBetweenUploadRequestAndScriptRequestCalls;
        logger.fine("Message from Data Caputure Module upload request endpoint: " + response + ". Sleeping " + millisecondsToSleep + " milliseconds before making rsync script request.");
        try {
            Thread.sleep(millisecondsToSleep);
        } catch (InterruptedException ex) {
            throw new RuntimeException(errorPreamble + " Unable to wait " + millisecondsToSleep + " milliseconds: " + ex.getLocalizedMessage());
        }
        ScriptRequestResponse scriptRequestResponse = null;
        try {
            scriptRequestResponse = ctxt.dataCaptureModule().retreiveRequestedRsyncScript(dataset.getIdentifier(), dcmBaseUrl + DataCaptureModuleServiceBean.scriptRequestPath);
        } catch (DataCaptureModuleException ex) {
            throw new RuntimeException("Problem making script request to Data Capture Module:  " + DataCaptureModuleUtil.getMessageFromException(ex));
        }
        String script = scriptRequestResponse.getScript();
        if (script == null || script.isEmpty()) {
            logger.warning("There was a problem getting the script for " + dataset.getIdentifier() + " . DCM returned status code: "+scriptRequestResponse.getHttpStatusCode());
        }
        logger.fine("script for dataset " + dataset.getId() + ": " + script);
        return scriptRequestResponse;
    }

}
