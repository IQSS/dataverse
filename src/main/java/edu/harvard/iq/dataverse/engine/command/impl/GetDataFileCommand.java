/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Matthew
 */
// no annotations here, since permissions are dynamically decided
// based off GetDatasetCommand for similar permissions checking
public class GetDataFileCommand extends AbstractCommand<DataFile> {

    private final DataFile df;

    public GetDataFileCommand(DataverseRequest aRequest, DataFile anAffectedDataset) {
        super(aRequest, anAffectedDataset);
        df = anAffectedDataset;
    }

    @Override
    public DataFile execute(CommandContext ctxt) throws CommandException {
        return df;
    }

    @Override
    public Map<String, Set<Permission>> getRequiredPermissions() {
        return Collections.singletonMap("",
                df.isReleased() ? Collections.<Permission>emptySet()
                : Collections.singleton(Permission.ViewUnpublishedDataset));
    }

}
