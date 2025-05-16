package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.dataverse.featured.DataverseFeaturedItem;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Retrieves a particular featured item {@link DataverseFeaturedItem}.
 */
public class GetDataverseFeaturedItemCommand extends AbstractCommand<DataverseFeaturedItem> {

    private final DataverseFeaturedItem dataverseFeaturedItem;

    public GetDataverseFeaturedItemCommand(DataverseRequest request, DataverseFeaturedItem dataverseFeaturedItem) {
        super(request, dataverseFeaturedItem.getDvObject());
        this.dataverseFeaturedItem = dataverseFeaturedItem;
    }

    @Override
    public DataverseFeaturedItem execute(CommandContext ctxt) throws CommandException {
        return dataverseFeaturedItem;
    }

    @Override
    public Map<String, Set<Permission>> getRequiredPermissions() {
        return Collections.singletonMap("",
                dataverseFeaturedItem.getDvObject().isReleased() ? Collections.emptySet()
                        : Collections.singleton(Permission.ViewUnpublishedDataverse));
    }
}
