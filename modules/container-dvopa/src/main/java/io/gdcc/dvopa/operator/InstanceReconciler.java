package io.gdcc.dvopa.operator;

import io.gdcc.dvopa.Utils;
import io.gdcc.dvopa.crd.DataverseInstance;
import io.gdcc.dvopa.resources.InstanceDeployment;
import io.javaoperatorsdk.operator.api.reconciler.Cleaner;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusHandler;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusUpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;

@ControllerConfiguration(
    dependents = {
        @Dependent(type = InstanceDeployment.class),
    })
public class InstanceReconciler
    implements Reconciler<DataverseInstance>, ErrorStatusHandler<DataverseInstance>, Cleaner<DataverseInstance> {
    
    @Override
    public DeleteControl cleanup(DataverseInstance instance, Context<DataverseInstance> context) {
        return DeleteControl.defaultDelete();
    }
    
    @Override
    public ErrorStatusUpdateControl<DataverseInstance> updateErrorStatus(DataverseInstance instance, Context<DataverseInstance> context, Exception e) {
        return Utils.handleError(instance, e);
    }
    
    @Override
    public UpdateControl<DataverseInstance> reconcile(DataverseInstance instance, Context<DataverseInstance> context) throws Exception {
        return UpdateControl.patchStatus(instance);
    }
}
