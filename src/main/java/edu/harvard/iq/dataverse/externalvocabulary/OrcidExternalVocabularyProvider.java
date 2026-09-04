package edu.harvard.iq.dataverse.externalvocabulary;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OrcidExternalVocabularyProvider implements ExternalVocabularyProvider {

    @Override
    public String getProtocol() {
        return "orcid";
    }

    @Override
    public List<ExternalVocabularyTerm> search(String query, JsonObject config, ExternalVocabularyContext context)
            throws ExternalVocabularyException {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        String baseUrl = getBaseUrl(config);
        String searchUrl = baseUrl.replace("https://", "https://pub.") + "v3.0/expanded-search";

        try (Client client = ClientBuilder.newClient()) {
            Invocation.Builder request = client
                    .target(searchUrl)
                    .queryParam("q", query)
                    .queryParam("rows", "10")
                    .request();

            try (Response response = ExternalVocabularyHttpHelper.applyConfiguredHeaders(request, config).get()) {
                ensureSuccessful(response, "search ORCID records");
                JsonObject payload = response.readEntity(JsonObject.class);
                JsonArray results = payload.getJsonArray("expanded-result");
                if (results == null) {
                    return List.of();
                }

                List<ExternalVocabularyTerm> terms = new ArrayList<>();
                for (JsonValue resultValue : results) {
                    if (resultValue.getValueType() != JsonValue.ValueType.OBJECT) {
                        continue;
                    }
                    JsonObject result = resultValue.asJsonObject();
                    String orcidId = result.getString("orcid-id", "");
                    String label = formatName(result.getString("family-names", null), result.getString("given-names", null));
                    terms.add(new ExternalVocabularyTerm(baseUrl + orcidId, label, "ORCID", baseUrl, getProtocol()));
                }
                return terms;
            }
        } catch (ExternalVocabularyException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalVocabularyException("Failed to search ORCID records.", e);
        }
    }

    @Override
    public Optional<ExternalVocabularyTerm> resolve(String uri, JsonObject config, ExternalVocabularyContext context)
            throws ExternalVocabularyException {
        if (uri == null || uri.trim().isEmpty()) {
            return Optional.empty();
        }

        String baseUrl = getBaseUrl(config);
        String orcidId = uri.startsWith(baseUrl) ? uri.substring(baseUrl.length()) : uri;
        String resolveUrl = baseUrl.replace("https://", "https://pub.") + "v3.0/" + orcidId + "/person";

        try (Client client = ClientBuilder.newClient()) {
            Invocation.Builder request = client.target(resolveUrl).request();
            try (Response response = ExternalVocabularyHttpHelper.applyConfiguredHeaders(request, config).get()) {
                ensureSuccessful(response, "resolve ORCID record");
                JsonObject payload = response.readEntity(JsonObject.class);
                JsonObject name = payload.getJsonObject("name");
                String given = nestedValue(name, "given-names");
                String family = nestedValue(name, "family-name");
                return Optional.of(new ExternalVocabularyTerm(baseUrl + orcidId, formatName(family, given), "ORCID", baseUrl, getProtocol()));
            }
        } catch (ExternalVocabularyException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalVocabularyException("Failed to resolve ORCID record.", e);
        }
    }

    private static String getBaseUrl(JsonObject config) {
        return ExternalVocabularyHttpHelper.withTrailingSlash(config.getString("cvoc-url", "https://orcid.org/"));
    }

    private static String nestedValue(JsonObject parent, String key) {
        if (parent == null || !parent.containsKey(key) || parent.get(key).getValueType() != JsonValue.ValueType.OBJECT) {
            return null;
        }
        return parent.getJsonObject(key).getString("value", null);
    }

    private static String formatName(String family, String given) {
        if (family != null && !family.isBlank() && given != null && !given.isBlank()) {
            return family + ", " + given;
        }
        if (given != null && !given.isBlank()) {
            return given;
        }
        if (family != null && !family.isBlank()) {
            return family;
        }
        return "";
    }

    private static void ensureSuccessful(Response response, String action) throws ExternalVocabularyException {
        if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
            throw new ExternalVocabularyException("Failed to " + action + ". External service returned HTTP " + response.getStatus() + ".");
        }
    }
}
