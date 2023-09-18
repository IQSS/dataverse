package edu.harvard.iq.dataverse.ingest.metadataextraction.impl.plugins.netcdf;

import edu.harvard.iq.dataverse.DatasetFieldConstant;
import edu.harvard.iq.dataverse.ingest.metadataextraction.FileMetadataExtractor;
import edu.harvard.iq.dataverse.ingest.metadataextraction.FileMetadataIngest;
import edu.harvard.iq.dataverse.ingest.metadataextraction.spi.FileMetadataExtractorSpi;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;

public class NetcdfFileMetadataExtractor extends FileMetadataExtractor {

    private static final Logger logger = Logger.getLogger(NetcdfFileMetadataExtractor.class.getCanonicalName());

    public static final String WEST_LONGITUDE_KEY = "geospatial_lon_min";
    public static final String EAST_LONGITUDE_KEY = "geospatial_lon_max";
    public static final String NORTH_LATITUDE_KEY = "geospatial_lat_max";
    public static final String SOUTH_LATITUDE_KEY = "geospatial_lat_min";

    private static final String GEOSPATIAL_BLOCK_NAME = "geospatial";
    private static final String WEST_LONGITUDE = DatasetFieldConstant.westLongitude;
    private static final String EAST_LONGITUDE = DatasetFieldConstant.eastLongitude;
    private static final String NORTH_LATITUDE = DatasetFieldConstant.northLatitude;
    private static final String SOUTH_LATITUDE = DatasetFieldConstant.southLatitude;

    public NetcdfFileMetadataExtractor(FileMetadataExtractorSpi originatingProvider) {
        super(originatingProvider);
    }

    public NetcdfFileMetadataExtractor() {
        super(null);
    }

    @Override
    public FileMetadataIngest ingest(BufferedInputStream stream) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public FileMetadataIngest ingestFile(File file) throws IOException {
        FileMetadataIngest fileMetadataIngest = new FileMetadataIngest();
        fileMetadataIngest.setMetadataBlockName(GEOSPATIAL_BLOCK_NAME);

        Map<String, String> geoFields = parseGeospatial(getNetcdfFile(file));
        WestAndEastLongitude welong = getStandardLongitude(new WestAndEastLongitude(geoFields.get(WEST_LONGITUDE), geoFields.get(EAST_LONGITUDE)));
        String westLongitudeFinal = welong != null ? welong.getWestLongitude() : null;
        String eastLongitudeFinal = welong != null ? welong.getEastLongitude() : null;
        String northLatitudeFinal = geoFields.get(NORTH_LATITUDE);
        String southLatitudeFinal = geoFields.get(SOUTH_LATITUDE);

        logger.fine(getLineStringsUrl(westLongitudeFinal, southLatitudeFinal, eastLongitudeFinal, northLatitudeFinal));

        Map<String, Set<String>> metadataMap = new HashMap<>();
        metadataMap.put(WEST_LONGITUDE, new HashSet<>());
        metadataMap.get(WEST_LONGITUDE).add(westLongitudeFinal);
        metadataMap.put(EAST_LONGITUDE, new HashSet<>());
        metadataMap.get(EAST_LONGITUDE).add(eastLongitudeFinal);
        metadataMap.put(NORTH_LATITUDE, new HashSet<>());
        metadataMap.get(NORTH_LATITUDE).add(northLatitudeFinal);
        metadataMap.put(SOUTH_LATITUDE, new HashSet<>());
        metadataMap.get(SOUTH_LATITUDE).add(southLatitudeFinal);
        fileMetadataIngest.setMetadataMap(metadataMap);
        return fileMetadataIngest;
    }

    public NetcdfFile getNetcdfFile(File file) throws IOException {
        /**
         * <attribute name="geospatial_lat_min" value="25.066666666666666" />
         * south
         * <attribute name="geospatial_lat_max" value="49.40000000000000" />
         * north
         * <attribute name="geospatial_lon_min" value="-124.7666666333333" />
         * west
         * <attribute name="geospatial_lon_max" value="-67.058333300000015" />
         * east
         * <attribute name="geospatial_lon_resolution" value="0.041666666666666" />
         * <attribute name="geospatial_lat_resolution" value="0.041666666666666" />
         * <attribute name="geospatial_lat_units" value="decimal_degrees north" />
         * <attribute name="geospatial_lon_units" value="decimal_degrees east" />
         */
        return NetcdfFiles.open(file.getAbsolutePath());
    }

    private Map<String, String> parseGeospatial(NetcdfFile netcdfFile) {
        Map<String, String> geoFields = new HashMap<>();

        Attribute westLongitude = netcdfFile.findGlobalAttribute(WEST_LONGITUDE_KEY);
        Attribute eastLongitude = netcdfFile.findGlobalAttribute(EAST_LONGITUDE_KEY);
        Attribute northLatitude = netcdfFile.findGlobalAttribute(NORTH_LATITUDE_KEY);
        Attribute southLatitude = netcdfFile.findGlobalAttribute(SOUTH_LATITUDE_KEY);

        geoFields.put(DatasetFieldConstant.westLongitude, getValue(westLongitude));
        geoFields.put(DatasetFieldConstant.eastLongitude, getValue(eastLongitude));
        geoFields.put(DatasetFieldConstant.northLatitude, getValue(northLatitude));
        geoFields.put(DatasetFieldConstant.southLatitude, getValue(southLatitude));

        logger.fine(getLineStringsUrl(
                geoFields.get(DatasetFieldConstant.westLongitude),
                geoFields.get(DatasetFieldConstant.southLatitude),
                geoFields.get(DatasetFieldConstant.eastLongitude),
                geoFields.get(DatasetFieldConstant.northLatitude)));

        return geoFields;
    }

    // We store strings in the database.
    private String getValue(Attribute attribute) {
        if (attribute == null) {
            return null;
        }
        DataType dataType = attribute.getDataType();
        if (dataType.isString()) {
            return attribute.getStringValue();
        } else if (dataType.isNumeric()) {
            return attribute.getNumericValue().toString();
        } else {
            return null;
        }
    }

    // Convert to standard -180 to 180 range by subtracting 360
    // if both longitudea are greater than 180. For example:
    //       west     south      east      north
    //     343.68,     41.8,   353.78,     49.62 becomes
    //     -16.320007, 41.8,    -6.220001, 49.62 instead
    // "If one of them is > 180, the domain is 0:360.
    // If one of them is <0, the domain is -180:180.
    // If both are between 0 and 180, the answer is indeterminate."
    // https://github.com/cf-convention/cf-conventions/issues/435#issuecomment-1505614364
    // Solr only wants -180 to 180. It will throw an error for values outside this range.
    public WestAndEastLongitude getStandardLongitude(WestAndEastLongitude westAndEastLongitude) {
        if (westAndEastLongitude == null) {
            return null;
        }
        if (westAndEastLongitude.getWestLongitude() == null || westAndEastLongitude.getEastLongitude() == null) {
            return null;
        }
        float eastAsFloat;
        float westAsFloat;
        try {
            westAsFloat = Float.valueOf(westAndEastLongitude.getWestLongitude());
            eastAsFloat = Float.valueOf(westAndEastLongitude.getEastLongitude());
        } catch (NumberFormatException ex) {
            return null;
        }
        // "If one of them is > 180, the domain is 0:360"
        if (westAsFloat > 180 && eastAsFloat > 180) {
            Float westStandard = westAsFloat - 360;
            Float eastStandard = eastAsFloat - 360;
            WestAndEastLongitude updatedWeLong = new WestAndEastLongitude(westStandard.toString(), eastStandard.toString());
            return updatedWeLong;
        }
        // "If one of them is <0, the domain is -180:180."
        // 180:180 is what Solr wants. Return it.
        if (westAsFloat < 0 || eastAsFloat < 0) {
            // BUT! Don't return it if the values
            // are so low to be out of range!
            // Something must be wrong with the data.
            if (westAsFloat < -180 || eastAsFloat < -180) {
                return null;
            }
            if (westAsFloat > 180 || eastAsFloat > 180) {
                // Not in the proper range of -80:180
                return null;
            }
            return westAndEastLongitude;
        }
        if ((westAsFloat > 180 || eastAsFloat > 180) && (westAsFloat < 180 || eastAsFloat < 180)) {
            // One value is over 180 and the other is under 180.
            // We don't know if we should subtract 360 or not.
            // Return null to prevent inserting a potentially
            // incorrect bounding box.
            return null;
        }
        return westAndEastLongitude;
    }

    // Generates a handy link to see what the bounding box looks like on a map
    private String getLineStringsUrl(String west, String south, String east, String north) {
        // BBOX (Left (LON) ,Bottom (LAT), Right (LON), Top (LAT), comma separated, with or without decimal point):
        return "https://linestrings.com/bbox/#" + west + "," + south + "," + east + "," + north;
    }

}
