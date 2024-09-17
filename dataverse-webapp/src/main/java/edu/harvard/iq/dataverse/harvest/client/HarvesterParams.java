package edu.harvard.iq.dataverse.harvest.client;

/**
 * Input harvest parameters or empty, if harvester needs no parameters.
 */
public abstract class HarvesterParams {

    public static HarvesterParams empty() {
        return new EmptyHarvesterParams();
    }

    final public <T extends HarvesterParams> T getParams(Class<T> paramsClass) {
        if (!paramsClass.isAssignableFrom(this.getClass())) {
            throw new IllegalStateException("Invalid parameters. Required:" + paramsClass);
        }

        return paramsClass.cast(this);
    }

    public static class EmptyHarvesterParams extends HarvesterParams {
    }
}
