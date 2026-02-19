package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.BagItLocalPath;
import edu.harvard.iq.dataverse.util.bagit.BagGenerator;
import edu.harvard.iq.dataverse.util.json.JsonLDTerm;
import edu.harvard.iq.dataverse.workflow.step.Failure;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepResult;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.logging.Logger;

import jakarta.json.Json;
import jakarta.json.JsonObject;
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
    
    public static boolean supportsDelete() {
        return true;
    }
    @Override
    public boolean canDelete() {
        return supportsDelete();
    }

    @Override
    public WorkflowStepResult performArchiveSubmission(DatasetVersion dv, String dataciteXml, JsonObject ore, 
            Map<String, JsonLDTerm> terms, ApiToken token, Map<String, String> requestedSettings) {
        logger.fine("In LocalSubmitToArchive...");
        String localPath = requestedSettings.get(BagItLocalPath.toString());
        String zipName = null;
        
        // Set a failure status that will be updated if we succeed
        JsonObjectBuilder statusObject = Json.createObjectBuilder();
        statusObject.add(DatasetVersion.ARCHIVAL_STATUS, DatasetVersion.ARCHIVAL_STATUS_FAILURE);
        statusObject.add(DatasetVersion.ARCHIVAL_STATUS_MESSAGE, "Bag not transferred");
        
        try {
            Dataset dataset = dv.getDataset();

            String spaceName = getSpaceName(dataset);

            // Define file paths
            String dataciteFileName = localPath + "/" + getDataCiteFileName(spaceName, dv) + ".xml";
            zipName = localPath + "/" + getFileName(spaceName, dv) + ".zip";

            // Check for and delete existing files for this version
            logger.fine("Checking for existing files in archive...");

            File existingDatacite = new File(dataciteFileName);
            if (existingDatacite.exists()) {
                logger.fine("Found existing datacite.xml, deleting: " + dataciteFileName);
                if (existingDatacite.delete()) {
                    logger.fine("Deleted existing datacite.xml");
                } else {
                    logger.warning("Failed to delete existing datacite.xml: " + dataciteFileName);
                }
            }

            File existingBag = new File(zipName);
            if (existingBag.exists()) {
                logger.fine("Found existing bag file, deleting: " + zipName);
                if (existingBag.delete()) {
                    logger.fine("Deleted existing bag file");
                } else {
                    logger.warning("Failed to delete existing bag file: " + zipName);
                }
            }

            // Also check for and delete the .partial file if it exists
            File existingPartial = new File(zipName + ".partial");
            if (existingPartial.exists()) {
                logger.fine("Found existing partial bag file, deleting: " + zipName + ".partial");
                if (existingPartial.delete()) {
                    logger.fine("Deleted existing partial bag file");
                } else {
                    logger.warning("Failed to delete existing partial bag file: " + zipName + ".partial");
                }
            }

            // Write datacite.xml file
            FileUtils.writeStringToFile(new File(dataciteFileName), dataciteXml, StandardCharsets.UTF_8);
            logger.fine("Datacite XML written to: " + dataciteFileName);

            // Generate bag
            BagGenerator bagger = new BagGenerator(ore, dataciteXml, terms);
            bagger.setAuthenticationKey(token.getTokenString());

            boolean bagSuccess = bagger.generateBag(new FileOutputStream(zipName + ".partial"));

            if (!bagSuccess) {
                logger.severe("Bag generation failed for " + zipName);
                return new Failure("Local Submission Failure", "Bag generation failed");
            }

            File srcFile = new File(zipName + ".partial");
            File destFile = new File(zipName);

            if (srcFile.renameTo(destFile)) {
                logger.fine("Localhost Submission step: Content Transferred to " + zipName);
                statusObject.add(DatasetVersion.ARCHIVAL_STATUS, DatasetVersion.ARCHIVAL_STATUS_SUCCESS);
                statusObject.add(DatasetVersion.ARCHIVAL_STATUS_MESSAGE, "file://" + zipName);
            } else {
                logger.severe("Unable to move " + zipName + ".partial to " + zipName);
                return new Failure("Local Submission Failure", "Unable to rename partial file to final file");
            }
        } catch (Exception e) {
            logger.warning("Failed to archive " + zipName + " : " + e.getLocalizedMessage());
            e.printStackTrace();
            return new Failure("Local Submission Failure", e.getLocalizedMessage() + ": check log for details");
        } finally {
            dv.setArchivalCopyLocation(statusObject.build().toString());
        }
        
        return WorkflowStepResult.OK;
    }

}
