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
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;

public class NetcdfFileMetadataExtractor extends FileMetadataExtractor {

    public static final String WEST_LONGITUDE_KEY = "geospatial_lon_min";
    public static final String EAST_LONGITUDE_KEY = "geospatial_lon_max";
    public static final String NORTH_LATITUDE_KEY = "geospatial_lat_max";
    public static final String SOUTH_LATITUDE_KEY = "geospatial_lat_min";

    private static final String GEOSPATIAL_BLOCK_NAME = "geospatial";
    private static final String ATTRIBUTE_TYPE = "astroType"; // delete this!
    private static final String COUNTRY = DatasetFieldConstant.country;
    private static final String CITY = DatasetFieldConstant.city;
    private static final String GEOGRAPHIC_BOUNDING_BOX = DatasetFieldConstant.geographicBoundingBox;

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
        parseGeospatial(getNetcdfFile(file));
        Map<String, Set<String>> metadataMap = new HashMap<>();
//        metadataMap.put(ATTRIBUTE_TYPE, new HashSet<String>());
//        metadataMap.put(GEOGRAPHIC_BOUNDING_BOX, new HashSet<String>());
        metadataMap.put(COUNTRY, new HashSet<String>());
        metadataMap.get(COUNTRY).add("United States");
        metadataMap.put(CITY, new HashSet<String>());
        metadataMap.get(CITY).add("Boston");
        fileMetadataIngest.setMetadataMap(metadataMap);
        Map<String, Set<String>> fileMetadataMap = fileMetadataIngest.getMetadataMap();
        for (Map.Entry<String, Set<String>> entry : fileMetadataMap.entrySet()) {
            String key = entry.getKey();
            Set<String> value = entry.getValue();
            System.out.println("key: " + key + " value: " + value);
        }
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
        System.out.println(file.getAbsolutePath());
        System.out.println(file.getAbsolutePath());
        System.out.println(file.getAbsolutePath());
        return NetcdfFiles.open(file.getAbsolutePath());
    }

    private Map<String, String> parseGeospatial(NetcdfFile netcdfFile) {
        Map<String, String> geoFields = new HashMap<>();

        Attribute history = netcdfFile.findGlobalAttribute("history");
        System.out.println("history: " + history);
        Attribute westLongitude = netcdfFile.findGlobalAttribute(WEST_LONGITUDE_KEY);
        System.out.println("west: " + westLongitude);
        Attribute eastLongitude = netcdfFile.findGlobalAttribute(EAST_LONGITUDE_KEY);
        Attribute northLatitude = netcdfFile.findGlobalAttribute(NORTH_LATITUDE_KEY);
        Attribute southLatitude = netcdfFile.findGlobalAttribute(SOUTH_LATITUDE_KEY);

        geoFields.put(DatasetFieldConstant.westLongitude, getValue(westLongitude));
        geoFields.put(DatasetFieldConstant.eastLongitude, getValue(eastLongitude));
        geoFields.put(DatasetFieldConstant.northLatitude, getValue(northLatitude));
        geoFields.put(DatasetFieldConstant.southLatitude, getValue(southLatitude));

        System.out.println("https://linestrings.com/bbox/#"
                + geoFields.get(DatasetFieldConstant.westLongitude) + ","
                + geoFields.get(DatasetFieldConstant.southLatitude) + ","
                + geoFields.get(DatasetFieldConstant.eastLongitude) + ","
                + geoFields.get(DatasetFieldConstant.westLongitude)
        );

        return geoFields;
    }

    // We store strings in the database.
    private String getValue(Attribute attribute) {
        DataType dataType = attribute.getDataType();
        System.out.println("dataType: " + dataType);
        if (dataType.isString()) {
            System.out.println("is string: " + attribute.getStringValue());
            return attribute.getStringValue();
        } else if (dataType.isNumeric()) {
            System.out.println("is numeric: " + attribute.getNumericValue().toString());
            return attribute.getNumericValue().toString();
        } else {
            System.out.println("returning null");
            return null;
        }
    }

}
