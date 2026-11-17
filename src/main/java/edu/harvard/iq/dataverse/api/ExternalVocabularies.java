package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.externalvocabulary.ExternalVocabularyException;
import edu.harvard.iq.dataverse.externalvocabulary.ExternalVocabularyServiceBean;
import edu.harvard.iq.dataverse.externalvocabulary.ExternalVocabularyTerm;
import jakarta.ejb.EJB;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("external-vocabularies")
@Produces("application/json")
@Tag(name = "External Vocabularies", description = "External controlled vocabulary configuration and term lookup operations.")
public class ExternalVocabularies extends AbstractApiBean {

    @EJB
    ExternalVocabularyServiceBean externalVocabularyService;

    @GET
    @Operation(summary = "Lists configured external vocabularies",
            description = "Returns sanitized external vocabulary configuration derived from the CVocConf setting.")
    public Response listConfiguredExternalVocabularies() {
        return ok(externalVocabularyService.getConfiguredExternalVocabularies());
    }

    @GET
    @Path("{fieldName}")
    @Operation(summary = "Returns external vocabulary configuration for a field",
            description = "Returns sanitized external vocabulary configuration for the parent field or term URI field.")
    public Response getExternalVocabularyForField(
            @Parameter(description = "Dataset field type name.", required = true)
            @PathParam("fieldName") String fieldName) {
        Optional<JsonObject> config = externalVocabularyService.findConfigByFieldName(fieldName);
        return config
                .map(jsonObject -> ok(externalVocabularyService.toSanitizedConfig(jsonObject)))
                .orElseGet(() -> notFound("No external vocabulary is configured for field " + fieldName + "."));
    }

    @GET
    @Path("{fieldName}/search")
    @Operation(summary = "Searches external vocabulary terms",
            description = "Uses the provider configured for the field to search a third-party controlled vocabulary service.")
    public Response searchExternalVocabulary(
            @PathParam("fieldName") String fieldName,
            @QueryParam("q") String query,
            @QueryParam("vocab") String vocabulary,
            @QueryParam("lang") String language) {
        try {
            List<ExternalVocabularyTerm> terms = externalVocabularyService.search(fieldName, query, vocabulary, language);
            JsonArrayBuilder results = Json.createArrayBuilder();
            for (ExternalVocabularyTerm term : terms) {
                results.add(term.toJsonObjectBuilder());
            }
            return ok(results);
        } catch (ExternalVocabularyException e) {
            return badRequest(e.getMessage());
        }
    }

    @GET
    @Path("{fieldName}/resolve")
    @Operation(summary = "Resolves an external vocabulary term",
            description = "Uses the provider configured for the field to resolve a stored URI into display metadata.")
    public Response resolveExternalVocabulary(
            @PathParam("fieldName") String fieldName,
            @QueryParam("uri") String uri,
            @QueryParam("lang") String language) {
        try {
            Optional<ExternalVocabularyTerm> term = externalVocabularyService.resolve(fieldName, uri, language);
            return term
                    .map(externalVocabularyTerm -> ok(externalVocabularyTerm.toJsonObjectBuilder()))
                    .orElseGet(() -> notFound("External vocabulary term could not be resolved."));
        } catch (ExternalVocabularyException e) {
            return badRequest(e.getMessage());
        }
    }

    @GET
    @Path("{fieldName}/validate")
    @Operation(summary = "Validates an external vocabulary value",
            description = "Validates a value against the configured URI spaces and free-text policy for a field.")
    public Response validateExternalVocabulary(
            @PathParam("fieldName") String fieldName,
            @QueryParam("value") String value) {
        try {
            return ok(Json.createObjectBuilder().add("valid", externalVocabularyService.validate(fieldName, value)));
        } catch (ExternalVocabularyException e) {
            return badRequest(e.getMessage());
        }
    }
}
