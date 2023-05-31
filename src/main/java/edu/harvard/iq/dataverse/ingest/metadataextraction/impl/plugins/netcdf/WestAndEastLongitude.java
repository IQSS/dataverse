package edu.harvard.iq.dataverse.ingest.metadataextraction.impl.plugins.netcdf;

import java.util.Objects;

public class WestAndEastLongitude {

    private final String westLongitude;
    private final String eastLongitude;

    public WestAndEastLongitude(String westLongitude, String eastLongitude) {
        this.westLongitude = westLongitude;
        this.eastLongitude = eastLongitude;
    }

    public String getWestLongitude() {
        return westLongitude;
    }

    public String getEastLongitude() {
        return eastLongitude;
    }

    @Override
    public String toString() {
        return "WestAndEastLongitude{" + "westLongitude=" + westLongitude + ", eastLongitude=" + eastLongitude + '}';
    }

    @Override
    public int hashCode() {
        int hash = 3;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final WestAndEastLongitude other = (WestAndEastLongitude) obj;
        if (!Objects.equals(this.westLongitude, other.westLongitude)) {
            return false;
        }
        return Objects.equals(this.eastLongitude, other.eastLongitude);
    }

}
