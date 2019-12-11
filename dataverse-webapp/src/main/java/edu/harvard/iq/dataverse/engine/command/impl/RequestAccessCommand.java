/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;

/**
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
    public DataFile execute(CommandContext ctxt)  {


        file.getFileAccessRequesters().add(requester);
        return ctxt.files().save(file);
    }


}

