package edu.harvard.iq.dataverse.util;

import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import static java.util.stream.Collectors.toList;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

/**
 * Simple mock class for HTTP response. This is needed as the normal response builders
 * require JAX-RS system initializations.
 * 
 * @author michael
 */
public class MockResponse extends Response {
    
    private final int status;
    private final Object entity;
    private final MultivaluedMap<String,Object> headers = new MultivaluedHashMap<>();
    
    public MockResponse( int aStatus, Object anEntity ) {
        status = aStatus;
        entity = anEntity;
    }
    
    public MockResponse( int aStatus ){
        this( aStatus, null );
    }
    
    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public StatusType getStatusInfo() {
        throw new UnsupportedOperationException("Not supported yet."); // TODO: Implement getEntity
    }

    @Override
    public Object getEntity() {
        return entity;
    }

    @Override
    public <T> T readEntity(Class<T> entityType) {
        return (T)entity;
    }

    @Override
    public <T> T readEntity(GenericType<T> entityType) {
        return (T)entity;
    }

    @Override
    public <T> T readEntity(Class<T> entityType, Annotation[] annotations) {
        return (T)entity;
    }

    @Override
    public <T> T readEntity(GenericType<T> entityType, Annotation[] annotations) {
        return (T)entity;
    }

    @Override
    public boolean hasEntity() {
        return (entity!=null);
    }

    @Override
    public boolean bufferEntity() {
        throw new UnsupportedOperationException("Not supported yet."); // TODO: Implement bufferEntity
    }

    @Override
    public void close() {
    }

    @Override
    public MediaType getMediaType() {
        throw new UnsupportedOperationException("Not supported yet."); // TODO: Implement getMediaType
    }

    @Override
    public Locale getLanguage() {
        throw new UnsupportedOperationException("Not supported yet."); // TODO: Implement getLanguage
    }

    @Override
    public int getLength() {
        throw new UnsupportedOperationException("Not supported yet."); // TODO: Implement getAllowedMethods
    }

    @Override
    public Set<String> getAllowedMethods() {
        throw new UnsupportedOperationException("Not supported yet."); // TODO: Implement getAllowedMethods
    }

    @Override
    public Map<String, NewCookie> getCookies() {
        throw new UnsupportedOperationException("Not supported yet."); // TODO: Implement getCookies
    }

    @Override
    public EntityTag getEntityTag() {
        throw new UnsupportedOperationException("Not supported yet."); // TODO: Implement getEntityTag
    }

    @Override
    public Date getDate() {
        throw new UnsupportedOperationException("Not supported yet."); // TODO: Implement getDate
    }

    @Override
    public Date getLastModified() {
        throw new UnsupportedOperationException("Not supported yet."); // TODO: Implement getLastModified
    }

    @Override
    public URI getLocation() {
        throw new UnsupportedOperationException("Not supported yet."); // TODO: Implement getLocation
    }

    @Override
    public Set<Link> getLinks() {
        throw new UnsupportedOperationException("Not supported yet."); // TODO: Implement getLinks
    }

    @Override
    public boolean hasLink(String relation) {
        throw new UnsupportedOperationException("Not supported yet."); // TODO: Implement hasLink
    }

    @Override
    public Link getLink(String relation) {
        throw new UnsupportedOperationException("Not supported yet."); // TODO: Implement getLink
    }

    @Override
    public Link.Builder getLinkBuilder(String relation) {
        throw new UnsupportedOperationException("Not supported yet."); // TODO: Implement getLinkBuilder
    }

    @Override
    public MultivaluedMap<String, Object> getMetadata() {
        return headers;
    }

    @Override
    public MultivaluedMap<String, String> getStringHeaders() {
        MultivaluedMap<String,String> retVal = new MultivaluedHashMap<>();
        headers.entrySet().forEach(e -> {
            retVal.put(e.getKey(), e.getValue().stream().map(Object::toString).collect(toList()));
        });
        return retVal;
    }

    @Override
    public String getHeaderString(String name) {
        return headers.getFirst(name).toString();
    }
    
}
