package io.gdcc.dvopa.crd;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;
import io.gdcc.dvopa.DvOpa;

@Group(DvOpa.API_GROUP)
@Version(DvOpa.API_VERSION)
public class DataverseInstance extends CustomResource<DataverseInstanceSpec, DataverseInstanceStatus> implements Namespaced {
    @Override
    public String toString() {
        return "DataverseInstance{" +
            "spec=" + spec +
            ", status=" + status +
            '}';
    }
}
