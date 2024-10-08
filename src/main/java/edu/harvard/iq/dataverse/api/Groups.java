package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.IpGroup;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.IpGroupProvider;
import edu.harvard.iq.dataverse.authorization.groups.impl.maildomain.MailDomainGroup;
import edu.harvard.iq.dataverse.authorization.groups.impl.maildomain.MailDomainGroupProvider;
import edu.harvard.iq.dataverse.authorization.groups.impl.shib.ShibGroup;
import edu.harvard.iq.dataverse.authorization.groups.impl.shib.ShibGroupProvider;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import edu.harvard.iq.dataverse.util.json.JsonParser;

import jakarta.ejb.Stateless;
import jakarta.interceptor.Interceptors;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.*;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import jakarta.annotation.PostConstruct;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.PathParam;
import static org.apache.commons.lang3.StringUtils.isNumeric;

/**
 *
 * @author michael
 */
@Path("admin/groups")
@Stateless
public class Groups extends AbstractApiBean {
    private static final Logger logger = Logger.getLogger(Groups.class.getName());

    private IpGroupProvider ipGroupPrv;
    private ShibGroupProvider shibGroupPrv;
    private MailDomainGroupProvider mailDomainGroupPrv;

    Pattern legalGroupName = Pattern.compile("^[-_a-zA-Z0-9]+$");

    @PostConstruct
    void postConstruct() {
        ipGroupPrv = groupSvc.getIpGroupProvider();
        shibGroupPrv = groupSvc.getShibGroupProvider();
        mailDomainGroupPrv = groupSvc.getMailDomainGroupProvider();
    }

    /**
     * Creates a new {@link IpGroup}. The name of the group is based on the
     * {@code alias:} field, but might be changed to ensure uniqueness.
     * @param dto
     * @return Response describing the created group or the error that prevented
     *         that group from being created.
     */
    @POST
    @Path("ip")
    public Response postIpGroup( JsonObject dto ){
        try {
           IpGroup grp = new JsonParser().parseIpGroup(dto);
            grp.setGroupProvider( ipGroupPrv );
            grp.setPersistedGroupAlias(
                    ipGroupPrv.findAvailableName(
                            grp.getPersistedGroupAlias()==null ? "ipGroup" : grp.getPersistedGroupAlias()));

            grp = ipGroupPrv.store(grp);
            return created("/groups/ip/" + grp.getPersistedGroupAlias(), json(grp) );

        } catch ( Exception e ) {
            logger.log( Level.WARNING, "Error while storing a new IP group: " + e.getMessage(), e);
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Error: " + e.getMessage() );

        }
    }

    /**
     * Creates or updates the {@link IpGroup} named {@code groupName}.
     * @param groupName Name of the group.
     * @param dto data of the group.
     * @return Response describing the created group or the error that prevented
     *         that group from being created.
     */
    @PUT
    @Path("ip/{group}")
    public Response putIpGroups( @PathParam("group") String groupName, JsonObject dto ){
        try {
            if ( groupName == null || groupName.trim().isEmpty() ) {
                return badRequest("Group name cannot be empty");
            }
            if ( ! legalGroupName.matcher(groupName).matches() ) {
                return badRequest("Group name can contain only letters, digits, and the chars '-' and '_'");
            }
            IpGroup grp = new JsonParser().parseIpGroup(dto);
            grp.setGroupProvider( ipGroupPrv );
            grp.setPersistedGroupAlias( groupName );
            grp = ipGroupPrv.store(grp);
            return created("/groups/ip/" + grp.getPersistedGroupAlias(), json(grp) );

        } catch ( Exception e ) {
            logger.log( Level.WARNING, "Error while storing a new IP group: " + e.getMessage(), e);
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Error: " + e.getMessage() );

        }
    }

    @GET
    @Path("ip")
    public Response listIpGroups() {
        return ok( ipGroupPrv.findGlobalGroups()
                             .stream().map(g->json(g)).collect(toJsonArray()) );
    }

    @GET
    @Path("ip/{group}")
    public Response getIpGroup( @PathParam("group") String groupIdtf ) {
        IpGroup grp;
        if ( isNumeric(groupIdtf) ) {
            grp = ipGroupPrv.get( Long.parseLong(groupIdtf) );
        } else {
            grp = ipGroupPrv.get(groupIdtf);
        }

        return (grp == null) ? notFound( "Group " + groupIdtf + " not found") : ok(json(grp));
    }

    @DELETE
    @Path("ip/{group}")
    public Response deleteIpGroup( @PathParam("group") String groupIdtf ) {
        IpGroup grp;
        if ( isNumeric(groupIdtf) ) {
            grp = ipGroupPrv.get( Long.parseLong(groupIdtf) );
        } else {
            grp = ipGroupPrv.get(groupIdtf);
        }

        if (grp == null) return notFound( "Group " + groupIdtf + " not found");

        try {
            ipGroupPrv.deleteGroup(grp);
            return ok("Group " + grp.getAlias() + " deleted.");
        } catch ( Exception topExp ) {
            // get to the cause (unwraps EJB exception wrappers).
            Throwable e = topExp;
            while ( e.getCause() != null ) {
                e = e.getCause();
            }

            if ( e instanceof IllegalArgumentException ) {
                return error(Response.Status.BAD_REQUEST, e.getMessage());
            } else {
                throw topExp;
            }
        }
    }

    @GET
    @Path("shib")
    public Response listShibGroups() {
        JsonArrayBuilder arrBld = Json.createArrayBuilder();
        for (ShibGroup g : shibGroupPrv.findGlobalGroups()) {
            arrBld.add(json(g));
        }
        return ok(arrBld);
    }

    @POST
    @Path("shib")
    public Response createShibGroup(JsonObject shibGroupInput) {
        String expectedNameKey = "name";
        JsonString name = shibGroupInput.getJsonString(expectedNameKey);
        if (name == null) {
            return error(Response.Status.BAD_REQUEST, "required field missing: " + expectedNameKey);
        }
        String expectedAttributeKey = "attribute";
        JsonString attribute = shibGroupInput.getJsonString(expectedAttributeKey);
        if (attribute == null) {
            return error(Response.Status.BAD_REQUEST, "required field missing: " + expectedAttributeKey);
        }
        String expectedPatternKey = "pattern";
        JsonString pattern = shibGroupInput.getJsonString(expectedPatternKey);
        if (pattern == null) {
            return error(Response.Status.BAD_REQUEST, "required field missing: " + expectedPatternKey);
        }
        ShibGroup shibGroupToPersist = new ShibGroup(name.getString(), attribute.getString(), pattern.getString(), shibGroupPrv);
        ShibGroup persitedShibGroup = shibGroupPrv.persist(shibGroupToPersist);
        if (persitedShibGroup != null) {
            return ok("Shibboleth group persisted: " + persitedShibGroup);
        } else {
            return error(Response.Status.BAD_REQUEST, "Could not persist Shibboleth group");
        }
    }

    @DELETE
    @Path("shib/{primaryKey}")
    public Response deleteShibGroup( @PathParam("primaryKey") String id ) {
        ShibGroup doomed = shibGroupPrv.get(id);
        if (doomed != null) {
            boolean deleted;
            try {
                deleted = shibGroupPrv.delete(doomed);
            } catch (Exception ex) {
                return error(Response.Status.BAD_REQUEST, ex.getMessage());
            }
            if (deleted) {
                return ok("Shibboleth group " + id + " deleted");
            } else {
                return error(Response.Status.BAD_REQUEST, "Could not delete Shibboleth group with an id of " + id);
            }
        } else {
            return error(Response.Status.BAD_REQUEST, "Could not find Shibboleth group with an id of " + id);
        }
    }
    
    
    /**
     * Creates a new {@link MailDomainGroup}. The name of the group is based on the
     * {@code alias:} field, but might be changed to ensure uniqueness.
     * @param dto
     * @return Response describing the created group or the error that prevented
     *         that group from being created.
     */
    @POST
    @Path("domain")
    public Response createMailDomainGroup(JsonObject dto) throws JsonParseException {
        MailDomainGroup grp = new JsonParser().parseMailDomainGroup(dto);
        mailDomainGroupPrv.saveOrUpdate(Optional.empty(), grp);
        mailDomainGroupPrv.updateGroups();

        return created("/groups/domain/" + grp.getPersistedGroupAlias(), json(grp) );
    }
    
    /**
     * Creates or updates the {@link MailDomainGroup} named {@code groupName}.
     * @param groupAlias Name of the group.
     * @param dto data of the group.
     * @return Response describing the created group or the error that prevented
     *         that group from being created.
     */
    @PUT
    @Path("domain/{groupAlias}")
    public Response updateMailDomainGroups(@PathParam("groupAlias") String groupAlias, JsonObject dto) throws JsonParseException {
        if ( groupAlias == null || groupAlias.trim().isEmpty() ) {
            return badRequest("Group name cannot be empty");
        }
        if ( ! legalGroupName.matcher(groupAlias).matches() ) {
            return badRequest("Group name can contain only letters, digits, and the chars '-' and '_'");
        }
        
        MailDomainGroup grp = new JsonParser().parseMailDomainGroup(dto);
        mailDomainGroupPrv.saveOrUpdate(Optional.of(groupAlias), grp);
        mailDomainGroupPrv.updateGroups();
        
        return created("/groups/domain/" + grp.getPersistedGroupAlias(), json(grp) );
    }
    
    @GET
    @Path("domain")
    public Response listMailDomainGroups() {
        return ok( mailDomainGroupPrv.findGlobalGroups()
            .stream().map(g->json(g)).collect(toJsonArray()) );
    }
    
    @GET
    @Path("domain/{groupAlias}")
    public Response getMailDomainGroup(@PathParam("groupAlias") String groupAlias) {
        MailDomainGroup grp = mailDomainGroupPrv.get(groupAlias);
        return (grp == null) ? notFound( "Group " + groupAlias + " not found") : ok(json(grp));
    }
    
    @DELETE
    @Path("domain/{groupAlias}")
    public Response deleteMailDomainGroup(@PathParam("groupAlias") String groupAlias) {
        mailDomainGroupPrv.delete(groupAlias);
        mailDomainGroupPrv.updateGroups();
        return ok("Group " + groupAlias + " deleted.");
    }

}
