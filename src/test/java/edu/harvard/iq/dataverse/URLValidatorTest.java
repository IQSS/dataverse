
package edu.harvard.iq.dataverse;

import static org.junit.Assert.assertEquals;

import javax.validation.ConstraintValidatorContext;

import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.hibernate.validator.internal.engine.path.PathImpl;
//import org.hibernate.validator.internal.engine.time.DefaultTimeProvider;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import javax.validation.ClockProvider;
import org.junit.Test;

/**
 *
 * @author skraffmi
 */
public class URLValidatorTest {
    //static DefaultTimeProvider timeProvider = DefaultTimeProvider.getInstance();
    ValidatorFactory vFac = Validation.buildDefaultValidatorFactory();
    

    @Test
    public void testIsURLValid() {
        assertEquals(true, URLValidator.isURLValid(null));
        assertEquals(true, URLValidator.isURLValid(""));
        assertEquals(true, URLValidator.isURLValid("https://twitter.com/"));

        assertEquals(false, URLValidator.isURLValid("cnn.com"));

    }

    @Test
    public void testIsValidWithUnspecifiedContext() {
        String value = "https://twitter.com/";
        ConstraintValidatorContext context = null;
        assertEquals(true, new URLValidator().isValid(value, context));
    }

    @Test
    public void testIsValidWithContextAndValidURL() {
        String value = "https://twitter.com/";
        ConstraintValidatorContext context = new ConstraintValidatorContextImpl(vFac.getClockProvider(), PathImpl.createPathFromString(""),null, null);

        assertEquals(true, new URLValidator().isValid(value, context));
    }

    @Test
    public void testIsValidWithContextButInvalidURL() {
        String value = "cnn.com";
        ConstraintValidatorContext context = new ConstraintValidatorContextImpl(vFac.getClockProvider(), PathImpl.createPathFromString(""),null, null);

        assertEquals(false, new URLValidator().isValid(value, context));
    }

}
