package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.api.annotations.ApiWriteOperation;
import edu.harvard.iq.dataverse.api.dto.IpGroupDTO;
import edu.harvard.iq.dataverse.api.dto.SamlGroupDTO;
import edu.harvard.iq.dataverse.authorization.groups.GroupServiceBean;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.IpGroupProvider;
import edu.harvard.iq.dataverse.authorization.groups.impl.saml.SamlGroupProvider;
import edu.harvard.iq.dataverse.persistence.group.IpGroup;
import edu.harvard.iq.dataverse.persistence.group.SamlGroup;
import edu.harvard.iq.dataverse.util.json.JsonParser;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.commons.lang.StringUtils.isNumeric;

/**
 * @author michael
 */
@Path("admin/groups")
@Stateless
public class Groups extends AbstractApiBean {
    private static final Logger logger = Logger.getLogger(Groups.class.getName());

    private IpGroupProvider ipGroupPrv;
    private SamlGroupProvider samlGroupProvider;

    @Inject
    private GroupServiceBean groupSvc;

    Pattern legalGroupName = Pattern.compile("^[-_a-zA-Z0-9]+$");

    @PostConstruct
    void postConstruct() {
        ipGroupPrv = groupSvc.getIpGroupProvider();
        samlGroupProvider = groupSvc.getSamlGroupProvider();
    }

    /**
     * Creates a new {@link IpGroup}. The name of the group is based on the
     * {@code alias:} field, but might be changed to ensure uniqueness.
     *
     * @param dto
     * @return Response describing the created group or the error that prevented
     * that group from being created.
     */
    @POST
    @ApiWriteOperation
    @Path("ip")
    public Response postIpGroup(JsonObject dto) {
        try {
            IpGroup grp = new JsonParser().parseIpGroup(dto);
            grp.setPersistedGroupAlias(
                    ipGroupPrv.findAvailableName(
                            grp.getPersistedGroupAlias() == null ? "ipGroup" : grp.getPersistedGroupAlias()));

            grp = ipGroupPrv.store(grp);
            return created("/groups/ip/" + grp.getPersistedGroupAlias(), new IpGroupDTO.Converter().convert(grp));

        } catch (Exception e) {
            logger.log(Level.WARNING, "Error while storing a new IP group: " + e.getMessage(), e);
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Error: " + e.getMessage());

        }
    }

    /**
     * Creates or updates the {@link IpGroup} named {@code groupName}.
     *
     * @param groupName Name of the group.
     * @param dto       data of the group.
     * @return Response describing the created group or the error that prevented
     * that group from being created.
     */
    @PUT
    @ApiWriteOperation
    @Path("ip/{groupName}")
    public Response putIpGroups(@PathParam("groupName") String groupName, JsonObject dto) {
        try {
            if (groupName == null || groupName.trim().isEmpty()) {
                return badRequest("Group name cannot be empty");
            }
            if (!legalGroupName.matcher(groupName).matches()) {
                return badRequest("Group name can contain only letters, digits, and the chars '-' and '_'");
            }
            IpGroup grp = new JsonParser().parseIpGroup(dto);
            grp.setPersistedGroupAlias(groupName);
            grp = ipGroupPrv.store(grp);
            return created("/groups/ip/" + grp.getPersistedGroupAlias(), new IpGroupDTO.Converter().convert(grp));

        } catch (Exception e) {
            logger.log(Level.WARNING, "Error while storing a new IP group: " + e.getMessage(), e);
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Error: " + e.getMessage());

        }
    }

    @GET
    @Path("ip")
    public Response listIpGroups() {
        return ok(ipGroupPrv.findGlobalGroups().stream()
                .map(g -> new IpGroupDTO.Converter().convert(g))
                .collect(Collectors.toList()));
    }

    @GET
    @Path("ip/{groupIdtf}")
    public Response getIpGroup(@PathParam("groupIdtf") String groupIdtf) {
        IpGroup grp = isNumeric(groupIdtf)
                ? ipGroupPrv.get(Long.parseLong(groupIdtf))
                : ipGroupPrv.get(groupIdtf);

        return (grp == null)
                ? notFound("Group " + groupIdtf + " not found")
                : ok(new IpGroupDTO.Converter().convert(grp));
    }

    @DELETE
    @ApiWriteOperation
    @Path("ip/{groupIdtf}")
    public Response deleteIpGroup(@PathParam("groupIdtf") String groupIdtf) {
        IpGroup grp = isNumeric(groupIdtf)
                ? ipGroupPrv.get(Long.parseLong(groupIdtf))
                : ipGroupPrv.get(groupIdtf);

        if (grp == null) {
            return notFound("Group " + groupIdtf + " not found");
        }

        try {
            ipGroupPrv.deleteGroup(grp);
            return ok("Group " + grp.getAlias() + " deleted.");
        } catch (Exception topExp) {
            // get to the cause (unwraps EJB exception wrappers).
            Throwable e = topExp;
            while (e.getCause() != null) {
                e = e.getCause();
            }

            if (e instanceof IllegalArgumentException) {
                return error(Response.Status.BAD_REQUEST, e.getMessage());
            } else {
                throw topExp;
            }
        }
    }

    @GET
    @Path("saml")
    public Response listSamlGroups() {
        List<SamlGroupDTO> samlGroups = samlGroupProvider.findGlobalGroups().stream()
                .map(g -> new SamlGroupDTO.Converter().convert(g))
                .collect(Collectors.toList());
        return ok(samlGroups);
    }

    @POST
    @ApiWriteOperation
    @Path("saml")
    public Response createSamlGroup(JsonObject samlGroupInput) {
        String expectedNameKey = "name";
        JsonString name = samlGroupInput.getJsonString(expectedNameKey);
        if (name == null) {
            return error(Response.Status.BAD_REQUEST, "required field missing: " + expectedNameKey);
        }
        String expectedEntityIdKey = "entityId";
        JsonString entityId = samlGroupInput.getJsonString(expectedEntityIdKey);
        if (entityId == null) {
            return error(Response.Status.BAD_REQUEST, "required field missing: " + expectedEntityIdKey);
        }
        SamlGroup persitedSamlGroup = samlGroupProvider.persist(new SamlGroup(name.getString(), entityId.getString()));
        return persitedSamlGroup != null
                ? ok("Saml group persisted: " + persitedSamlGroup)
                : error(Response.Status.BAD_REQUEST, "Could not persist Saml group");
    }

    @DELETE
    @ApiWriteOperation
    @Path("saml/{primaryKey}")
    public Response deleteSamlGroup(@PathParam("primaryKey") String id) {
        SamlGroup doomed = samlGroupProvider.get(id);
        if (doomed != null) {
            try {
                samlGroupProvider.delete(doomed);
                return ok("Saml group " + id + " deleted");
            } catch (IllegalArgumentException iae) {
                return error(Response.Status.BAD_REQUEST, iae.getMessage());
            }
        } else {
            return notFound("Could not find Saml group with an id of " + id);
        }
    }
}
