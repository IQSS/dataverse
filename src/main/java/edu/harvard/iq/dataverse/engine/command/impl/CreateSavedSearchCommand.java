/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.search.savedsearch.SavedSearch;

/**
 *
 * @author skraffmi
 */
@RequiredPermissions( Permission.PublishDataverse )
public class CreateSavedSearchCommand extends AbstractCommand<SavedSearch> {
    
    private final SavedSearch created;

    public CreateSavedSearchCommand(User aUser, DvObject anAffectedDvObject, SavedSearch savedSearch) {
        super(aUser, anAffectedDvObject);
        this.created = savedSearch;
    }


    @Override
    public SavedSearch execute(CommandContext ctxt) throws CommandException {
        String query = created.getQuery();
        if (query == null) {
            String wildcardSearch = "*";
            created.setQuery(wildcardSearch);
        }
        return ctxt.savedSearches().save(created);
    }
    
}
