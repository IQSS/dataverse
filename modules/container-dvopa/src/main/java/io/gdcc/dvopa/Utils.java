package io.gdcc.dvopa;

import io.gdcc.dvopa.crd.DataverseInstance;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusUpdateControl;

public class Utils {
    
    public static final String RECOMMENDED_LABEL_PREFIX = "app.kubernetes.io";
    public static final String RECOMMENDED_LABEL_NAME = RECOMMENDED_LABEL_PREFIX + "/name";
    public static final String RECOMMENDED_LABEL_INSTANCE = RECOMMENDED_LABEL_PREFIX + "/instance";
    public static final String RECOMMENDED_LABEL_VERSION = RECOMMENDED_LABEL_PREFIX + "/version";
    public static final String RECOMMENDED_LABEL_COMPONENT = RECOMMENDED_LABEL_PREFIX + "/component";
    public static final String RECOMMENDED_LABEL_PART_OF = RECOMMENDED_LABEL_PREFIX + "/part-of";
    public static final String RECOMMENDED_LABEL_MANAGED_BY = RECOMMENDED_LABEL_PREFIX + "/managed-by";
    
    public static String deploymentName(DataverseInstance dvInstance) {
        return dvInstance.getMetadata().getName();
    }
    
    public static ErrorStatusUpdateControl<DataverseInstance> handleError(DataverseInstance resource, Exception e) {
        //resource.getStatus().setErrorMessage("Error: " + e.getMessage());
        return ErrorStatusUpdateControl.updateStatus(resource);
    }
}
