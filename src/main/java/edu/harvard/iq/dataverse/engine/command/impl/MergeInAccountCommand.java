/*

 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DatasetLock;
import edu.harvard.iq.dataverse.DatasetVersionUser;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.GuestbookResponse;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.UserNotification;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserLookup;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUser;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.OAuth2TokenData;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.batch.util.LoggingUtil;
import edu.harvard.iq.dataverse.confirmemail.ConfirmEmailData;
import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.passwordreset.PasswordResetData;
import edu.harvard.iq.dataverse.search.IndexResponse;
import edu.harvard.iq.dataverse.search.savedsearch.SavedSearch;
import edu.harvard.iq.dataverse.workflows.WorkflowComment;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import org.apache.solr.client.solrj.SolrServerException;

/**
 * Merges one account into another.
 * 
 * @author matthew
 */

@RequiredPermissions({})
public class MergeInAccountCommand extends AbstractVoidCommand {
    final AuthenticatedUser consumedAU;
    final AuthenticatedUser ongoingAU;

    private static final Logger logger = Logger.getLogger(MergeInAccountCommand.class.getCanonicalName());
    
    public MergeInAccountCommand(DataverseRequest createDataverseRequest, AuthenticatedUser consumedAuthenticatedUser, AuthenticatedUser ongoingAU) {
        super(
            createDataverseRequest,
            (DvObject) null
        );
        consumedAU = consumedAuthenticatedUser;
        this.ongoingAU = ongoingAU;
    }
    
    @Override
    protected void executeImpl(CommandContext ctxt) throws CommandException {

        if (consumedAU.getId() == ongoingAU.getId()) {
            throw new IllegalCommandException("You cannot merge an account into itself.", this);
        }

        if (consumedAU.isDeactivated() && !ongoingAU.isDeactivated() || !consumedAU.isDeactivated() && ongoingAU.isDeactivated()) {
            throw new IllegalCommandException("User accounts can only be merged if they are either both active or both deactivated.", this);
        }

        List<RoleAssignment> baseRAList = ctxt.roleAssignees().getAssignmentsFor(ongoingAU.getIdentifier());
        List<RoleAssignment> consumedRAList = ctxt.roleAssignees().getAssignmentsFor(consumedAU.getIdentifier());
        
        for(RoleAssignment cra : consumedRAList) {
            if(cra.getAssigneeIdentifier().charAt(0) == '@') {
                
                boolean willDelete = false;
                for(RoleAssignment bra : baseRAList) {
                    //Matching on the id not the whole DVObject as I'm suspicious of dvobject equality
                    if( bra.getDefinitionPoint().getId().equals(cra.getDefinitionPoint().getId())
                        && bra.getRole().equals(cra.getRole())) { 
                        willDelete = true; //more or less a skip, as we run a delete query afterwards
                    }
                }
                if(!willDelete) {
                    cra.setAssigneeIdentifier(ongoingAU.getIdentifier());
                    ctxt.em().merge(cra);
                    IndexResponse indexResponse = ctxt.solrIndex().indexPermissionsForOneDvObject(cra.getDefinitionPoint());
                    try {
                        ctxt.index().indexDvObject(cra.getDefinitionPoint());
                    } catch (IOException | SolrServerException e) {
                        String failureLogText = "Post merge account dataset indexing failed. You can kickoff a re-index of this dataset with: \r\n curl http://localhost:8080/api/admin/index/datasets/" + cra.getDefinitionPoint().getId().toString();
                        failureLogText += "\r\n" + e.getLocalizedMessage();
                        LoggingUtil.writeOnSuccessFailureLog(this, failureLogText, cra.getDefinitionPoint());

                    }                   
                } // no else here because the any willDelete == true will happen in the named query below.
            } else {
                throw new IllegalCommandException("Original userIdentifier provided does not seem to be an AuthenticatedUser", this);
            }
        }
        
        //Delete role assignments for consumedIdentifier not merged, e.g. duplicates
        int resultCount = ctxt.em().createNamedQuery("RoleAssignment.deleteAllByAssigneeIdentifier", RoleAssignment.class).
                        setParameter("assigneeIdentifier", consumedAU.getIdentifier())
                        .executeUpdate();
        
        // DatasetVersionUser
        for (DatasetVersionUser user : ctxt.datasetVersion().getDatasetVersionUsersByAuthenticatedUser(consumedAU)) {
            user.setAuthenticatedUser(ongoingAU);
            ctxt.em().merge(user);
        }
        
        //DatasetLocks
        for (DatasetLock lock : ctxt.datasets().getDatasetLocksByUser(consumedAU)) {
            lock.setUser(ongoingAU);
            ctxt.em().merge(lock);
        }

        //DVObjects creator and release
        for (DvObject dvo : ctxt.dvObjects().findByAuthenticatedUserId(consumedAU)) {
            if (dvo.getCreator().equals(consumedAU)){
                dvo.setCreator(ongoingAU);
            }
            if (dvo.getReleaseUser() != null &&  dvo.getReleaseUser().equals(consumedAU)){
                dvo.setReleaseUser(ongoingAU);
            }
            ctxt.em().merge(dvo);
        }
        
        //GuestbookResponse
        for (GuestbookResponse gbr : ctxt.responses().findByAuthenticatedUserId(consumedAU)) {
            gbr.setAuthenticatedUser(ongoingAU);
            ctxt.em().merge(gbr);
        }
        
        //UserNotification
        for (UserNotification note : ctxt.notifications().findByUser(consumedAU.getId())) {
            note.setUser(ongoingAU);
            ctxt.em().merge(note);
        }
        
        //UserNotification
        for (UserNotification note : ctxt.notifications().findByRequestor(consumedAU.getId())) {
            note.setRequestor(ongoingAU);
            ctxt.em().merge(note);
        }
        
        // Set<ExplicitGroup>
        //for () 
        
        //SavedSearch
        for (SavedSearch search : ctxt.savedSearches().findByAuthenticatedUser(consumedAU)) {
            search.setCreator(ongoingAU);
            ctxt.em().merge(search);
        }
        
        //Workflow Comments
        for (WorkflowComment wc : ctxt.authentication().getWorkflowCommentsByAuthenticatedUser(consumedAU)) {
            wc.setAuthenticatedUser(ongoingAU);
            ctxt.em().merge(wc);
        }
        

        
        //ConfirmEmailData  
        
        // todo: the deletion should be handed down to the service!
        ConfirmEmailData confirmEmailData = ctxt.confirmEmail().findSingleConfirmEmailDataByUser(consumedAU); 
        if (confirmEmailData != null){
            ctxt.em().remove(confirmEmailData);
        }

        
        //Access Request is not an entity. have to update with native query
        
        ctxt.em().createNativeQuery("UPDATE fileaccessrequests SET authenticated_user_id="+ongoingAU.getId()+" WHERE authenticated_user_id="+consumedAU.getId()).executeUpdate();
        
        ctxt.em().createNativeQuery("Delete from OAuth2TokenData where user_id ="+consumedAU.getId()).executeUpdate();
        
        ctxt.em().createNativeQuery("UPDATE explicitgroup_authenticateduser SET containedauthenticatedusers_id="+ongoingAU.getId()+" WHERE containedauthenticatedusers_id="+consumedAU.getId()).executeUpdate();
        
        ctxt.actionLog().changeUserIdentifierInHistory(consumedAU.getIdentifier(), ongoingAU.getIdentifier());
        
        //delete:
        //  builtin user - if applicable
        //  authenticated user
        //  AuthenticatedUserLookup
        //  apiToken
        ApiToken toRemove = ctxt.authentication().findApiTokenByUser(consumedAU);
        if(null != toRemove) { //not all users have apiTokens
            ctxt.em().remove(toRemove);
        }
        AuthenticatedUserLookup consumedAUL = consumedAU.getAuthenticatedUserLookup();
        ctxt.em().remove(consumedAUL);
        ctxt.em().remove(consumedAU);
        BuiltinUser consumedBuiltinUser = ctxt.builtinUsers().findByUserName(consumedAU.getUserIdentifier());
        if (consumedBuiltinUser != null) {
            ctxt.builtinUsers().removeUser(consumedBuiltinUser.getUserName());
        }
        
        
    }
    
    @Override
    public String describe() {
        return "User " + consumedAU.getUserIdentifier() + " (type: " + consumedAU.getAuthenticatedUserLookup().getAuthenticationProviderId() + " | persistentUserId: "
                + consumedAU.getAuthenticatedUserLookup().getPersistentUserId() +
                "; Name: "+ consumedAU.getFirstName() + " " + consumedAU.getLastName() +"; Institution: "  + consumedAU.getAffiliation() + "; Email: " + consumedAU.getEmail() + ") merged into " +ongoingAU.getUserIdentifier();
    }
    
}
