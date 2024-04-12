/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.FileAccessRequest;
import edu.harvard.iq.dataverse.GuestbookResponse;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.FileUtil;

/**
 *
 * @author gdurand
 */
@RequiredPermissions({})
public class RequestAccessCommand extends AbstractCommand<DataFile> {

    private static final Logger logger = Logger.getLogger(RequestAccessCommand.class.getName());

    private final DataFile file;
    private final AuthenticatedUser requester;
    private final FileAccessRequest fileAccessRequest;
    private final Boolean sendNotification;

    public RequestAccessCommand(DataverseRequest dvRequest, DataFile file) {
        // for data file check permission on owning dataset
        this(dvRequest, file, false);
    }

    public RequestAccessCommand(DataverseRequest dvRequest, DataFile file, Boolean sendNotification) {
        // for data file check permission on owning dataset
        super(dvRequest, file);
        this.file = file;
        this.requester = (AuthenticatedUser) dvRequest.getUser();
        this.fileAccessRequest = new FileAccessRequest(file, requester);
        this.sendNotification = sendNotification;
    }

    public RequestAccessCommand(DataverseRequest dvRequest, DataFile file, GuestbookResponse gbr) {
        this(dvRequest, file, gbr, false);
    }

    public RequestAccessCommand(DataverseRequest dvRequest, DataFile file, GuestbookResponse gbr,
            Boolean sendNotification) {
        // for data file check permission on owning dataset
        super(dvRequest, file);
        this.file = file;
        this.requester = (AuthenticatedUser) dvRequest.getUser();
        this.fileAccessRequest = new FileAccessRequest(file, requester, gbr);
        this.sendNotification = sendNotification;
    }

    @Override
    public DataFile execute(CommandContext ctxt) throws CommandException {

        if (!file.getOwner().isFileAccessRequest()) {
            throw new CommandException(BundleUtil.getStringFromBundle("file.requestAccess.notAllowed"), this);
        }

        // if user already has permission to download file or the file is public throw
        // command exception
        logger.fine("User: " + this.getRequest().getAuthenticatedUser().getName());
        logger.fine("File: " + file.getId() + " : restricted?: " + file.isRestricted());
        logger.fine(
                "permission?: " + ctxt.permissions().requestOn(this.getRequest(), file).has(Permission.DownloadFile));
        if (!file.isRestricted()
                || ctxt.permissions().requestOn(this.getRequest(), file).has(Permission.DownloadFile)) {
            throw new CommandException(
                    BundleUtil.getStringFromBundle("file.requestAccess.notAllowed.alreadyHasDownloadPermisssion"),
                    this);
        }

        if (FileUtil.isActivelyEmbargoed(file)) {
            throw new CommandException(BundleUtil.getStringFromBundle("file.requestAccess.notAllowed.embargoed"), this);
        }
        file.addFileAccessRequest(fileAccessRequest);
        List<FileAccessRequest> fars = requester.getFileAccessRequests();
        if(fars!=null) {
            fars.add(fileAccessRequest);
        } else {
            requester.setFileAccessRequests(Arrays.asList(fileAccessRequest));
        }
        DataFile savedFile = ctxt.files().save(file);
        if (sendNotification) {
            logger.fine("ctxt.fileDownload().sendRequestFileAccessNotification(savedFile, requester);");
            ctxt.fileDownload().sendRequestFileAccessNotification(savedFile.getOwner(), savedFile.getId(), requester);
        }
        return savedFile;
    }

}
