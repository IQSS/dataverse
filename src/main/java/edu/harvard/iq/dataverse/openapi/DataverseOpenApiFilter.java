package edu.harvard.iq.dataverse.openapi;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.Paths;
import org.eclipse.microprofile.openapi.models.Components;
import org.eclipse.microprofile.openapi.models.headers.Header;
import org.eclipse.microprofile.openapi.models.media.Content;
import org.eclipse.microprofile.openapi.models.media.MediaType;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.responses.APIResponses;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;

/**
 * Post-processes the OpenAPI model produced by automatic annotation scanning.
 * <p>
 * Most Dataverse API documentation is generated from JAX-RS and MicroProfile
 * OpenAPI annotations in the resource classes. This filter handles the small set
 * of cases where the generated model needs source-aware normalization after that
 * scan has completed: adding global metadata that the generator misses, replacing
 * Java implementation types with their actual wire payloads, removing known
 * circular schema links, and pruning component schemas that are no longer
 * reachable from public operations.
 * <p>
 * The goal is to keep endpoint documentation close to the source code while
 * preventing generated OpenAPI from exposing server-side helper objects or other
 * artifacts of the Java implementation as public REST contracts.
 */
public class DataverseOpenApiFilter implements OASFilter {

    /*
     * These operations produce binary downloads, but their Java return types are
     * server-side writer inputs such as BundleDownloadInstance or DownloadInstance.
     * @Produces documents the Content-Type, but SmallRye can still infer the schema
     * from the Java return type and expose those helper objects as public payloads.
     * The filter replaces their response schemas with string/binary schemas.
     */
    private static final String ACCESS_BUNDLE = "Access_datafileBundle";
    private static final String ACCESS_BUNDLE_WITH_GUESTBOOK = "Access_datafileBundleWithGuestbookResponse";
    private static final String ACCESS_AUXILIARY = "Access_downloadAuxiliaryFile";

    /**
     * Applies Dataverse-specific cleanup after SmallRye has generated the base
     * OpenAPI model.
     * <p>
     * The filter fills in stable security metadata, replaces inferred internal
     * writer/helper schemas with the actual wire payloads for selected operations,
     * removes a known circular schema back-reference, and drops component schemas
     * that are no longer referenced by any operation.
     *
     * @param openAPI generated OpenAPI model to normalize
     */
    @Override
    public void filterOpenAPI(OpenAPI openAPI) {
        ensureDataverseApiKeySecurityScheme(openAPI);
        forEachOperation(openAPI, operation -> {
            String operationId = operation.getOperationId();
            if (ACCESS_BUNDLE.equals(operationId) || ACCESS_BUNDLE_WITH_GUESTBOOK.equals(operationId)) {
                replaceOkResponse(operation, "application/zip",
                        "ZIP archive containing the data file bundle, citation files, and available metadata.");
            } else if (ACCESS_AUXILIARY.equals(operationId)) {
                replaceOkResponse(operation, "application/octet-stream",
                        "Auxiliary file bytes for the requested data file format.");
            }
        });
        removeCircularSchemaBackReferences(openAPI);
        pruneUnreachableSchemas(openAPI);
    }

    /**
     * Ensures operations that reference {@code DataverseApiKey} have a matching
     * global OpenAPI security scheme.
     * <p>
     * The Maven OpenAPI generation path does not reliably emit the annotation from
     * the JAX-RS {@code ApplicationPath} configuration class, so the filter adds
     * the API-token header scheme directly to {@code components.securitySchemes}.
     *
     * @param openAPI generated OpenAPI model to update
     */
    private void ensureDataverseApiKeySecurityScheme(OpenAPI openAPI) {
        Components components = openAPI.getComponents();
        if (components == null) {
            components = OASFactory.createComponents();
            openAPI.setComponents(components);
        }
        SecurityScheme scheme = OASFactory.createSecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name("X-Dataverse-key")
                .description("Dataverse API token.");
        components.addSecurityScheme("DataverseApiKey", scheme);
    }

    /**
     * Callback used when iterating over all generated operations.
     */
    private interface OperationConsumer {
        void accept(Operation operation);
    }

    /**
     * Visits every HTTP operation in the OpenAPI paths tree.
     *
     * @param openAPI generated OpenAPI model to scan
     * @param consumer callback invoked for each non-null operation
     */
    private void forEachOperation(OpenAPI openAPI, OperationConsumer consumer) {
        Paths paths = openAPI.getPaths();
        if (paths == null || paths.getPathItems() == null) {
            return;
        }
        for (PathItem pathItem : paths.getPathItems().values()) {
            if (pathItem == null || pathItem.getOperations() == null) {
                continue;
            }
            for (Operation operation : pathItem.getOperations().values()) {
                if (operation != null) {
                    consumer.accept(operation);
                }
            }
        }
    }

    /**
     * Replaces an operation's {@code 200} response with a documented binary
     * payload.
     * <p>
     * A method-level {@code @Produces("application/zip")} or
     * {@code @Produces("application/octet-stream")} annotation is not enough for
     * custom writer types. It sets the media type, but SmallRye may still infer a
     * component schema from the Java return type. For download endpoints that
     * return writer inputs, that would document server-side helper objects instead
     * of the bytes received by clients. This override keeps the media type and
     * replaces the schema with {@code type: string, format: binary}.
     *
     * @param operation operation whose successful response should be rewritten
     * @param mediaType response media type, such as {@code application/zip}
     * @param description public response description
     */
    private void replaceOkResponse(Operation operation, String mediaType, String description) {
        APIResponses responses = operation.getResponses();
        if (responses == null) {
            responses = OASFactory.createAPIResponses();
            operation.setResponses(responses);
        }
        APIResponse response = responses.getAPIResponse("200");
        if (response == null) {
            response = OASFactory.createAPIResponse();
            responses.addAPIResponse("200", response);
        }
        response.setDescription(description);

        Content content = OASFactory.createContent();
        content.addMediaType(mediaType, OASFactory.createMediaType().schema(binarySchema()));
        response.setContent(content);
    }

    /**
     * Creates the OpenAPI schema used for binary response content.
     *
     * @return string schema with {@code binary} format
     */
    private Schema binarySchema() {
        return OASFactory.createSchema()
                .type(Schema.SchemaType.STRING)
                .format("binary");
    }

    /**
     * Removes a generated schema property that creates a circular
     * {@code BuiltinUser -> PasswordResetData -> BuiltinUser} reference chain.
     *
     * @param openAPI generated OpenAPI model to update
     */
    private void removeCircularSchemaBackReferences(OpenAPI openAPI) {
        Components components = openAPI.getComponents();
        if (components == null || components.getSchemas() == null) {
            return;
        }
        Schema passwordResetData = components.getSchemas().get("PasswordResetData");
        if (passwordResetData != null && passwordResetData.getProperties() != null) {
            Map<String, Schema> properties = new LinkedHashMap<>(passwordResetData.getProperties());
            properties.remove("builtinUser");
            passwordResetData.setProperties(properties);
        }
    }

    /**
     * Removes component schemas that are not reachable from paths, parameters,
     * request bodies, responses, or their nested schema references.
     * <p>
     * This keeps explicitly replaced internal schemas out of the final generated
     * OpenAPI document.
     * <p>
     * For example, bundle and auxiliary download endpoints can cause SmallRye to
     * infer schemas for internal writer inputs such as {@code BundleDownloadInstance}
     * and {@code DownloadInstance}. Those schemas can then pull in service and
     * command objects such as {@code EjbDataverseEngine}, {@code CommandContext},
     * {@code DataverseRequestServiceBean}, and {@code SolrIndexServiceBean}. After
     * the endpoint response is replaced with a binary {@code application/zip} or
     * {@code application/octet-stream} payload, these implementation schemas are no
     * longer reachable from any public operation and are removed here.
     *
     * @param openAPI generated OpenAPI model to prune
     */
    private void pruneUnreachableSchemas(OpenAPI openAPI) {
        Components components = openAPI.getComponents();
        if (components == null || components.getSchemas() == null || components.getSchemas().isEmpty()) {
            return;
        }

        Map<String, Schema> schemas = components.getSchemas();
        Set<String> reachable = new HashSet<>();
        collectPathSchemaReferences(openAPI, reachable);

        ArrayDeque<String> queue = new ArrayDeque<>(reachable);
        while (!queue.isEmpty()) {
            String schemaName = queue.removeFirst();
            Schema schema = schemas.get(schemaName);
            if (schema == null) {
                continue;
            }
            Set<String> nested = new HashSet<>();
            collectSchemaReferences(schema, nested);
            for (String nestedName : nested) {
                if (schemas.containsKey(nestedName) && reachable.add(nestedName)) {
                    queue.addLast(nestedName);
                }
            }
        }

        Map<String, Schema> filteredSchemas = new LinkedHashMap<>();
        for (Map.Entry<String, Schema> entry : schemas.entrySet()) {
            if (reachable.contains(entry.getKey())) {
                filteredSchemas.put(entry.getKey(), entry.getValue());
            }
        }
        components.setSchemas(filteredSchemas);
    }

    /**
     * Collects component schema references directly used by path-level and
     * operation-level parameters, request bodies, responses, and response headers.
     *
     * @param openAPI generated OpenAPI model to scan
     * @param refs component schema names referenced from paths
     */
    private void collectPathSchemaReferences(OpenAPI openAPI, Set<String> refs) {
        Paths paths = openAPI.getPaths();
        if (paths == null || paths.getPathItems() == null) {
            return;
        }
        for (PathItem pathItem : paths.getPathItems().values()) {
            if (pathItem == null) {
                continue;
            }
            collectParameters(pathItem.getParameters(), refs);
            if (pathItem.getOperations() == null) {
                continue;
            }
            for (Operation operation : pathItem.getOperations().values()) {
                if (operation == null) {
                    continue;
                }
                collectParameters(operation.getParameters(), refs);
                collectRequestBody(operation.getRequestBody(), refs);
                collectResponses(operation.getResponses(), refs);
            }
        }
    }

    /**
     * Collects schema references used by a list of OpenAPI parameters.
     *
     * @param parameters parameters to inspect
     * @param refs component schema names discovered while scanning
     */
    private void collectParameters(Iterable<Parameter> parameters, Set<String> refs) {
        if (parameters == null) {
            return;
        }
        for (Parameter parameter : parameters) {
            if (parameter != null) {
                collectSchemaReferences(parameter.getSchema(), refs);
            }
        }
    }

    /**
     * Collects schema references used by a request body.
     *
     * @param requestBody request body to inspect
     * @param refs component schema names discovered while scanning
     */
    private void collectRequestBody(RequestBody requestBody, Set<String> refs) {
        if (requestBody != null) {
            collectContent(requestBody.getContent(), refs);
        }
    }

    /**
     * Collects schema references used by operation responses and response headers.
     *
     * @param responses responses to inspect
     * @param refs component schema names discovered while scanning
     */
    private void collectResponses(APIResponses responses, Set<String> refs) {
        if (responses == null || responses.getAPIResponses() == null) {
            return;
        }
        for (APIResponse response : responses.getAPIResponses().values()) {
            if (response == null) {
                continue;
            }
            collectContent(response.getContent(), refs);
            if (response.getHeaders() != null) {
                for (Header header : response.getHeaders().values()) {
                    if (header != null) {
                        collectSchemaReferences(header.getSchema(), refs);
                    }
                }
            }
        }
    }

    /**
     * Collects schema references from all media types in an OpenAPI content map.
     *
     * @param content content map to inspect
     * @param refs component schema names discovered while scanning
     */
    private void collectContent(Content content, Set<String> refs) {
        if (content == null || content.getMediaTypes() == null) {
            return;
        }
        for (MediaType mediaType : content.getMediaTypes().values()) {
            if (mediaType != null) {
                collectSchemaReferences(mediaType.getSchema(), refs);
            }
        }
    }

    /**
     * Recursively collects component schema references from a schema.
     * <p>
     * The traversal follows array items, object properties, additional-property
     * schemas, composition keywords, and negated schemas.
     *
     * @param schema schema to inspect
     * @param refs component schema names discovered while scanning
     */
    private void collectSchemaReferences(Schema schema, Set<String> refs) {
        if (schema == null) {
            return;
        }
        String componentName = componentSchemaName(schema.getRef());
        if (componentName != null) {
            refs.add(componentName);
        }
        if (schema.getItems() != null) {
            collectSchemaReferences(schema.getItems(), refs);
        }
        if (schema.getProperties() != null) {
            for (Schema propertySchema : schema.getProperties().values()) {
                collectSchemaReferences(propertySchema, refs);
            }
        }
        if (schema.getAdditionalPropertiesSchema() != null) {
            collectSchemaReferences(schema.getAdditionalPropertiesSchema(), refs);
        }
        collectSchemaList(schema.getAllOf(), refs);
        collectSchemaList(schema.getAnyOf(), refs);
        collectSchemaList(schema.getOneOf(), refs);
        if (schema.getNot() != null) {
            collectSchemaReferences(schema.getNot(), refs);
        }
    }

    /**
     * Collects schema references from a list of schemas, such as {@code allOf},
     * {@code anyOf}, or {@code oneOf}.
     *
     * @param schemas schemas to inspect
     * @param refs component schema names discovered while scanning
     */
    private void collectSchemaList(Iterable<Schema> schemas, Set<String> refs) {
        if (schemas == null) {
            return;
        }
        for (Schema schema : schemas) {
            collectSchemaReferences(schema, refs);
        }
    }

    /**
     * Extracts the component schema name from a local schema reference.
     *
     * @param ref schema reference value
     * @return component schema name, or {@code null} when the reference is absent
     *         or does not point to {@code #/components/schemas/}
     */
    private String componentSchemaName(String ref) {
        String prefix = "#/components/schemas/";
        if (ref == null || !ref.startsWith(prefix)) {
            return null;
        }
        return ref.substring(prefix.length());
    }
}
