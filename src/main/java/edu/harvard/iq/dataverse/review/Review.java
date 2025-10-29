package edu.harvard.iq.dataverse.review;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.DvObjectContainer;
import edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.persistence.Entity;

@Entity
public class Review extends DvObjectContainer {

    @Override
    protected boolean isPermissionRoot() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isPermissionRoot'");
    }

    @Override
    public <T> T accept(Visitor<T> v) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'accept'");
    }

    @Override
    public boolean equals(Object o) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'equals'");
    }

    @Override
    public String getDisplayName() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getDisplayName'");
    }

    @Override
    public String getCurrentName() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getCurrentName'");
    }

    @Override
    public boolean isAncestorOf(DvObject other) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isAncestorOf'");
    }

    // TODO consider moving to JsonPrinter
    public static JsonObjectBuilder toJson(Review review) {
        JsonObjectBuilder jsonObjectBuilder = new NullSafeJsonBuilder();
        if (review != null) {
            jsonObjectBuilder.add("toString", review.toString());
        }
        return jsonObjectBuilder;
    }

    public static JsonObjectBuilder toJson() {
        JsonObjectBuilder jsonObjectBuilder = new NullSafeJsonBuilder();
        jsonObjectBuilder.add("toString", "Review");
//        if (review != null) {
//            jsonObjectBuilder.add("toString", review.toString());
//        }
        return jsonObjectBuilder;
    }

}