package edu.harvard.iq.dataverse.util.json;

import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.api.Util;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;

/**
 * A JSON builder that drops any null values. If we didn't drop'em,
 * we'd get an NPE from the standard JSON builder. But just omitting them
 * makes sense. So there.
 * 
 * @author michael
 */
public class NullSafeJsonBuilder implements JsonObjectBuilder {
	
	public static NullSafeJsonBuilder jsonObjectBuilder() {
		return new NullSafeJsonBuilder();
	}
	
	private final JsonObjectBuilder delegate;
	
	public NullSafeJsonBuilder() {
		delegate = Json.createObjectBuilder();
	}

	@Override
	public NullSafeJsonBuilder add(String name, JsonValue value) {
		if ( value!=null ) delegate.add(name, value);
		return this;
	}

	@Override
	public NullSafeJsonBuilder add(String name, String value) {
		if ( value!=null ) 
			 delegate.add(name, value);
		return this;
	}

	@Override
	public NullSafeJsonBuilder add(String name, BigInteger value) {
		if ( value!=null ) 
			 delegate.add(name, value);
		return this;
	}

	@Override
	public NullSafeJsonBuilder add(String name, BigDecimal value) {
		if ( value!=null ) 
			delegate.add(name, value);
		
		return this;
	}

	@Override
	public NullSafeJsonBuilder add(String name, int value) {
		delegate.add(name, value);
		return this;
	}

	public NullSafeJsonBuilder add(String name, Long value) {
        return ( value != null ) ? add(name, value.longValue()) : this;
    }
    
	@Override
	public NullSafeJsonBuilder add(String name, long value) {
		delegate.add(name, value);
		return this;
	}

	@Override
	public NullSafeJsonBuilder add(String name, double value) {
		delegate.add(name, value);
		return this;
	}

	@Override
	public NullSafeJsonBuilder add(String name, boolean value) {
		delegate.add(name, value);
		return this;
	}
    
	@Override
	public NullSafeJsonBuilder addNull(String name) {
		delegate.addNull(name);
		return this;
	}

	@Override
	public NullSafeJsonBuilder add(String name, JsonObjectBuilder builder) {
		if ( builder!=null ) 
			 delegate.add(name, builder);
		return this;
	}

	@Override
	public NullSafeJsonBuilder add(String name, JsonArrayBuilder builder) {
		if ( builder!=null ) 
			delegate.add(name, builder);
		return this;
	}
	
	public NullSafeJsonBuilder addStrValue( String name, DatasetField field ) {
		if ( field != null ) {
			delegate.add( name, field.getValue() );
		}
		return this;
	}
	
	@Override
	public JsonObject build() {
		return delegate.build();
	}

    public NullSafeJsonBuilder add(String name, Timestamp timestamp) {
        return (timestamp != null) ? add(name, Util.getDateTimeFormat().format(timestamp)) : this;
    }
}
