package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.PrivateUrlUser;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.privateurl.PrivateUrl;
import edu.harvard.iq.dataverse.util.BundleUtil;

import java.util.UUID;
import java.util.logging.Logger;

@RequiredPermissions(Permission.ManageDatasetPermissions)
public class CreatePrivateUrlCommand extends AbstractCommand<PrivateUrl> {

    private static final Logger logger = Logger.getLogger(CreatePrivateUrlCommand.class.getCanonicalName());

    final Dataset dataset;
    final boolean anonymizedAccess;

    public CreatePrivateUrlCommand(DataverseRequest dataverseRequest, Dataset theDataset, boolean anonymizedAccess) {
        super(dataverseRequest, theDataset);
        dataset = theDataset;
        this.anonymizedAccess = anonymizedAccess;
    }

    @Override
    public PrivateUrl execute(CommandContext ctxt) throws CommandException {
        logger.fine("Executing CreatePrivateUrlCommand...");
        if (dataset == null) {
            logger.info("Can't create Private URL. Dataset is null.");
            throw new IllegalCommandException(BundleUtil.getStringFromBundle("datasets.api.privateurl.error.datasetnotfound"), this);
        }
        PrivateUrl existing = ctxt.privateUrl().getPrivateUrlFromDatasetId(dataset.getId());
        if (existing != null) {
            logger.info("Private URL already exists for dataset id " + dataset.getId() + ".");
            throw new IllegalCommandException(BundleUtil.getStringFromBundle("datasets.api.privateurl.error.alreadyexists"), this);
        }
        DatasetVersion latestVersion = dataset.getLatestVersion();
        if (!latestVersion.isDraft()) {
            logger.info("Can't create Private URL because the latest version of dataset id" + dataset.getId() + " is not a draft.");
            throw new IllegalCommandException(BundleUtil.getStringFromBundle("datasets.api.privateurl.error.notdraft"), this);
        }
        if (dataset.isReleased() && anonymizedAccess) {
            logger.info("Can't create anonymized access Private URL because the dataset id" + dataset.getId() + " has a published version.");
            throw new IllegalCommandException(BundleUtil.getStringFromBundle("datasets.api.privateurl.anonymized.error.released"), this);
        }
        PrivateUrlUser privateUrlUser = new PrivateUrlUser(dataset.getId(), anonymizedAccess);
        DataverseRole memberRole = ctxt.roles().findBuiltinRoleByAlias(DataverseRole.MEMBER);
        final String privateUrlToken = UUID.randomUUID().toString();
        RoleAssignment roleAssignment = ctxt.engine().submit(new AssignRoleCommand(privateUrlUser, memberRole, dataset, getRequest(), privateUrlToken, anonymizedAccess));
        PrivateUrl privateUrl = new PrivateUrl(roleAssignment, dataset, ctxt.systemConfig().getDataverseSiteUrl());
        return privateUrl;
    }

}
