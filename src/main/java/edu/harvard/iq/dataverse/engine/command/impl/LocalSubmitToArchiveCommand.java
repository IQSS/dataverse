package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DOIDataCiteRegisterService;
import edu.harvard.iq.dataverse.DataCitation;
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
        try {

            Dataset dataset = dv.getDataset();

            if (dataset.getLockFor(Reason.finalizePublication) == null
                    && dataset.getLockFor(Reason.FileValidationFailed) == null) {

                String spaceName = dataset.getGlobalId().asString().replace(':', '-').replace('/', '-')
                        .replace('.', '-').toLowerCase();

                DataCitation dc = new DataCitation(dv);
                Map<String, String> metadata = dc.getDataCiteMetadata();
                String dataciteXml = DOIDataCiteRegisterService
                        .getMetadataFromDvObject(dv.getDataset().getGlobalId().asString(), metadata, dv.getDataset());

                FileUtils.writeStringToFile(
                        new File(localPath + "/" + spaceName + "-datacite.v" + dv.getFriendlyVersionNumber() + ".xml"),
                        dataciteXml, StandardCharsets.UTF_8);
                BagGenerator bagger = new BagGenerator(new OREMap(dv, false), dataciteXml);
                bagger.setAuthenticationKey(token.getTokenString());
                zipName = localPath + "/" + spaceName + "v" + dv.getFriendlyVersionNumber() + ".zip";
                bagger.generateBag(new FileOutputStream(zipName + ".partial"));

                File srcFile = new File(zipName + ".partial");
                File destFile = new File(zipName);

                if (srcFile.renameTo(destFile)) {
                    logger.fine("Localhost Submission step: Content Transferred");
                    dv.setArchivalCopyLocation("file://" + zipName);
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
        }
        return WorkflowStepResult.OK;
    }

}
