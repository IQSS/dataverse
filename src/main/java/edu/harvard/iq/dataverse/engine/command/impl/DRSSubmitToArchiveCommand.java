package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.SettingsWrapper;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import edu.harvard.iq.dataverse.workflow.step.Failure;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepResult;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

@RequiredPermissions(Permission.PublishDataset)
public class DRSSubmitToArchiveCommand extends S3SubmitToArchiveCommand implements Command<DatasetVersion> {

    private static final Logger logger = Logger.getLogger(DRSSubmitToArchiveCommand.class.getName());
    private static final String DRS_CONFIG = ":DRSArchivalConfig";

    public DRSSubmitToArchiveCommand(DataverseRequest aRequest, DatasetVersion version) {
        super(aRequest, version);
    }

    @Override
    public WorkflowStepResult performArchiveSubmission(DatasetVersion dv, ApiToken token,
            Map<String, String> requestedSettings) {
        logger.fine("In DRSSubmitToArchiveCommand...");
        JsonObject drsConfigObject = null;

        try {
            drsConfigObject = JsonUtil.getJsonObject(requestedSettings.get(DRS_CONFIG));
        } catch (Exception e) {
            logger.warning("Unable to parse " + DRS_CONFIG + " setting as a Json object");
        }
        if (drsConfigObject != null) {
            Set<String> collections = drsConfigObject.getJsonObject("collections").keySet();
            Dataset dataset = dv.getDataset();
            Dataverse ancestor = dataset.getOwner();
            String alias = getArchivableAncestor(ancestor, collections);
           
            if (alias != null) {
                JsonObject collectionConfig = drsConfigObject.getJsonObject("collections").getJsonObject(alias);

                WorkflowStepResult s3Result = super.performArchiveSubmission(dv, token, requestedSettings);

                if (s3Result == WorkflowStepResult.OK) {
                    // Now contact DRS
                    JsonObjectBuilder job = Json.createObjectBuilder(drsConfigObject);
                    job.remove("collections");
                    for (Entry<String, JsonValue> entry : collectionConfig.entrySet()) {
                        job.add(entry.getKey(), entry.getValue());
                    }

                    String drsConfigString = JsonUtil.prettyPrint(job.build());
                    try (ByteArrayInputStream configIn = new ByteArrayInputStream(drsConfigString.getBytes("UTF-8"))) {
                        // Add datacite.xml file
                        ObjectMetadata om = new ObjectMetadata();
                        om.setContentLength(configIn.available());
                        String dcKey = getSpaceName(dataset) + "/drsConfig." + getSpaceName(dataset) + "_v"
                                + dv.getFriendlyVersionNumber() + ".json";
                        tm.upload(new PutObjectRequest(bucketName, dcKey, configIn, om)).waitForCompletion();
                        om = s3.getObjectMetadata(bucketName, dcKey);
                    } catch (RuntimeException rte) {
                        logger.warning("Error creating DRS Config file during DRS archiving: " + rte.getMessage());
                        rte.printStackTrace();
                        return new Failure("Error in generating Config file",
                                "DRS Submission Failure: config file not created");
                    } catch (InterruptedException e) {
                        logger.warning("DRS Archiver failure: " + e.getLocalizedMessage());
                        e.printStackTrace();
                        return new Failure("DRS Archiver fail in config transfer");
                    } catch (UnsupportedEncodingException e1) {
                        logger.warning("UTF-8 not supported!");
                    } catch (IOException e1) {
                        logger.warning("Failure creating ByteArrayInputStream from string!");
                    }

                    logger.fine("DRS Submission step: Config Transferred");

                    // Document the location of dataset archival copy location (actually the URL
                    // where you can
                    // view it as an admin)

                    // Unsigned URL - gives location but not access without creds
                } else {

                    logger.warning("DRS: S3 archiving failed - will not send config: " + getSpaceName(dataset) + "_v"
                            + dv.getFriendlyVersionNumber());
                    return new Failure("DRS Archiver fail in initial S3 Archiver transfer");
                }

            } else {
                logger.fine("DRS Archiver: No matching collection found - will not archive: " + getSpaceName(dataset)
                        + "_v" + dv.getFriendlyVersionNumber());
                return WorkflowStepResult.OK;
            }

        } else {
            logger.warning(DRS_CONFIG + " not found");
            return new Failure("DRS Submission not configured - no " + DRS_CONFIG + " found.");
        }
        return WorkflowStepResult.OK;
    }
    
    private static String getArchivableAncestor(Dataverse ancestor, Set<String> collections) {
        String alias = ancestor.getAlias();
        while (ancestor != null && !collections.contains(alias)) {
            ancestor = ancestor.getOwner();
            if (ancestor != null) {
                alias = ancestor.getAlias();
            } else {
                alias = null;
            }
        }
        return alias;
    }

    public static boolean isArchivable(Dataset d, SettingsWrapper sw) {
        JsonObject drsConfigObject = null;

        try {
            String config = sw.get(DRS_CONFIG, null);
            if(config!=null) {
            drsConfigObject = JsonUtil.getJsonObject(config);
            }
        } catch (Exception e) {
            logger.warning("Unable to parse " + DRS_CONFIG + " setting as a Json object");
        }
        if (drsConfigObject != null) {
            Set<String> collections = drsConfigObject.getJsonObject("collections").keySet();
            return getArchivableAncestor(d.getOwner(),collections)!=null;
        }
        return false;
    }
}
