package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.util.MockResponse;
import javax.ws.rs.core.Response;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author michael
 */
public class AbstractApiBeanTest {
    
    AbstractApiBeanImpl sut;
    
    @Before
    public void before() {
        sut = new AbstractApiBeanImpl();
    }
    
    @Test
    public void testIsNumeric() {
        assertTrue( sut.isNumeric("1") );
        assertTrue( sut.isNumeric("199999") );
        assertFalse( sut.isNumeric("a") );
    }

    @Test
    public void testParseBooleanOrDie_ok() throws Exception {
        assertTrue( sut.parseBooleanOrDie("1") );
        assertTrue( sut.parseBooleanOrDie("yes") );
        assertTrue( sut.parseBooleanOrDie("true") );
        assertFalse( sut.parseBooleanOrDie("false") );
        assertFalse( sut.parseBooleanOrDie("0") );
        assertFalse( sut.parseBooleanOrDie("no") );
    }
    
    @Test( expected=Exception.class )
    public void testParseBooleanOrDie_invalid() throws Exception {
        sut.parseBooleanOrDie("I'm not a boolean value!");
    }

    @Test
    public void testFailIfNull_ok() throws Exception {
        sut.failIfNull(sut, "");
    }
    
    @Test
    public void testAllowCors() {
        Response r = sut.allowCors(new MockResponse(200));
        assertEquals("*", r.getHeaderString("Access-Control-Allow-Origin"));
    }

    /** dummy implementation */
    public class AbstractApiBeanImpl extends AbstractApiBean {
        
    }
    
    
}
