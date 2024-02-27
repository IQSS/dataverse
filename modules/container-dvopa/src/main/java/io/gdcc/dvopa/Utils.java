package io.gdcc.dvopa;

import io.gdcc.dvopa.crd.DataverseInstance;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusUpdateControl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    
    public static final String RECOMMENDED_LABEL_PREFIX = "app.kubernetes.io";
    public static final String RECOMMENDED_LABEL_NAME = RECOMMENDED_LABEL_PREFIX + "/name";
    public static final String RECOMMENDED_LABEL_INSTANCE = RECOMMENDED_LABEL_PREFIX + "/instance";
    public static final String RECOMMENDED_LABEL_VERSION = RECOMMENDED_LABEL_PREFIX + "/version";
    public static final String RECOMMENDED_LABEL_COMPONENT = RECOMMENDED_LABEL_PREFIX + "/component";
    public static final String RECOMMENDED_LABEL_PART_OF = RECOMMENDED_LABEL_PREFIX + "/part-of";
    public static final String RECOMMENDED_LABEL_MANAGED_BY = RECOMMENDED_LABEL_PREFIX + "/managed-by";
    
    // Image name validations patterns, taken directly from the docker source
    // https://github.com/docker/docker/blob/04da4041757370fb6f85510c8977c5a18ddae380/vendor/github.com/docker/distribution/reference/regexp.go#L18
    private static final String REGEX_NAME_COMPONENT = "[a-z0-9]+(?:(?:(?:[._]|__|[-]*)[a-z0-9]+)+)?";
    // https://github.com/docker/docker/blob/04da4041757370fb6f85510c8977c5a18ddae380/vendor/github.com/docker/distribution/reference/regexp.go#L53
    private static final String REGEX_IMAGE_NAME = REGEX_NAME_COMPONENT + "(?:(?:/" + REGEX_NAME_COMPONENT + ")+)?";
    
    // https://github.com/docker/docker/blob/04da4041757370fb6f85510c8977c5a18ddae380/vendor/github.com/docker/distribution/reference/regexp.go#L25
    // NOTE: This is not aligned with RFC 2181, which allows a lot more characters for domains.
    //       UTF-8 domains are not allowed, too. It is, however, in line with RFC 1123, about valid hostnames.
    //       As this regex is about what Docker / containers support, this is probably fine.
    //       See also https://stackoverflow.com/a/2183140
    private static final String REGEX_DOMAIN_COMPONENT = "(?:[a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9-]*[a-zA-Z0-9])";
    private static final String REGEX_PORT_COMPONTENT = "(?::(?:6553[0-5]|655[0-2]\\d|65[0-4]\\d\\d]|6[0-4]\\d{3}|[0-5]?\\d{1,4}))";
    // https://github.com/docker/docker/blob/04da4041757370fb6f85510c8977c5a18ddae380/vendor/github.com/docker/distribution/reference/regexp.go#L31
    // NOTE: Limiting to 25 domain components here, which seams reasonable for even very long and complex FQDNs
    private static final String REGEX_REGISTRY = REGEX_DOMAIN_COMPONENT + "(?:\\." + REGEX_DOMAIN_COMPONENT + "){0,24}" + REGEX_PORT_COMPONTENT + "?";
    
    // https://github.com/docker/docker/blob/04da4041757370fb6f85510c8977c5a18ddae380/vendor/github.com/docker/distribution/reference/regexp.go#L37
    private static final String REGEXP_TAG = "\\w[\\w.-]{0,127}";
    private static final String REGEXP_DIGEST = "sha256:[a-z0-9]{32,}";
    
    public static final String REGEXP_IMAGE_REFERENCE = "^(?:" + REGEX_REGISTRY + ")?" + REGEX_IMAGE_NAME + "(:" + REGEXP_TAG + "|@" + REGEXP_DIGEST + ")?$";
    public static final Matcher CONTAINER_IMAGE_REFERENCE = Pattern.compile(REGEXP_IMAGE_REFERENCE).matcher("");
    
    public static String deploymentName(DataverseInstance dvInstance) {
        return dvInstance.getMetadata().getName();
    }
    
    public static ErrorStatusUpdateControl<DataverseInstance> handleError(DataverseInstance resource, Exception e) {
        //resource.getStatus().setErrorMessage("Error: " + e.getMessage());
        return ErrorStatusUpdateControl.updateStatus(resource);
    }
    
    public static boolean validateImage(String image) {
        if (image == null) {
            return false;
        }
        return CONTAINER_IMAGE_REFERENCE.reset(image).matches();
    }
    
    public static String getTagFromImage(String image) {
        if (image == null || ! CONTAINER_IMAGE_REFERENCE.reset(image).find()) {
            return null;
        }
        String group1 = CONTAINER_IMAGE_REFERENCE.group(1);
        if (group1 == null || group1.isEmpty()) {
            return "latest";
        }
        // Cut off the first char (will be ":" or "@") before returning
        return group1.substring(1);
    }
}
