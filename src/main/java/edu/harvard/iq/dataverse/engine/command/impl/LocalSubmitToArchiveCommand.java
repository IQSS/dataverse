package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DatasetLock.Reason;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.util.bagit.BagGenerator;
import edu.harvard.iq.dataverse.util.bagit.OREMap;
import edu.harvard.iq.dataverse.workflow.step.Failure;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepResult;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.logging.Logger;

import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;

import java.io.File;
import java.io.FileOutputStream;

import org.apache.commons.io.FileUtils;

@RequiredPermissions(Permission.PublishDataset)
public class LocalSubmitToArchiveCommand extends AbstractSubmitToArchiveCommand implements Command<DatasetVersion> {

    private static final Logger logger = Logger.getLogger(LocalSubmitToArchiveCommand.class.getName());

    public LocalSubmitToArchiveCommand(DataverseRequest aRequest, DatasetVersion version) {
        super(aRequest, version);
    }

    @Override
    public WorkflowStepResult performArchiveSubmission(DatasetVersion dv, ApiToken token,
            Map<String, String> requestedSettings) {
        logger.fine("In LocalCloudSubmitToArchive...");
        String localPath = requestedSettings.get(":BagItLocalPath");
        String zipName = null;
        
        //Set a failure status that will be updated if we succeed
        JsonObjectBuilder statusObject = Json.createObjectBuilder();
        statusObject.add(DatasetVersion.ARCHIVAL_STATUS, DatasetVersion.ARCHIVAL_STATUS_FAILURE);
        statusObject.add(DatasetVersion.ARCHIVAL_STATUS_MESSAGE, "Bag not transferred");
        
        try {

            Dataset dataset = dv.getDataset();

            if (dataset.getLockFor(Reason.finalizePublication) == null
                    && dataset.getLockFor(Reason.FileValidationFailed) == null) {

                String spaceName = dataset.getGlobalId().asString().replace(':', '-').replace('/', '-')
                        .replace('.', '-').toLowerCase();

                String dataciteXml = getDataCiteXml(dv);
                
                FileUtils.writeStringToFile(
                        new File(localPath + "/" + spaceName + "-datacite.v" + dv.getFriendlyVersionNumber() + ".xml"),
                        dataciteXml, StandardCharsets.UTF_8);
                BagGenerator bagger = new BagGenerator(new OREMap(dv, false), dataciteXml);
                bagger.setNumConnections(getNumberOfBagGeneratorThreads());
                bagger.setAuthenticationKey(token.getTokenString());
                zipName = localPath + "/" + spaceName + "v" + dv.getFriendlyVersionNumber() + ".zip";
                //ToDo: generateBag(File f, true) seems to do the same thing (with a .tmp extension) - since we don't have to use a stream here, could probably just reuse the existing code? 
                bagger.generateBag(new FileOutputStream(zipName + ".partial"));

                File srcFile = new File(zipName + ".partial");
                File destFile = new File(zipName);

                if (srcFile.renameTo(destFile)) {
                    logger.fine("Localhost Submission step: Content Transferred");
                    statusObject.add(DatasetVersion.ARCHIVAL_STATUS, DatasetVersion.ARCHIVAL_STATUS_SUCCESS);
                    statusObject.add(DatasetVersion.ARCHIVAL_STATUS_MESSAGE, "file://" + zipName);
                } else {
                    logger.warning("Unable to move " + zipName + ".partial to " + zipName);
                }
            } else {
                logger.warning(
                        "Localhost Submision Workflow aborted: Dataset locked for finalizePublication, or because file validation failed");
                return new Failure("Dataset locked");
            }
        } catch (Exception e) {
            logger.warning("Failed to archive " + zipName + " : " + e.getLocalizedMessage());
            e.printStackTrace();
        } finally {
            dv.setArchivalCopyLocation(statusObject.build().toString());
        }
        
        return WorkflowStepResult.OK;
    }

}
