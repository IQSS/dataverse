package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

public class CheckRateLimitForDatasetPageCommand  extends AbstractVoidCommand {

    public CheckRateLimitForDatasetPageCommand(DataverseRequest aRequest, DvObject dvObject) {
        super(aRequest, dvObject);
    }

    @Override
    protected void executeImpl(CommandContext ctxt) throws CommandException { }
}
