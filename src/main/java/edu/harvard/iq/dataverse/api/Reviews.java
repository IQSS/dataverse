package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.api.auth.AuthRequired;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.dataset.DatasetTypeServiceBean;
import edu.harvard.iq.dataverse.dataverse.DataverseUtil;
import edu.harvard.iq.dataverse.engine.command.impl.CreateNewDatasetCommand;
import edu.harvard.iq.dataverse.review.Review;
import edu.harvard.iq.dataverse.review.ReviewServiceBean;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.ConstraintViolationUtil;
import edu.harvard.iq.dataverse.util.StringUtil;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import jakarta.ejb.EJB;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.stream.JsonParsingException;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("reviews")
public class Reviews extends AbstractApiBean {

    private static final Logger logger = Logger.getLogger(Reviews.class.getCanonicalName());

    @EJB
    ReviewServiceBean reviewService;

    @EJB
    DatasetTypeServiceBean datasetTypeService;

    @EJB
    SettingsServiceBean settingsService;

    @GET
    public Response listAll() {
        List<Review> reviews = reviewService.findAll();
        JsonArrayBuilder jab = Json.createArrayBuilder();
        for (Review review : reviews) {
            jab.add(ReviewServiceBean.toJson(review));
        }
        return ok(jab);
    }

    @GET
    @Path("{id}")
    public Response find(@PathParam("id") long idSupplied) {
        Review review = reviewService.find(idSupplied);
        return ok(ReviewServiceBean.toJson(review));
    }

    // TODO consider moving this to /api/dataverses (like createDataset, which this
    // is a based on)
    @POST
    @AuthRequired
    public Response createReview(@Context ContainerRequestContext crc,
            String jsonBody,
            @QueryParam("parentCollection") String parentCollection,
            @QueryParam("doNotValidate") String doNotValidateParam) {
        try {
            logger.fine("Json is: " + jsonBody);
            User u = getRequestUser(crc);
            Dataverse owner = findDataverseOrDie(parentCollection);
            Dataset ds = parseDataset(jsonBody);
            ds.setOwner(owner);
            // Will make validation happen always except for the (rare) occasion of all
            // three conditions are true
            boolean validate = !(u.isAuthenticated() && StringUtil.isTrue(doNotValidateParam) &&
                    JvmSettings.API_ALLOW_INCOMPLETE_METADATA.lookupOptional(Boolean.class).orElse(false));

            if (ds.getVersions().isEmpty()) {
                return badRequest(
                        BundleUtil.getStringFromBundle("dataverses.api.create.dataset.error.mustIncludeVersion"));
            }

            if (!ds.getFiles().isEmpty() && !u.isSuperuser()) {
                return badRequest(BundleUtil.getStringFromBundle("dataverses.api.create.dataset.error.superuserFiles"));
            }

            // Throw BadRequestException if metadataLanguage isn't compatible with setting
            DataverseUtil.checkMetadataLangauge(ds, owner, settingsService.getBaseMetadataLanguageMap(null, true));

            // clean possible version metadata
            DatasetVersion version = ds.getVersions().get(0);

            if (!validate && (version.getDatasetAuthors().isEmpty() || version.getDatasetAuthors().stream()
                    .anyMatch(a -> a.getName() == null || a.getName().isEmpty()))) {
                return badRequest(
                        BundleUtil.getStringFromBundle("dataverses.api.create.dataset.error.mustIncludeAuthorName"));
            }

            version.setMinorVersionNumber(null);
            version.setVersionNumber(null);
            version.setVersionState(DatasetVersion.VersionState.DRAFT);
            version.getTermsOfUseAndAccess().setFileAccessRequest(true);
            version.getTermsOfUseAndAccess().setDatasetVersion(version);

            ds.setAuthority(null);
            ds.setIdentifier(null);
            ds.setProtocol(null);
            ds.setGlobalIdCreateTime(null);
            Dataset managedDs;
            try {
                // TODO: change notification to say "new review created"
                managedDs = execCommand(new CreateNewDatasetCommand(ds, createDataverseRequest(u), validate, true));
            } catch (WrappedResponse ww) {
                Throwable cause = ww.getCause();
                StringBuilder sb = new StringBuilder();
                if (cause == null) {
                    return ww.refineResponse("cause was null!");
                }
                while (cause.getCause() != null) {
                    cause = cause.getCause();
                    if (cause instanceof ConstraintViolationException) {
                        sb.append(ConstraintViolationUtil.getErrorStringForConstraintViolations(cause));
                    }
                }
                String error = sb.toString();
                if (!error.isEmpty()) {
                    logger.log(Level.INFO, error);
                    return ww.refineResponse(error);
                }
                return ww.getResponse();
            }

            return created("/datasets/" + managedDs.getId(),
                    Json.createObjectBuilder()
                            .add("id", managedDs.getId())
                            .add("persistentId", managedDs.getGlobalId().asString()));

        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }

    }

    private Dataset parseDataset(String datasetJson) throws WrappedResponse {
        try {
            return jsonParser().parseDataset(JsonUtil.getJsonObject(datasetJson));
        } catch (JsonParsingException | JsonParseException jpe) {
            String message = jpe.getLocalizedMessage();
            logger.log(Level.SEVERE, "Error parsing dataset JSON. message: {0}", message);
            logger.log(Level.SEVERE, "Error parsing dataset json. Json: {0}", datasetJson);
            throw new WrappedResponse(error(Status.BAD_REQUEST, "Error parsing Json: " + jpe.getMessage()));
        }
    }

}
