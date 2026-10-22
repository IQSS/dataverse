package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.search.SearchException;
import edu.harvard.iq.dataverse.search.savedsearch.SavedSearch;
import edu.harvard.iq.dataverse.search.savedsearch.SavedSearchServiceBean;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.json.JsonObjectBuilder;

@RequiredPermissions(Permission.PublishDataverse)
public class CreateSavedSearchCommand extends AbstractCommand<SavedSearch> {

    private static final Logger logger = Logger.getLogger(SavedSearchServiceBean.class.getCanonicalName());

    private final SavedSearch savedSearchToCreate;

    public CreateSavedSearchCommand(DataverseRequest aRequest, DvObject anAffectedDvObject, SavedSearch savedSearch) {
        super(aRequest, anAffectedDvObject);
        this.savedSearchToCreate = savedSearch;
    }

    @Override
    public SavedSearch execute(CommandContext ctxt) throws CommandException {
        String query = savedSearchToCreate.getQuery();
        if (query == null) {
            /**
             * @todo This probably shouldn't be the default. We are disallowing
             * wildcard searches. Go fix the DataversePage and make sure the
             * query is set.
             */
            String wildcardSearch = "*";
            savedSearchToCreate.setQuery(wildcardSearch);
        }
        SavedSearch persistedSavedSearch = ctxt.savedSearches().save(savedSearchToCreate);
        if (persistedSavedSearch != null) {
            try {
                DataverseRequest dataverseRequest = new DataverseRequest(savedSearchToCreate.getCreator(), SavedSearchServiceBean.getHttpServletRequest());
                JsonObjectBuilder result = ctxt.savedSearches().makeLinksForSingleSavedSearch(dataverseRequest, persistedSavedSearch, true);
                logger.log(Level.INFO, "result from attempt to make links from saved search: {0}", result.build().toString());
            } catch (SearchException ex) {
                logger.info(ex.getLocalizedMessage());
            }
            return persistedSavedSearch;
        } else {
            return null;
        }
    }

}
