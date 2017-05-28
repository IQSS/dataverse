package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.util.json.JsonParseException;
import edu.harvard.iq.dataverse.util.json.JsonParser;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.brief;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.json;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.toJsonArray;
import edu.harvard.iq.dataverse.workflow.Workflow;
import edu.harvard.iq.dataverse.workflow.WorkflowContext.TriggerType;
import edu.harvard.iq.dataverse.workflow.WorkflowServiceBean;
import edu.harvard.iq.dataverse.workflow.WorkflowStepSPI;
import edu.harvard.iq.dataverse.workflow.stepspi.WorkflowStepProviderInterface;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * API Endpoint for managing workflows.
 * @author michael
 */
@Path("admin/workflows")
public class WorkflowsAdmin extends AbstractApiBean {
      
    private static final Logger logger = Logger.getLogger(WorkflowsAdmin.class.getName());
       
    @EJB
    WorkflowServiceBean workflows;
    
    @Inject
    private javax.enterprise.inject.Instance<WorkflowStepProviderInterface> stepProviders;
    
    @POST
    public Response addWorkflow(JsonObject jsonWorkflow) {
        JsonParser jp = new JsonParser();
        try {
            Workflow wf = jp.parseWorkflow(jsonWorkflow);
            Workflow managedWf = workflows.save(wf);
            
            return created("/admin/workflows/"+managedWf.getId(), json(managedWf));
        } catch (JsonParseException ex) {
            return badRequest("Can't parse Json: " + ex.getMessage());
        }
    }
    
    @GET
    public Response listWorkflows() {
        return ok( workflows.listWorkflows().stream()
                            .map(wf->brief.json(wf)).collect(toJsonArray()) );
    }
    
    @Path("default/{triggerType}")
    @PUT
    public Response setDefault(@PathParam("triggerType") String triggerType, String identifier) {
        try {
            long idtf = Long.parseLong(identifier.trim());
            TriggerType tt = TriggerType.valueOf(triggerType);
            Optional<Workflow> wf = workflows.getWorkflow(idtf);
            if ( wf.isPresent() ) {
                workflows.setDefaultWorkflowId(tt, idtf);
                return ok("Default workflow id for trigger " + tt.name() + " set to " + idtf);
            } else {
                return notFound("Can't find workflow with id " + idtf);
            }
        } catch (NumberFormatException nfe) {
            return badRequest("workflow identifier has to be numeric.");
        } catch ( IllegalArgumentException iae ) {
            return badRequest("Unknown trigger type '" + triggerType + "'. Available triggers: " + Arrays.toString(TriggerType.values()) );
        }
    }
    
    @Path("default/")
    @GET
    public Response listDefaults() {
        JsonObjectBuilder bld = Json.createObjectBuilder();
        for ( TriggerType tp : TriggerType.values() ) {
            bld.add(tp.name(), 
                    workflows.getDefaultWorkflow(tp)
                             .map(wf->(JsonValue)brief.json(wf).build())
                             .orElse(JsonValue.NULL));
        }
        return ok(bld);
    }
    
    @Path("default/{triggerType}")
    @GET
    public Response getDefault(@PathParam("triggerType") String triggerType) {
        try {
            return workflows.getDefaultWorkflow(TriggerType.valueOf(triggerType))
                            .map( wf -> ok(json(wf)) )
                            .orElse( notFound("no default workflow") );
        } catch ( IllegalArgumentException iae ) {
            return badRequest("Unknown trigger type '" + triggerType + "'. Available triggers: " + Arrays.toString(TriggerType.values()) );
        }
    }
    
    @Path("default/{triggerType}")
    @DELETE
    public Response deleteDefault(@PathParam("triggerType") String triggerType) {
        try {
            workflows.setDefaultWorkflowId(TriggerType.valueOf(triggerType), null);
            return ok("default workflow for trigger " + triggerType + " unset.");
        } catch ( IllegalArgumentException iae ) {
            return badRequest("Unknown trigger type '" + triggerType + "'. Available triggers: " + Arrays.toString(TriggerType.values()) );
        }
    }
    
    @Path("/{identifier}")
    @GET
    public Response getWorkflow(@PathParam("identifier") String identifier ) {
        try {
            long idtf = Long.parseLong(identifier);
            return workflows.getWorkflow(idtf)
                            .map(wf->ok(json(wf)))
                            .orElse(notFound("Can't find workflow with id " + identifier));
        } catch (NumberFormatException nfe) {
            return badRequest("workflow identifier has to be numeric.");
        }
    }
    
    @Path("/{id}")
    @DELETE
    public Response deleteWorkflow(@PathParam("id") String id ) {
        try {
            long idtf = Long.parseLong(id);
            return workflows.deleteWorkflow(idtf) ? ok("Workflow " + idtf + " deleted") 
                                 : notFound("workflow with id " + idtf + " not found"); 
        } catch (NumberFormatException nfe) {
            return badRequest("workflow identifier has to be numeric.");
            
        } catch ( IllegalArgumentException e ) {
            return forbidden("Cannot delete the default workflow. Please change the default workflow and try again.");
            
        } catch ( Exception e ) {
            Throwable cc = e;
            while ( cc.getCause() != null ) {
                cc=cc.getCause();
            }
            if ( cc instanceof IllegalArgumentException ) {
                return forbidden("Cannot delete the default workflow. Please change the default workflow and try again.");
            } else {
                throw e;
            }
        }
    }
    
    
    @Path("/test-cp1")
    @GET
    public Response testCP1() {
        try {
            Class.forName("edu.harvard.iq.dataverse.workflow.WorkflowStepSPI");
            return ok("WorkflowStepSPI found using class.forName");
        } catch (ClassNotFoundException ex) {
            return ok("WorkflowStepSPI NOT found using class.forName");
        }
    }
    
    @Path("/test-cp")
    @GET
    public Response testCP() throws Exception {
        String path = "/Applications/NetBeans/glassfish-4.1/glassfish/domains/domain1/lib/DataverseWorkflowStepProvider.jar";
        URL jarUrl = new File(path).toURI().toURL();
        URLClassLoader loader = new URLClassLoader(new URL[]{jarUrl}, Thread.currentThread().getContextClassLoader());
        Class<?> clz = loader.loadClass("edu.harvard.iq.dataverse.samplestepprovider.SampleStepProvider");
        Object newInstance = clz.newInstance();
        return ok("Instantiated " + newInstance.toString() );
    }
    
    @Path("/test-cp2")
    @GET
    public Response testCP2() throws MalformedURLException {
        String path = "/Applications/NetBeans/glassfish-4.1/glassfish/domains/domain1/lib/DataverseWorkflowStepProvider.jar";
        URL jarUrl = new File(path).toURI().toURL();
        URLClassLoader loader = new URLClassLoader(new URL[]{jarUrl}, Thread.currentThread().getContextClassLoader());
        ServiceLoader<WorkflowStepSPI> sl = ServiceLoader.load(WorkflowStepSPI.class, loader);
        Iterator<WorkflowStepSPI> iterator = sl.iterator();
        if ( iterator.hasNext() ) {
            WorkflowStepSPI wss = iterator.next();
            return ok("Loaded " + wss.toString() );
        } else {
            return ok("No services found");
        }
    }
    
    @Path("/test-spi")
    @GET
    public Response testSpi() {
        try {
            ServiceLoader<WorkflowStepSPI> loader = ServiceLoader.load(WorkflowStepSPI.class, Thread.currentThread().getContextClassLoader());
            loader.reload();
            List<String> names = new LinkedList<>();
            Iterator<WorkflowStepSPI> itr = loader.iterator();
            while ( itr.hasNext() ) {
                WorkflowStepSPI wss = itr.next();
                logger.log(Level.INFO, "Found WorkflowStepProvider: {0}", wss.getClass().getCanonicalName());
                names.add(wss.toString());
            }
            logger.log(Level.INFO, "Searching for Workflow Step Providers done.");
            return ok( names.toString() );
        } catch (NoClassDefFoundError ncdfe) {
            logger.log(Level.WARNING, "Class not found: " + ncdfe.getMessage(), ncdfe);
            return error( Status.INTERNAL_SERVER_ERROR, ncdfe.getLocalizedMessage() );
        } catch (ServiceConfigurationError serviceError) {
            logger.log(Level.WARNING, "Service Error loading workflow step providers: " + serviceError.getMessage(), serviceError);
            return error( Status.INTERNAL_SERVER_ERROR, serviceError.getLocalizedMessage() );
        }
    }
    
    
    @Path("/test-spi-i")
    @GET
    public Response testSpiI() {
        try {
            ServiceLoader<WorkflowStepProviderInterface> loader = ServiceLoader.load(WorkflowStepProviderInterface.class, Thread.currentThread().getContextClassLoader());
            loader.reload();
            List<String> names = new LinkedList<>();
            Iterator<WorkflowStepProviderInterface> itr = loader.iterator();
            while ( itr.hasNext() ) {
                WorkflowStepProviderInterface wss = itr.next();
                logger.log(Level.INFO, "Found WorkflowStepProviderInterface: {0}", wss.getClass().getCanonicalName());
                logger.log(Level.INFO, "Got string: {0}", wss.getAString() );
            }
            logger.log(Level.INFO, "Searching for WorkflowStepProviderInterface done.");
            return ok( names.toString() );
        } catch (NoClassDefFoundError ncdfe) {
            logger.log(Level.WARNING, "Class not found: " + ncdfe.getMessage(), ncdfe);
            return error( Status.INTERNAL_SERVER_ERROR, ncdfe.getLocalizedMessage() );
        } catch (ServiceConfigurationError serviceError) {
            logger.log(Level.WARNING, "Service Error loading workflow step providers: " + serviceError.getMessage(), serviceError);
            return error( Status.INTERNAL_SERVER_ERROR, serviceError.getLocalizedMessage() );
        }
    }
    
    @Path("/test-spi-i2")
    @GET
    public Response testSpiI2() {
        try {
            Iterator<WorkflowStepProviderInterface> spiItr = stepProviders.iterator();
            StringBuilder sb = new StringBuilder();
            
            while ( spiItr.hasNext() ) {
                WorkflowStepProviderInterface instance = spiItr.next();
                logger.info( "found intance " + instance.toString() );
                sb.append( instance.getAString() ).append("\n");
            }
            
            return ok( sb.toString() );
        } catch (NoClassDefFoundError ncdfe) {
            logger.log(Level.WARNING, "Class not found: " + ncdfe.getMessage(), ncdfe);
            return error( Status.INTERNAL_SERVER_ERROR, ncdfe.getLocalizedMessage() );
        } catch (ServiceConfigurationError serviceError) {
            logger.log(Level.WARNING, "Service Error loading workflow step providers: " + serviceError.getMessage(), serviceError);
            return error( Status.INTERNAL_SERVER_ERROR, serviceError.getLocalizedMessage() );
        }
    }
}
