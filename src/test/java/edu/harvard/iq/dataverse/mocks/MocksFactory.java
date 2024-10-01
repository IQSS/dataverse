package edu.harvard.iq.dataverse.mocks;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileCategory;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetFieldType.FieldType;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseFieldTypeInputLevel;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.MetadataBlock;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroup;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroupProvider;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddress;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
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
        addFileMetadata( retVal );
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
    
     public static FileMetadata addFileMetadata( DataFile df ) {
        FileMetadata fmd = new FileMetadata();
        
        fmd.setId( nextId() );
        fmd.setLabel( "Metadata for DataFile " + df.getId() );
        
        fmd.setDataFile(df);
        if ( df.getFileMetadatas() != null ) {
            df.getFileMetadatas().add( fmd );
        } else {
            df.setFileMetadatas( new LinkedList(Arrays.asList(fmd)) );
        }
        
        return fmd;
    }
    
    public static AuthenticatedUser makeAuthenticatedUser( String firstName, String lastName ) {
        AuthenticatedUser user = new AuthenticatedUser();
        user.setId( nextId() );
        user.setAffiliation("UnitTester");
        user.setEmail( firstName + "." + lastName + "@someU.edu" );
        user.setLastName(lastName);
        user.setFirstName(firstName);
        user.setPosition("In-Memory user");
        user.setUserIdentifier("unittest" + user.getId() );
        user.setCreatedTime(new Timestamp(new Date().getTime()));
        user.setLastLoginTime(user.getCreatedTime());
        return user;
    }
    
    /**
     * @return A request with a guest user.
     */
    public static DataverseRequest makeRequest() {
        return makeRequest( GuestUser.get() );
    }
    
    public static DataverseRequest makeRequest( User u ) {
        return new DataverseRequest( u, IpAddress.valueOf("1.2.3.4") );
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
        ds.setId( nextId() );
        ds.setIdentifier("sample-ds-" + ds.getId() );
        ds.setAuthority("10.5072");
        ds.setCategoriesByName( Arrays.asList("CatOne", "CatTwo", "CatThree") );
        final List<DataFile> files = makeFiles(10);
        final List<FileMetadata> metadatas = new ArrayList<>(10);
        final List<DataFileCategory> categories = ds.getCategories();
        Random rand = new Random();
        files.forEach( df ->{
            df.getFileMetadata().addCategory(categories.get(rand.nextInt(categories.size())));
            metadatas.add( df.getFileMetadata() );
        });
        ds.setFiles(files);
        final DatasetVersion initialVersion = ds.getVersions().get(0);
        initialVersion.setFileMetadatas(metadatas);
        
        List<DatasetField> fields = new ArrayList<>();
        DatasetField field = new DatasetField();
        field.setId(nextId());
        field.setSingleValue("Sample Field Value");
        field.setDatasetFieldType( makeDatasetFieldType() );
        fields.add( field );
        initialVersion.setDatasetFields(fields);
        ds.setOwner( makeDataverse() );
        
        return ds;
    }
    
    public static DatasetVersion makeDatasetVersion(List<DataFileCategory> categories) {
        final DatasetVersion retVal = new DatasetVersion();
        final List<DataFile> files = makeFiles(10);
        final List<FileMetadata> metadatas = new ArrayList<>(10);
        Random rand = new Random();
        files.forEach(df -> {
            df.getFileMetadata().addCategory(categories.get(rand.nextInt(categories.size())));
            metadatas.add( df.getFileMetadata() );
        });
        retVal.setFileMetadatas(metadatas);
        
        List<DatasetField> fields = new ArrayList<>();
        DatasetField field = new DatasetField();
        field.setId(nextId());
        field.setSingleValue("Sample Field Value");
        field.setDatasetFieldType( makeDatasetFieldType() );
        fields.add( field );
        retVal.setDatasetFields(fields);
        
        return retVal;
    }
    
    public static DatasetFieldType makeDatasetFieldType() {
        final Long id = nextId();
        DatasetFieldType retVal = new DatasetFieldType("SampleType-"+id, FieldType.TEXT, false);
        retVal.setId(id);
        MetadataBlock mdb = new MetadataBlock();
        mdb.setId(new Random().nextLong());
        mdb.setName("Test");
        retVal.setMetadataBlock(mdb);
        return retVal;
    }
    
    public static DataverseRole makeRole( String name ) {       
        return makeRole(name, true);
    }
    
    public static DataverseRole makeRole( String name, Boolean includePublishDataset ) {
        DataverseRole dvr = new DataverseRole();
        
        dvr.setId( nextId() );
        dvr.setAlias( name );
        dvr.setName( name );
        dvr.setDescription( name + "  " + name + " " + name );
        
        dvr.addPermission(Permission.ManageDatasetPermissions);
        dvr.addPermission(Permission.EditDataset);
        if  (includePublishDataset){
           dvr.addPermission(Permission.PublishDataset);
        }

        dvr.addPermission(Permission.ViewUnpublishedDataset);
        
        return dvr;
    }
    
    public static DataverseFieldTypeInputLevel makeDataverseFieldTypeInputLevel( DatasetFieldType fieldType ) {
        DataverseFieldTypeInputLevel retVal = new DataverseFieldTypeInputLevel();
        
        retVal.setId(nextId());
        retVal.setInclude(true);
        retVal.setDatasetFieldType( fieldType );
        
        return retVal;
    }
    
    public static ExplicitGroup makeExplicitGroup( String name, ExplicitGroupProvider prv ) {
        long id = nextId();
        ExplicitGroup eg = new ExplicitGroup(prv);
        
        eg.setId(id);
        eg.setDisplayName( name==null ? "explicitGroup-" + id : name );
        eg.setGroupAliasInOwner("eg" + id);
        
        return eg;
    }
    
    public static ExplicitGroup makeExplicitGroup( ExplicitGroupProvider prv ) {
        return makeExplicitGroup(null, prv);
    }
}
