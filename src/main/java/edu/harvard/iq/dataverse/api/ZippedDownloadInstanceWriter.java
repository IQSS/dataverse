/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.api;

import java.lang.reflect.Type;
import java.lang.annotation.Annotation;
import java.io.OutputStream;
import java.io.IOException;

import javax.ws.rs.WebApplicationException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import edu.harvard.iq.dataverse.dataaccess.*;
import java.util.logging.Logger;

/**
 *
 * @author Leonid Andreev
 */
@Provider
public class ZippedDownloadInstanceWriter implements MessageBodyWriter<ZippedDownloadInstance> {
    
    private static final Logger logger = Logger.getLogger(ZippedDownloadInstanceWriter.class.getCanonicalName());

    
    @Override
    public boolean isWriteable(Class<?> clazz, Type type, Annotation[] annotation, MediaType mediaType) {
        return clazz == ZippedDownloadInstance.class;
    }

    @Override
    public long getSize(ZippedDownloadInstance di, Class<?> clazz, Type type, Annotation[] annotation, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(ZippedDownloadInstance di, Class<?> clazz, Type type, Annotation[] annotation, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream outstream) throws IOException, WebApplicationException {

        if (di.getDataFiles() != null && di.getDataFiles().size() > 0) {

            httpHeaders.add("Content-disposition", "attachment; filename=\"dataverse_files.zip\"");
            httpHeaders.add("Content-Type", "application/zip; name=\"dataverse_files.zip\"");

            DataFileZipper zipper = new DataFileZipper();

            try {
                if (di.getSizeLimit() > 0) {
                    zipper.zipFiles(di.getDataFiles(), outstream, di.getManifest(), di.getSizeLimit());
                } else {
                    zipper.zipFiles(di.getDataFiles(), outstream, di.getManifest());
                }
            } catch (IOException ioe) {
                throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
            }

            return;
        }

        logger.warning("empty list of extra arguments.");
        throw new WebApplicationException(Response.Status.BAD_REQUEST);

    }

}
