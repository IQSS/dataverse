package io.gdcc.dvopa.resources;

import io.fabric8.crd.generator.annotation.Labels;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.gdcc.dvopa.DvOpa;
import io.gdcc.dvopa.Utils;
import io.gdcc.dvopa.crd.DataverseInstance;
import io.javaoperatorsdk.operator.ReconcilerUtils;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

import static io.gdcc.dvopa.Utils.RECOMMENDED_LABEL_INSTANCE;
import static io.gdcc.dvopa.Utils.RECOMMENDED_LABEL_MANAGED_BY;
import static io.gdcc.dvopa.Utils.RECOMMENDED_LABEL_VERSION;

@KubernetesDependent(labelSelector = RECOMMENDED_LABEL_MANAGED_BY)
public class InstanceDeployment extends CRUDKubernetesDependentResource<Deployment, DataverseInstance> {
    
    public InstanceDeployment() {
        super(Deployment.class);
    }
    
    @Override
    protected Deployment desired(DataverseInstance primaryResource, Context<DataverseInstance> context) {
        // Load template
        Deployment deployment = ReconcilerUtils.loadYaml(Deployment.class, Utils.class, "deployment.yml");
        
        // Make it appear within the right namespace and copy version
        deployment.getMetadata().setNamespace(primaryResource.getMetadata().getNamespace());
        deployment.getMetadata().getLabels().put(RECOMMENDED_LABEL_VERSION, primaryResource.getSpec().version());
        
        // Add name of the instance as instance-name everywhere (remember: selector and template labels must match!)
        String deploymentName = Utils.deploymentName(primaryResource);
        deployment.getMetadata().setName(deploymentName);
        deployment.getMetadata().getLabels().put(RECOMMENDED_LABEL_INSTANCE, deploymentName);
        deployment.getSpec().getSelector().getMatchLabels().put(RECOMMENDED_LABEL_INSTANCE, deploymentName);
        deployment.getSpec().getTemplate().getMetadata().getLabels().put(RECOMMENDED_LABEL_INSTANCE, deploymentName);
        
        // Add label for managed-by everywhere (remember: selector and template labels must match!)
        deployment.getMetadata().getLabels().put(RECOMMENDED_LABEL_MANAGED_BY, DvOpa.NAME);
        deployment.getSpec().getSelector().getMatchLabels().put(RECOMMENDED_LABEL_MANAGED_BY, DvOpa.NAME);
        deployment.getSpec().getTemplate().getMetadata().getLabels().put(RECOMMENDED_LABEL_MANAGED_BY, DvOpa.NAME);
        
        return deployment;
    }
}
