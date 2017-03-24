package edu.harvard.iq.dataverse.engine.command.impl;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.MapLayerMetadata;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.logging.Logger;

@RequiredPermissions(Permission.EditDataset)
public class CheckMapLayerMetadataCommand extends AbstractCommand<MapLayerMetadata> {

    private static final Logger logger = Logger.getLogger(CheckMapLayerMetadataCommand.class.getCanonicalName());

    private final DataFile dataFile;

    public CheckMapLayerMetadataCommand(DataverseRequest aRequest, DataFile datafile) {
        super(aRequest, datafile);
        this.dataFile = datafile;
    }

    @Override
    public MapLayerMetadata execute(CommandContext ctxt) throws CommandException {
        if (dataFile == null) {
            return null;
        }
        MapLayerMetadata mapLayerMetadata = ctxt.mapLayerMetadata().findMetadataByDatafile(dataFile);
        if (mapLayerMetadata == null) {
            return null;
        }
        String layerLink = mapLayerMetadata.getLayerLink();
        logger.info(layerLink);
        GetRequest getRequest = Unirest.get(layerLink);
        try {
            int statusCode = getRequest.asBinary().getStatus();
            mapLayerMetadata.setLastVerifiedStatus(statusCode);
            Timestamp now = new Timestamp(new Date().getTime());
            /**
             * @todo Figure out why this timestamp isn't being persisted.
             */
            mapLayerMetadata.setLastVerifiedTime(now);
            logger.info("Setting status code to " + statusCode + " and timestamp to " + now + " for MapLayerMetadata id " + mapLayerMetadata.getId() + " from DataFile id " + dataFile.getId());
            return ctxt.mapLayerMetadata().save(mapLayerMetadata);
        } catch (UnirestException ex) {
            logger.info("Couldn't update last verfied status code or timestamp: " + ex.getLocalizedMessage());
            return null;
        }
    }

}
