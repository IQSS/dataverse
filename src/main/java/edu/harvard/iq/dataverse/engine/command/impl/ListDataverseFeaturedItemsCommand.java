package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.dataverse.featured.DataverseFeaturedItem;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

import java.util.*;

/**
 * Lists the featured items {@link DataverseFeaturedItem} of a {@link Dataverse}.
 */
public class ListDataverseFeaturedItemsCommand extends AbstractCommand<List<DataverseFeaturedItem>> {

    private final Dataverse dataverse;

    public ListDataverseFeaturedItemsCommand(DataverseRequest request, Dataverse dataverse) {
        super(request, dataverse);
        this.dataverse = dataverse;
    }

    @Override
    public List<DataverseFeaturedItem> execute(CommandContext ctxt) throws CommandException {
        return ctxt.dataverseFeaturedItems().findAllByDataverseOrdered(dataverse, true);
    }

    @Override
    public Map<String, Set<Permission>> getRequiredPermissions() {
        return Collections.singletonMap("",
                dataverse.isReleased() ? Collections.emptySet()
                        : Collections.singleton(Permission.ViewUnpublishedDataverse));
    }
}
