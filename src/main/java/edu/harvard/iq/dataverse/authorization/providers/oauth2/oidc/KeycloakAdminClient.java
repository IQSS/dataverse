package edu.harvard.iq.dataverse.authorization.providers.oauth2.oidc;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.harvard.iq.dataverse.settings.JvmSettings;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.Singleton;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;

import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Minimal client for the Keycloak Admin REST API, used to read the data that the
 * OIDC token cannot carry -- most notably group attributes, which Keycloak does not
 * map into claims.
 * <p>
 * Authenticates as a service account via the {@code client_credentials} grant. The
 * service account client needs the {@code view-users} and {@code query-groups} realm
 * roles (realm-management client roles) and nothing else.
 * <p>
 * Group attributes are cached: the tenant -> collection binding changes very rarely,
 * and we do not want a Keycloak round trip on every single login.
 */
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class KeycloakAdminClient {

    private static final Logger logger = Logger.getLogger(KeycloakAdminClient.class.getName());

    /**
     * Refresh the service account token this long before it actually expires, so we
     * never present a token that dies mid-flight.
     */
    private static final Duration TOKEN_EXPIRY_MARGIN = Duration.ofSeconds(30);

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private static final int MEMBER_PAGE_SIZE = 100;

    /** Backstop against paging forever if a group is unexpectedly huge. */
    private static final int MAX_GROUP_MEMBERS = 100_000;

    private static final int MAX_SUBGROUPS = 5_000;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /** Group path -> its attributes. Never caches failures. */
    private Cache<String, JsonObject> groupCache;

    private final Object tokenLock = new Object();
    private String accessToken;
    private Instant accessTokenExpiry = Instant.EPOCH;

    @PostConstruct
    void setup() {
        long maxAge = JvmSettings.OIDC_SYNC_CACHE_MAXAGE.lookupOptional(Integer.class).orElse(300);
        groupCache = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofSeconds(maxAge))
                .build();
    }

    /**
     * Fetch a group by its full path, including its attributes.
     *
     * @param groupPath full path, with or without the leading slash, e.g. {@code /platica/tenant-users/tenant-1}
     * @return the group representation, or empty when the group does not exist or Keycloak is unreachable
     */
    public Optional<JsonObject> getGroupByPath(String groupPath) {
        if (groupPath == null || groupPath.isBlank()) {
            return Optional.empty();
        }
        String normalised = normalisePath(groupPath);
        JsonObject cached = groupCache.getIfPresent(normalised);
        if (cached != null) {
            return Optional.of(cached);
        }
        // Each path segment must be encoded individually: the slashes are part of the URL.
        String encoded = Arrays.stream(normalised.split("/"))
                .map(segment -> URLEncoder.encode(segment, StandardCharsets.UTF_8))
                .collect(Collectors.joining("/"));

        Optional<JsonObject> group = get("/admin/realms/" + realm() + "/group-by-path/" + encoded)
                .filter(v -> v.getValueType() == JsonValue.ValueType.OBJECT)
                .map(JsonValue::asJsonObject);
        group.ifPresent(g -> groupCache.put(normalised, g));
        return group;
    }

    /**
     * Read a single-valued group attribute. Keycloak stores attributes as arrays, so we
     * take the first entry.
     *
     * @return the attribute value, or empty when the group or the attribute does not exist
     */
    public Optional<String> getGroupAttribute(String groupPath, String attributeName) {
        return getGroupByPath(groupPath)
                .map(group -> group.getJsonObject("attributes"))
                .map(attributes -> attributes.get(attributeName))
                .filter(v -> v.getValueType() == JsonValue.ValueType.ARRAY)
                .map(JsonValue::asJsonArray)
                .filter(values -> !values.isEmpty())
                .map(values -> values.getString(0))
                .filter(value -> !value.isBlank());
    }

    /**
     * List the direct subgroups of a group, by full path.
     * <p>
     * Keycloak moved this around: older versions inline {@code subGroups} in the group
     * representation, newer ones expose a dedicated {@code /children} endpoint and leave the
     * inline list empty. Both are tried, so this works across versions.
     *
     * @return the subgroups, or an empty list when the group has none or Keycloak is unreachable
     */
    public List<JsonObject> getSubgroups(String groupPath) {
        Optional<JsonObject> group = getGroupByPath(groupPath);
        if (group.isEmpty()) {
            return List.of();
        }
        List<JsonObject> inlined = objectsIn(group.get().get("subGroups"));
        if (!inlined.isEmpty()) {
            return inlined;
        }
        String id = group.get().getString("id", null);
        if (id == null) {
            return List.of();
        }
        return objectsIn(get("/admin/realms/" + realm() + "/groups/" + id + "/children?max=" + MAX_SUBGROUPS)
                .orElse(null));
    }

    /**
     * List the members of a group, following pagination to the end.
     * <p>
     * Returns empty when Keycloak cannot be reached, which callers must treat as "no
     * information" rather than "the group is empty" -- revoking on a failed read would strip
     * everyone's permissions.
     *
     * @return the members, or empty when the group does not exist or Keycloak is unreachable
     */
    public Optional<List<JsonObject>> getGroupMembers(String groupId) {
        List<JsonObject> members = new ArrayList<>();
        int first = 0;
        while (true) {
            Optional<JsonValue> page = get("/admin/realms/" + realm() + "/groups/" + groupId
                    + "/members?briefRepresentation=true&first=" + first + "&max=" + MEMBER_PAGE_SIZE);
            if (page.isEmpty()) {
                return Optional.empty();
            }
            List<JsonObject> batch = objectsIn(page.get());
            members.addAll(batch);
            if (batch.size() < MEMBER_PAGE_SIZE) {
                return Optional.of(members);
            }
            first += MEMBER_PAGE_SIZE;
            if (members.size() > MAX_GROUP_MEMBERS) {
                logger.warning("Group " + groupId + " has more than " + MAX_GROUP_MEMBERS
                        + " members; refusing to page further. Raise the limit if this is legitimate.");
                return Optional.of(members);
            }
        }
    }

    private static List<JsonObject> objectsIn(JsonValue value) {
        if (value == null || value.getValueType() != JsonValue.ValueType.ARRAY) {
            return List.of();
        }
        return value.asJsonArray().stream()
                .filter(entry -> entry.getValueType() == JsonValue.ValueType.OBJECT)
                .map(JsonValue::asJsonObject)
                .collect(Collectors.toList());
    }

    /**
     * Drop the cached attributes for a group, so the next read hits Keycloak again.
     * Mainly useful from admin endpoints when someone has just fixed an attribute.
     */
    public void invalidateGroupCache() {
        groupCache.invalidateAll();
    }

    /**
     * @return true when the sync client has enough configuration to be usable at all.
     */
    public boolean isConfigured() {
        return JvmSettings.OIDC_SYNC_CLIENT_ID.lookupOptional().isPresent()
                && JvmSettings.OIDC_SYNC_CLIENT_SECRET.lookupOptional().isPresent()
                && serverUrl() != null
                && realm() != null;
    }

    // ---------------------------------------------------------------- internals

    private Optional<JsonValue> get(String path) {
        Optional<String> token = getAccessToken();
        if (token.isEmpty()) {
            return Optional.empty();
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrl() + path))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Authorization", "Bearer " + token.get())
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 404) {
                // Legitimate answer: the group simply is not there. Not worth a warning.
                logger.fine(() -> "Keycloak admin API: not found for " + path);
                return Optional.empty();
            }
            if (response.statusCode() == 401 || response.statusCode() == 403) {
                // Our token may have been revoked; force a refresh on the next call.
                synchronized (tokenLock) {
                    accessTokenExpiry = Instant.EPOCH;
                }
                logger.warning("Keycloak admin API rejected our service account token for " + path
                        + " (HTTP " + response.statusCode() + "). Check the client's realm-management roles.");
                return Optional.empty();
            }
            if (response.statusCode() >= 300) {
                logger.warning("Keycloak admin API returned HTTP " + response.statusCode() + " for " + path);
                return Optional.empty();
            }
            return Optional.of(parse(response.body()));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            logger.log(Level.WARNING, "Interrupted while calling the Keycloak admin API for " + path, ex);
            return Optional.empty();
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Could not reach the Keycloak admin API for " + path, ex);
            return Optional.empty();
        }
    }

    private Optional<String> getAccessToken() {
        synchronized (tokenLock) {
            if (accessToken != null && Instant.now().isBefore(accessTokenExpiry)) {
                return Optional.of(accessToken);
            }
            accessToken = null;

            Optional<String> clientId = JvmSettings.OIDC_SYNC_CLIENT_ID.lookupOptional();
            Optional<String> clientSecret = JvmSettings.OIDC_SYNC_CLIENT_SECRET.lookupOptional();
            if (clientId.isEmpty() || clientSecret.isEmpty()) {
                logger.warning("Keycloak group sync is enabled but no service account client is configured "
                        + "(dataverse.auth.oidc.sync.client-id / .client-secret).");
                return Optional.empty();
            }

            String form = "grant_type=client_credentials"
                    + "&client_id=" + URLEncoder.encode(clientId.get(), StandardCharsets.UTF_8)
                    + "&client_secret=" + URLEncoder.encode(clientSecret.get(), StandardCharsets.UTF_8);
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(serverUrl() + "/realms/" + realm() + "/protocol/openid-connect/token"))
                        .timeout(REQUEST_TIMEOUT)
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .header("Accept", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(form))
                        .build();

                HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 300) {
                    logger.warning("Could not obtain a Keycloak service account token: HTTP " + response.statusCode());
                    return Optional.empty();
                }
                JsonObject body = parse(response.body()).asJsonObject();
                String token = body.getString("access_token", null);
                if (token == null) {
                    logger.warning("Keycloak token response carried no access_token.");
                    return Optional.empty();
                }
                int expiresIn = body.getInt("expires_in", 60);
                accessToken = token;
                accessTokenExpiry = Instant.now().plusSeconds(expiresIn).minus(TOKEN_EXPIRY_MARGIN);
                return Optional.of(accessToken);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                logger.log(Level.WARNING, "Interrupted while obtaining a Keycloak service account token", ex);
                return Optional.empty();
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Could not obtain a Keycloak service account token", ex);
                return Optional.empty();
            }
        }
    }

    private static JsonValue parse(String body) {
        try (JsonReader reader = Json.createReader(new StringReader(body))) {
            return reader.readValue();
        }
    }

    static String normalisePath(String groupPath) {
        String path = groupPath.trim();
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    /**
     * Base URL of the Keycloak server, without a trailing slash. Derived from the OIDC
     * issuer URL ({@code https://kc/realms/platica} -> {@code https://kc}) unless overridden.
     */
    private String serverUrl() {
        Optional<String> explicit = JvmSettings.OIDC_SYNC_SERVER_URL.lookupOptional();
        if (explicit.isPresent()) {
            return stripTrailingSlash(explicit.get());
        }
        Optional<String> issuer = JvmSettings.OIDC_AUTH_SERVER_URL.lookupOptional();
        if (issuer.isEmpty()) {
            return null;
        }
        String value = stripTrailingSlash(issuer.get());
        int realmsIndex = value.lastIndexOf("/realms/");
        return realmsIndex > 0 ? value.substring(0, realmsIndex) : value;
    }

    /**
     * Realm name. Derived from the OIDC issuer URL unless overridden.
     */
    private String realm() {
        Optional<String> explicit = JvmSettings.OIDC_SYNC_REALM.lookupOptional();
        if (explicit.isPresent()) {
            return explicit.get();
        }
        Optional<String> issuer = JvmSettings.OIDC_AUTH_SERVER_URL.lookupOptional();
        if (issuer.isEmpty()) {
            return null;
        }
        String value = stripTrailingSlash(issuer.get());
        int realmsIndex = value.lastIndexOf("/realms/");
        return realmsIndex > 0 ? value.substring(realmsIndex + "/realms/".length()) : null;
    }

    private static String stripTrailingSlash(String value) {
        String trimmed = value.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
