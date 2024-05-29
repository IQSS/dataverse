package edu.harvard.iq.dataverse.openapi;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.*;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.ws.rs.core.*;
import org.apache.commons.io.IOUtils;
import edu.harvard.iq.dataverse.api.Info;
import edu.harvard.iq.dataverse.util.BundleUtil;

@WebServlet("/openapi")
public class OpenApi extends HttpServlet {

    private static final Logger logger = Logger.getLogger(Info.class.getCanonicalName());

    private static final String YAML_FORMAT = "yaml";
    private static final String JSON_FORMAT = "json";


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        
        
        String format = req.getParameter("format");
        String accept = req.getHeader("Accept");

        /*
         * We first check for the headers, if the request accepts application/json 
         * have to check for the format parameter, if it is different from json 
         * return BAD_REQUEST (400)
         */
        if (MediaType.APPLICATION_JSON.equals(accept)){
            if (format != null && !JSON_FORMAT.equals(format)){
                List<String> args = Arrays.asList(accept, format);
                String bundleResponse = BundleUtil.getStringFromBundle("openapi.exception.unaligned", args);
                resp.sendError(Response.Status.BAD_REQUEST.getStatusCode(),
                                bundleResponse);
                return;
            } else {
                format = JSON_FORMAT;
            }
        }

        /*
         * We currently support only JSON or YAML being the second the default
         * if no format is specified, if a different format is specified we return 
         * UNSUPPORTED_MEDIA_TYPE (415) specifying that the format is not supported
         */

        format = format == null ? YAML_FORMAT : format.toLowerCase();

        if (JSON_FORMAT.equals(format)) {
            resp.setContentType(MediaType.APPLICATION_JSON_TYPE.toString());
        } else  if (YAML_FORMAT.equals(format)){
            resp.setContentType(MediaType.TEXT_PLAIN_TYPE.toString());
        } else {
            
            List<String> args = Arrays.asList(format);
            String bundleResponse = BundleUtil.getStringFromBundle("openapi.exception.invalid.format", args);

            JsonObject errorResponse = Json.createObjectBuilder()
                .add("status", "ERROR")
                .add("code", HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE)
                .add("message", bundleResponse)
                .build();

            resp.setContentType(MediaType.APPLICATION_JSON_TYPE.toString());
            resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);

            PrintWriter responseWriter = resp.getWriter();
            responseWriter.println(errorResponse.toString());
            responseWriter.flush();
            return;
        }

        try {
            String baseFileName = "/META-INF/openapi." + format;
            ClassLoader classLoader = this.getClass().getClassLoader();
            URL aliasesResource = classLoader.getResource(baseFileName);
            InputStream openapiDefinitionStream  = aliasesResource.openStream();
            String content = IOUtils.toString(openapiDefinitionStream, StandardCharsets.UTF_8);
            resp.getWriter().write(content);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "OpenAPI Definition format not found " + format + ":" + e.getMessage(), e);
            String bundleResponse = BundleUtil.getStringFromBundle("openapi.exception");
            resp.sendError(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                            bundleResponse);
        }

        
    }
    
}
