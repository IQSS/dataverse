/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.trsa;

import edu.harvard.iq.dataverse.api.AbstractApiBean;
import edu.harvard.iq.dataverse.trsa.Trsa;
import edu.harvard.iq.dataverse.trsa.TrsaRegistryServiceBean;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 * @author asone
 */

@Path("admin/trsaRegistration")
public class TrsaRegistration extends AbstractApiBean {

    @EJB
    TrsaRegistryServiceBean trsaRegistryServiceBean;
    
    
    @POST
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public void create(Trsa entity) {
        trsaRegistryServiceBean.create(entity);
    }

    @PUT
    @Path("{id}")
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public void edit(@PathParam("id") Long id, Trsa entity) {
        trsaRegistryServiceBean.edit(entity);
    }

    @DELETE
    @Path("{id}")
    public void remove(@PathParam("id") Long id) {
        trsaRegistryServiceBean.remove(trsaRegistryServiceBean.find(id));
    }

    @GET
    @Path("{id}")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Trsa find(@PathParam("id") Long id) {
        return trsaRegistryServiceBean.find(id);
    }

    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response getTrsaRegistries() {
        JsonArrayBuilder jab = Json.createArrayBuilder();
        trsaRegistryServiceBean.findAll().forEach((trsaRegistry)->{
            jab.add(trsaRegistry.toJson());
        });
        return ok(jab);
    }
    
    
    
    

    @GET
    @Path("{from}/{to}")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public List<Trsa> findRange(@PathParam("from") Integer from, @PathParam("to") Integer to) {
        return trsaRegistryServiceBean.findRange(new int[]{from, to});
    }

    @GET
    @Path("count")
    @Produces(MediaType.TEXT_PLAIN)
    public String countREST() {
        return String.valueOf(trsaRegistryServiceBean.count());
    }

    
}
