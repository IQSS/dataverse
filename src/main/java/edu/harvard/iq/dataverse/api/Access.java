/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileServiceBean;

import java.lang.reflect.Type;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import javax.servlet.http.HttpServletResponse;

/*
    Custom API exceptions [NOT YET IMPLEMENTED]
import edu.harvard.iq.dataverse.api.exceptions.NotFoundException;
import edu.harvard.iq.dataverse.api.exceptions.ServiceUnavailableException;
import edu.harvard.iq.dataverse.api.exceptions.PermissionDeniedException;
import edu.harvard.iq.dataverse.api.exceptions.AuthorizationRequiredException;
*/

/**
 *
 * @author Leonid Andreev
 * 
 * The data (file) access API is based on the DVN access API v.1.0 (that came 
 * with the v.3.* of the DVN app) and extended for DVN 4.0 to include some
 * extra fancy functionality, such as subsetting individual columns in tabular
 * data files and more.
 */

@Path("access")
public class Access {
    private static final Logger logger = Logger.getLogger(Meta.class.getCanonicalName());
    
    @EJB
    DataFileServiceBean dataFileService;

    @EJB
    
    @Path("datafile/{fileId}")
    @GET
    @Produces({ "application/xml" })
    public DownloadInstance datafile(@PathParam("fileId") Long fileId, @Context HttpHeaders header, @Context HttpServletResponse response) /*throws NotFoundException, ServiceUnavailableException, PermissionDeniedException, AuthorizationRequiredException*/ {
        String retValue = "";
        
        ByteArrayOutputStream outStream = null;
        

        DataFile df = dataFileService.find(fileId);
        /* TODO: 
         * Throw a meaningful exception if file not found!
         * -- L.A. 4.0alpha1
         */
        DownloadInfo dInfo = new DownloadInfo(df);
        DownloadInstance downloadInstance = new DownloadInstance(dInfo);
        
        
        /* 
         * Provide content type header:
         * (this should be done by the InstanceWriter class - ?)
         */
         
        /* Provide "Access-Control-Allow-Origin" header:
         * (may not be needed here... - that header was added specifically
         * to get the data exploration app to be able to access the metadata
         * API; may have been something specific to Vito's installation too
         * -- L.A.)
         */
        response.setHeader("Access-Control-Allow-Origin", "*");
                
        /* 
         * Provide some browser-friendly headers: (?)
         */
        //return retValue; 
        return downloadInstance;
    }
    /*
    public class DownloadInstance {
        private ByteArrayOutputStream outStream = null;
        
        public ByteArrayOutputStream getOutStream() {
            return outStream;
        } 
        
        public void setOutStream(ByteArrayOutputStream outStream) {
            this.outStream=outStream;
        }
    }
    */
    
    /*
    @Singleton
    @Provider
    public class DownloadInstanceWriter implements MessageBodyWriter<DownloadInstance> {

        public boolean isWriteable(Class<?> clazz, Type type, Annotation[] annotation, MediaType mediaType) {
            return clazz == DownloadInstance.class;
        }

        public long getSize(DownloadInstance di, Class<?> clazz, Type type, Annotation[] annotation, MediaType mediaType) {
            return -1;
        }

        public void writeTo(DownloadInstance di, Class<?> clazz, Type type, Annotation[] annotation, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream outstream) throws IOException, WebApplicationException {

            ByteArrayOutputStream generatedStream = di.getOutStream();
            byte[] generatedBytes = generatedStream.toByteArray();

            outstream.write(generatedBytes, 0, generatedBytes.length);
            // in prod. we'll want to use the 
            // outstream.write(byte[], offset, lenght) version
            //
            // do i need to close outstream?
        }
    
    }
    */
    

}