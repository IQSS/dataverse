package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.MapLayerMetadata;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import java.util.logging.Logger;

@RequiredPermissions(Permission.EditDataset)
public class DeleteMapLayerMetadataCommand extends AbstractCommand<Boolean> {

    private static final Logger logger = Logger.getLogger(DeleteMapLayerMetadataCommand.class.getCanonicalName());

    private final DataFile dataFile;

    public DeleteMapLayerMetadataCommand(DataverseRequest aRequest, DataFile datafile) {
        super(aRequest, datafile);
        this.dataFile = datafile;
    }

    @Override
    public Boolean execute(CommandContext ctxt) throws CommandException {
        if (dataFile == null) {
            return false;
        }
        MapLayerMetadata mapLayerMetadata = ctxt.mapLayerMetadata().findMetadataByDatafile(dataFile);
        if (mapLayerMetadata == null) {
            return false;
        }
        boolean mapDeleted = ctxt.mapLayerMetadata().deleteMapLayerMetadataObject(mapLayerMetadata, getUser());
        logger.info("Boolean returned from deleteMapLayerMetadataObject: " + mapDeleted);
        return mapDeleted;

    }

}
