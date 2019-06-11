package edu.harvard.iq.dataverse.license;

import edu.harvard.iq.dataverse.util.jpa.LocaleConverter;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;

/**
 * JPA embeddable model class representing some
 * text with assigned locale.
 * 
 * @author madryk
 */
@Embeddable
public class LocaleText implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(nullable=false)
    @Convert(converter=LocaleConverter.class)
    private Locale locale;

    
    @Column(nullable=false)
    private String text;
    
    
    //-------------------- CONSTRUCTORS --------------------
    
    @SuppressWarnings("unused") /** for jpa only **/
    protected LocaleText() {
    }
    
    
    public LocaleText(Locale locale, String text) {
        this.locale = Objects.requireNonNull(locale);
        this.text = Objects.requireNonNull(text);
    }
    
    //-------------------- GETTERS --------------------
    
    /**
     * Returns locale of text
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * Returns localized text
     */
    public String getText() {
        return text;
    }

}
