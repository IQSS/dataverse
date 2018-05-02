/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.GuestbookResponse;
import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import java.sql.Timestamp;
import java.util.Date;

/**
 *
 * @author skraffmiller
 */
@RequiredPermissions({})
public class CreateGuestbookResponseCommand extends AbstractVoidCommand  {
    private final GuestbookResponse response;
    public CreateGuestbookResponseCommand(DataverseRequest aRequest, GuestbookResponse responseIn, Dataset affectedDataset) {
        super(aRequest, affectedDataset);
        response = responseIn;
    }

    @Override
    protected void executeImpl(CommandContext ctxt) throws CommandException {
       Timestamp createDate = new Timestamp(new Date().getTime());
       response.setResponseTime(createDate);
       ctxt.responses().save(response);
    }
    
}
