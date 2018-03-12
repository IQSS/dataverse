/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

/**
 *
 * @author gdurand
 */
@RequiredPermissions({})
public class RequestAccessCommand extends AbstractCommand<DataFile> {
    
    private final DataFile file;
    private final AuthenticatedUser requester;


    public RequestAccessCommand(DataverseRequest dvRequest, DataFile file) {
        // for data file check permission on owning dataset
        super(dvRequest, file);        
        this.file = file;        
        this.requester = (AuthenticatedUser) dvRequest.getUser();
    }

    @Override
    public DataFile execute(CommandContext ctxt) throws CommandException {       
        file.getFileAccessRequesters().add(requester);
        return ctxt.files().save(file);
    }



}

