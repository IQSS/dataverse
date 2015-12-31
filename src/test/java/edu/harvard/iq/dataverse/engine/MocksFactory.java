package edu.harvard.iq.dataverse.engine;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.MetadataBlock;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddress;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A utility class for creating mock objects for unit tests. Mostly, the non-parameter
 * methods created objects with reasonable defaults that should fit most tests.
 * Of course, feel free to change of make these mocks more elaborate as the code
 * evolves.
 * 
 * @author michael
 */
public class MocksFactory {
    
    private static final AtomicInteger NEXT_ID = new AtomicInteger();
    
    public static Long nextId() {
        return Long.valueOf( NEXT_ID.incrementAndGet() );
    }
    
    public static Date date(int year, int month, int day ) {
        return new Date( LocalDate.of(year, Month.of(month), day).toEpochDay() );
    }
    
    public static Timestamp timestamp(int year, int month, int day ) {
        return new Timestamp( date(year, month, day).getTime() );
    }
    
    public static DataFile makeDataFile() {
        DataFile retVal = new DataFile();
        retVal.setId( nextId() );
        retVal.setContentType("application/unitTests");
        retVal.setCreateDate( new Timestamp(System.currentTimeMillis()) );
        retVal.setModificationTime( retVal.getCreateDate() );
        return retVal;
    }
    
    public static List<DataFile> makeFiles( int count ) {
        List<DataFile> retVal = new ArrayList<>(count);
        for ( int i=0; i<count; i++ ) {
            retVal.add( makeDataFile() );
        }
        return retVal;
    }
    
    public static AuthenticatedUser makeAuthentiucatedUser( String firstName, String lastName ) {
        AuthenticatedUser user = new AuthenticatedUser();
        user.setId( nextId() );
        user.setAffiliation("UnitTester");
        user.setEmail( firstName + "." + lastName + "@someU.edu" );
        user.setLastName(lastName);
        user.setFirstName(firstName);
        user.setPosition("In-Memory user");
        user.setUserIdentifier("unittest" + user.getId() );
        return user;
    }
    
    public static DataverseRequest makeDatasetRequest() {
        return new DataverseRequest( makeAuthentiucatedUser("Jane", "Doe"), IpAddress.valueOf("215.0.2.17") );
    }
    
    public static Dataverse makeDataverse() {
        Dataverse retVal = new Dataverse();
        retVal.setId( nextId() );
        
        retVal.setAffiliation("Unit Test U");
        retVal.setAlias("unitTest" + retVal.getId());
        retVal.setCreateDate(timestamp(2012,4,5));
        retVal.setMetadataBlockRoot(true);
        retVal.setName("UnitTest Dataverse #" + retVal.getId());
        
        MetadataBlock mtb = new MetadataBlock();
        mtb.setDisplayName("Test Block #1-" + retVal.getId());
        mtb.setId(nextId());
        mtb.setDatasetFieldTypes( Arrays.asList(
                new DatasetFieldType("JustAString", DatasetFieldType.FieldType.TEXT, false),
                new DatasetFieldType("ManyStrings", DatasetFieldType.FieldType.TEXT, true),
                new DatasetFieldType("AnEmail", DatasetFieldType.FieldType.EMAIL, false)
        ));
        
        retVal.setMetadataBlocks( Arrays.asList(mtb) );
        
        return retVal;
    }
    
    public static Dataset makeDataset() {
        Dataset ds = new Dataset();
        ds.setIdentifier("sample-ds");
        ds.setFiles( makeFiles(10) );
        ds.setOwner( makeDataverse() );
        return ds;
    }
}
