package edu.harvard.iq.dataverse.authorization;

public class GuestUser implements User {

    @Override
    public String getIdentifier() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getDisplayInfo() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
