package edu.harvard.iq.dataverse.harvest.client;

import edu.harvard.iq.dataverse.api.imports.ImportException;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestType;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestingClient;

import java.util.logging.Logger;

/**
 * Harvester implementations of a harvest type.
 */
public interface Harvester<T extends HarvesterParams> {

    /**
     * What type of harvest the concrete harvester is handling.
     */
    HarvestType harvestType();

    /**
     * The params class the harvester is expecting.
     */
    Class<T> getParamsClass();

    /**
     * Perform the harvest using the provided client configuration and parameters.
     *
     * @param params: Parameters to use for the harvest. Implementations may require specific parameters.
     */
    HarvesterResult harvest(DataverseRequest dataverseRequest, HarvestingClient harvestingClient, Logger hdLogger, T params)
            throws ImportException;
}
