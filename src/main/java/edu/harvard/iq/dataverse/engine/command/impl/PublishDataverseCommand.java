package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.UserNotification;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import static edu.harvard.iq.dataverse.dataverse.DataverseUtil.validateDataverseMetadataExternally;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

@RequiredPermissions(Permission.PublishDataverse)
public class PublishDataverseCommand extends AbstractCommand<Dataverse> {
    private static final Logger logger = Logger.getLogger(PublishDataverseCommand.class.getName());

    private final Dataverse dataverse;

    public PublishDataverseCommand(DataverseRequest aRequest, Dataverse dataverse) {
        super(aRequest, dataverse);
        this.dataverse = dataverse;
    }

    @Override
    public Dataverse execute(CommandContext ctxt) throws CommandException {
        if (dataverse.isReleased()) {
            throw new IllegalCommandException("Dataverse " + dataverse.getAlias() + " has already been published.", this);
        }
        
        Dataverse parent = dataverse.getOwner();
        // root dataverse doesn't have a parent
        if (parent != null) {
            if (!parent.isReleased()) {
                throw new IllegalCommandException("Dataverse " + dataverse.getAlias() + " may not be published because its host dataverse (" + parent.getAlias() + ") has not been published.", this);
            }
        }

        // Perform any optional validation steps, if defined:
        if (ctxt.systemConfig().isExternalDataverseValidationEnabled()) {
            // For admins, an override of the external validation step may be enabled: 
            if (!(getUser().isSuperuser() && ctxt.systemConfig().isExternalValidationAdminOverrideEnabled())) {
                String executable = ctxt.systemConfig().getDataverseValidationExecutable();
                boolean result = validateDataverseMetadataExternally(dataverse, executable, getRequest());
            
                if (!result) {
                    String rejectionMessage = ctxt.systemConfig().getDataverseValidationFailureMsg();
                    throw new IllegalCommandException(rejectionMessage, this);
                }
            }
        }
        
        //Before setting dataverse to released send notifications to users with download file
        List<RoleAssignment> ras = ctxt.roles().directRoleAssignments(dataverse);
        for (RoleAssignment ra : ras) {
            if (ra.getRole().permissions().contains(Permission.DownloadFile)) {
                for (AuthenticatedUser au : ctxt.roleAssignees().getExplicitUsers(ctxt.roleAssignees().getRoleAssignee(ra.getAssigneeIdentifier()))) {
                    ctxt.notifications().sendNotification(au, new Timestamp(new Date().getTime()), UserNotification.Type.ASSIGNROLE, dataverse.getId());
                }
            }
        }

        dataverse.setPublicationDate(new Timestamp(new Date().getTime()));
        dataverse.setReleaseUser((AuthenticatedUser) getUser());
        Dataverse savedDataverse = ctxt.dataverses().save(dataverse);
        
        return savedDataverse;

    }
    
    @Override
    public boolean onSuccess(CommandContext ctxt, Object r) {
        return ctxt.dataverses().index((Dataverse) r,true);
    }

}
