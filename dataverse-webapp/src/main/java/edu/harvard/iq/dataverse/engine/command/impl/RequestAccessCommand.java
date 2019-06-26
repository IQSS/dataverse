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
import edu.harvard.iq.dataverse.util.BundleUtil;

/**
 * @author gdurand
 */
@RequiredPermissions({})
public class RequestAccessCommand extends AbstractCommand<DataFile> {

    private final DataFile file;
    private final AuthenticatedUser requester;
    private final Boolean sendNotification;


    public RequestAccessCommand(DataverseRequest dvRequest, DataFile file) {
        // for data file check permission on owning dataset
        super(dvRequest, file);
        this.file = file;
        this.requester = (AuthenticatedUser) dvRequest.getUser();
        this.sendNotification = false;
    }

    public RequestAccessCommand(DataverseRequest dvRequest, DataFile file, Boolean sendNotification) {
        // for data file check permission on owning dataset
        super(dvRequest, file);
        this.file = file;
        this.requester = (AuthenticatedUser) dvRequest.getUser();
        this.sendNotification = sendNotification;
    }

    @Override
    public DataFile execute(CommandContext ctxt) throws CommandException {


        file.getFileAccessRequesters().add(requester);
        if (sendNotification) {
            ctxt.fileDownload().sendRequestFileAccessNotification(this.file.getOwner(), this.file.getId(), requester);
        }
        return ctxt.files().save(file);
    }


}

