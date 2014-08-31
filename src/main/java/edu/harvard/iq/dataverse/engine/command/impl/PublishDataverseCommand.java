package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import java.sql.Timestamp;
import java.util.Date;

@RequiredPermissions(Permission.Publish)
public class PublishDataverseCommand extends AbstractCommand<Dataverse> {

    private final Dataverse dataverse;
    private final User dataverseUser;

    public PublishDataverseCommand(User dataverseUser, Dataverse dataverse) {
        super(dataverseUser, dataverse);
        this.dataverse = dataverse;
        this.dataverseUser = dataverseUser;
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

        dataverse.setPublicationDate(new Timestamp(new Date().getTime()));
        dataverse.setReleaseUserIdentifier(dataverseUser.getIdentifier());
        return ctxt.dataverses().save(dataverse);
    }

}
