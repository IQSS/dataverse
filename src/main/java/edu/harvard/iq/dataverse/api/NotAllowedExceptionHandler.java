import javax.json.Json;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class NotAllowedExceptionHandler implements ExceptionMapper<NotAllowedException>{

    @Context
    HttpServletRequest request;
    
    @Override
    public Response toResponse(NotAllowedException ex){
        String uri = request.getRequestURI();
        return Response.status(405)
                .entity( Json.createObjectBuilder()
                             .add("status", "ERROR")
                             .add("code", 405)
                             .add("message", "'" + uri + "' endpoint does not support method '"+request.getMethod()+"'. Consult our API guide at http://guides.dataverse.org.")
                        .build())
                .type("application/json").build();
        
       
    }
    
}
