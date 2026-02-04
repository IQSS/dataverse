package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersionUser;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.GuestbookResponse;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroup;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.search.savedsearch.SavedSearch;
import edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder;

import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;

// Superuser-only enforced below.
@RequiredPermissions({})
public class GetUserTracesCommand extends AbstractCommand<JsonObjectBuilder> {

    private static final Logger logger = Logger.getLogger(GetUserTracesCommand.class.getCanonicalName());
    
    private DataverseRequest request;
    private AuthenticatedUser user;
    private String element;

    public GetUserTracesCommand(DataverseRequest request, AuthenticatedUser user, String element) {
        super(request, (DvObject) null);
        this.request = request;
        this.user = user;
        this.element = element;
    }

    @Override
    public JsonObjectBuilder execute(CommandContext ctxt) throws CommandException {
        if (!(getUser() instanceof AuthenticatedUser) || !getUser().isSuperuser()) {
            throw new PermissionException("Get user traces command can only be called by superusers.", this, null, null);
        }
        if (user == null) {
            throw new CommandException("Cannot get traces. User not found.", this);
        }
        Long userId = user.getId();
        JsonObjectBuilder traces = Json.createObjectBuilder();
        if (element == null || element.equals("roleAssignments")) {
            // List<Long> roleAssignments =
            // ctxt.permissions().getDvObjectsUserHasRoleOn(user);
            List<RoleAssignment> roleAssignments = ctxt.roleAssignees().getAssignmentsFor(user.getIdentifier());
            if (roleAssignments != null && !roleAssignments.isEmpty()) {
                JsonObjectBuilder job = Json.createObjectBuilder();
                JsonArrayBuilder jab = Json.createArrayBuilder();
                for (RoleAssignment roleAssignment : roleAssignments) {
                    jab.add(NullSafeJsonBuilder.jsonObjectBuilder()
                            .add("id", roleAssignment.getId())
                            .add("definitionPointName", roleAssignment.getDefinitionPoint().getCurrentName())
                            .add("definitionPointIdentifier", roleAssignment.getDefinitionPoint().getIdentifier())
                            .add("definitionPointId", roleAssignment.getDefinitionPoint().getId())
                            .add("roleAlias", roleAssignment.getRole().getAlias())
                            .add("roleName", roleAssignment.getRole().getName()));
                }
                job.add("count", roleAssignments.size());
                job.add("items", jab);
                traces.add("roleAssignments", job);
            }
        }
        if (element == null || element.equals("dataverseCreator")) {
            List<Dataverse> dataversesCreated = ctxt.dataverses().findByCreatorId(userId);
            if (dataversesCreated != null && !dataversesCreated.isEmpty()) {
                JsonObjectBuilder job = Json.createObjectBuilder();
                JsonArrayBuilder jab = Json.createArrayBuilder();
                for (Dataverse dataverse : dataversesCreated) {
                    jab.add(Json.createObjectBuilder()
                            .add("id", dataverse.getId())
                            .add("alias", dataverse.getAlias()));
                }
                job.add("count", dataversesCreated.size());
                job.add("items", jab);
                traces.add("dataverseCreator", job);
            }
        }
        if (element == null || element.equals("dataversePublisher")) {
            List<Dataverse> dataversesPublished = ctxt.dataverses().findByReleaseUserId(userId);
            if (dataversesPublished != null && !dataversesPublished.isEmpty()) {
                JsonObjectBuilder job = Json.createObjectBuilder();
                JsonArrayBuilder jab = Json.createArrayBuilder();
                for (Dataverse dataverse : dataversesPublished) {
                    jab.add(Json.createObjectBuilder()
                            .add("id", dataverse.getId())
                            .add("alias", dataverse.getAlias()));
                }
                job.add("count", dataversesPublished.size());
                job.add("items", jab);
                traces.add("dataversePublisher", job);
            }
        }
        if (element == null || element.equals("datasetCreator")) {
            List<Dataset> datasetsCreated = ctxt.datasets().findByCreatorId(userId);
            if (datasetsCreated != null && !datasetsCreated.isEmpty()) {
                JsonObjectBuilder job = Json.createObjectBuilder();
                JsonArrayBuilder jab = Json.createArrayBuilder();
                for (Dataset dataset : datasetsCreated) {
                    jab.add(Json.createObjectBuilder()
                            .add("id", dataset.getId())
                            .add("pid", dataset.getGlobalId().asString()));
                }
                job.add("count", datasetsCreated.size());
                job.add("items", jab);
                traces.add("datasetCreator", job);
            }
        }
        if (element == null || element.equals("datasetPublisher")) {
            List<Dataset> datasetsPublished = ctxt.datasets().findByReleaseUserId(userId);
            if (datasetsPublished != null && !datasetsPublished.isEmpty()) {
                JsonObjectBuilder job = Json.createObjectBuilder();
                JsonArrayBuilder jab = Json.createArrayBuilder();
                for (Dataset dataset : datasetsPublished) {
                    jab.add(Json.createObjectBuilder()
                            .add("id", dataset.getId())
                            .add("pid", dataset.getGlobalId().asString()));
                }
                job.add("count", datasetsPublished.size());
                job.add("items", jab);
                traces.add("datasetPublisher", job);
            }
        }
        if (element == null || element.equals("dataFileCreator")) {
            List<DataFile> dataFilesCreated = ctxt.files().findByCreatorId(userId);
            if (dataFilesCreated != null && !dataFilesCreated.isEmpty()) {
                JsonObjectBuilder job = Json.createObjectBuilder();
                JsonArrayBuilder jab = Json.createArrayBuilder();
                for (DataFile dataFile : dataFilesCreated) {
                    jab.add(Json.createObjectBuilder()
                            .add("id", dataFile.getId())
                            .add("filename", dataFile.getCurrentName())
                            .add("datasetPid", dataFile.getOwner().getGlobalId().asString()));
                }
                job.add("count", dataFilesCreated.size());
                job.add("items", jab);
                traces.add("dataFileCreator", job);
            }
        }
        if (element == null || element.equals("dataFilePublisher")) {
            // TODO: Consider removing this because we don't seem to populate releaseuser_id
            // for files.
            List<DataFile> dataFilesPublished = ctxt.files().findByReleaseUserId(userId);
            if (dataFilesPublished != null && !dataFilesPublished.isEmpty()) {
                JsonObjectBuilder job = Json.createObjectBuilder();
                JsonArrayBuilder jab = Json.createArrayBuilder();
                for (DataFile dataFile : dataFilesPublished) {
                    jab.add(Json.createObjectBuilder()
                            .add("id", dataFile.getId())
                            .add("filename", dataFile.getCurrentName())
                            .add("datasetPid", dataFile.getOwner().getGlobalId().asString()));
                }
                job.add("count", dataFilesPublished.size());
                job.add("items", jab);
                traces.add("dataFilePublisher", job);
            }
        }
        if (element == null || element.equals("datasetVersionUsers")) {
            // These are the users who have published a version (or created a draft).
            List<DatasetVersionUser> datasetVersionUsers = ctxt.datasetVersion().getDatasetVersionUsersByAuthenticatedUser(user);
            if (datasetVersionUsers != null && !datasetVersionUsers.isEmpty()) {
                JsonObjectBuilder job = Json.createObjectBuilder();
                JsonArrayBuilder jab = Json.createArrayBuilder();
                for (DatasetVersionUser datasetVersionUser : datasetVersionUsers) {
                    jab.add(Json.createObjectBuilder()
                            .add("id", datasetVersionUser.getId())
                            .add("dataset", datasetVersionUser.getDatasetVersion().getDataset().getGlobalId().asString())
                            .add("version", datasetVersionUser.getDatasetVersion().getSemanticVersion()));
                }
                job.add("count", datasetVersionUsers.size());
                job.add("items", jab);
                traces.add("datasetVersionUsers", job);
            }
        }
        if (element == null || element.equals("explicitGroups")) {
            Set<ExplicitGroup> explicitGroups = ctxt.explicitGroups().findDirectlyContainingGroups(user);
            if (explicitGroups != null && !explicitGroups.isEmpty()) {
                JsonObjectBuilder job = Json.createObjectBuilder();
                JsonArrayBuilder jab = Json.createArrayBuilder();
                for (ExplicitGroup explicitGroup : explicitGroups) {
                    jab.add(Json.createObjectBuilder()
                            .add("id", explicitGroup.getId())
                            .add("name", explicitGroup.getDisplayName()));
                }
                job.add("count", explicitGroups.size());
                job.add("items", jab);
                traces.add("explicitGroups", job);
            }
        }
        if (element == null || element.equals("guestbookEntries")) {
            List<GuestbookResponse> guestbookResponses = ctxt.responses().findByAuthenticatedUserId(user);
            if (guestbookResponses != null && !guestbookResponses.isEmpty()) {
                JsonObjectBuilder job = Json.createObjectBuilder();
                // The feeling is that this is too much detail for the call for all elements so
                // we only show a count in that case.
                if (element != null) {
                    JsonArrayBuilder jab = Json.createArrayBuilder();
                    for (GuestbookResponse guestbookResponse : guestbookResponses) {
                        try {
                            JsonObjectBuilder gbe = Json.createObjectBuilder()
                                    .add("id", guestbookResponse.getId())
                                    .add("eventType", guestbookResponse.getEventType())
                                    .add("filename", guestbookResponse.getDataFile().getCurrentName())
                                    .add("date", guestbookResponse.getResponseDate())
                                    .add("guestbookName", guestbookResponse.getGuestbook().getName());
                            if(guestbookResponse.getDataset().getGlobalId()!=null) {
                                gbe.add("dataset", guestbookResponse.getDataset().getGlobalId().asString());
                            }
                            if (guestbookResponse.getDatasetVersion() != null) {
                                gbe.add("version", guestbookResponse.getDatasetVersion().getSemanticVersion());
                            }
                            jab.add(gbe);
                        } catch (NullPointerException npe) {
                            //Legacy/bad db entries
                            logger.warning("Guestbook id:" + guestbookResponse.getId() + " does not have required info.");
                        }
                    }
                    job.add("items", jab);
                }
                job.add("count", guestbookResponses.size());
                // job.add("items", jab);
                traces.add("guestbookEntries", job);
            }
        }
        if (element == null || element.equals("savedSearches")) {
            List<SavedSearch> savedSearchs = ctxt.savedSearches().findByAuthenticatedUser(user);
            if (savedSearchs != null && !savedSearchs.isEmpty()) {
                JsonObjectBuilder job = Json.createObjectBuilder();
                JsonArrayBuilder jab = Json.createArrayBuilder();
                for (SavedSearch savedSearch : savedSearchs) {
                    jab.add(Json.createObjectBuilder()
                            .add("id", savedSearch.getId()));
                }
                job.add("count", savedSearchs.size());
                job.add("items", jab);
                traces.add("savedSearches", job);
            }
        }
        JsonObjectBuilder result = Json.createObjectBuilder();
        result.add("user", Json.createObjectBuilder()
                .add("identifier", user.getIdentifier())
                .add("name", user.getName()));
        result.add("traces", traces);
        return result;
    }

}
