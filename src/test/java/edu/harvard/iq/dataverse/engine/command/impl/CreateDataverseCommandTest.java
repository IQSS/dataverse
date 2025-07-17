package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseFacet;
import edu.harvard.iq.dataverse.DataverseFacetServiceBean;
import edu.harvard.iq.dataverse.DataverseFieldTypeInputLevel;
import edu.harvard.iq.dataverse.DataverseFieldTypeInputLevelServiceBean;
import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddress;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static edu.harvard.iq.dataverse.mocks.MocksFactory.*;
import edu.harvard.iq.dataverse.engine.TestCommandContext;
import edu.harvard.iq.dataverse.engine.TestDataverseEngine;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author michael
 */
public class CreateDataverseCommandTest {
    
    boolean indexCalled = false;
    Map<String, Dataverse> dvByAliasStore = new HashMap<>();
    Map<Long, Dataverse> dvStore = new HashMap<>();
    boolean isRootDvExists;
    boolean facetsDeleted;
    boolean dftilsDeleted;
    List<DataverseFieldTypeInputLevel> createdDftils;
    List<DataverseFacet> createdFacets;
    
    DataverseServiceBean dataverses = new DataverseServiceBean(){
        @Override
        public boolean isRootDataverseExists() {
            return isRootDvExists;
        }

        @Override
        public Dataverse findByAlias(String anAlias) {
            return dvByAliasStore.get(anAlias);
        }

        @Override
        public Dataverse save(Dataverse dataverse) {
            if ( dataverse.getId() == null ) {
                dataverse.setId( nextId() );
            }
            dvStore.put( dataverse.getId(), dataverse);
            if ( dataverse.getAlias() != null ) {
                dvByAliasStore.put( dataverse.getAlias(), dataverse);
            }
            return dataverse;
        }
        
        @Override
        public boolean index(Dataverse dataverse) {
            indexCalled=true;
            return true;
        }        
        
    };
    
    DataverseRoleServiceBean roles = new DataverseRoleServiceBean(){
        
        List<RoleAssignment> assignments = new LinkedList<>();
        
        Map<String, DataverseRole> builtInRoles;
        
        {
            builtInRoles = new HashMap<>();
            builtInRoles.put( DataverseRole.EDITOR, makeRole("default-editor"));
            builtInRoles.put( DataverseRole.ADMIN, makeRole("default-admin"));
            builtInRoles.put( DataverseRole.MANAGER, makeRole("default-manager"));
        }
        
        @Override
        public DataverseRole findBuiltinRoleByAlias(String alias) {
            return builtInRoles.get(alias);
        }

        @Override
        public RoleAssignment save(RoleAssignment assignment) {
            assignment.setId( nextId() );
            assignments.add(assignment);
            return assignment;
        }
        
        @Override
        public RoleAssignment save(RoleAssignment assignment, boolean index) {
            return save (assignment);
        }        

        @Override
        public List<RoleAssignment> directRoleAssignments(DvObject dvo) {
            // works since there's only one dataverse involved in the context 
            // of this unit test.
            return assignments;
        }
        
        
        
    };
    
    IndexServiceBean index = new IndexServiceBean(){
        @Override
        public Future<String> indexDataverse(Dataverse dataverse) {
            indexCalled = true;
            return null;
        }
    };
    
    DataverseFieldTypeInputLevelServiceBean dfils = new DataverseFieldTypeInputLevelServiceBean(){
        @Override
        public void create(DataverseFieldTypeInputLevel dataverseFieldTypeInputLevel) {
            createdDftils.add( dataverseFieldTypeInputLevel );
        }
        
        @Override
        public DataverseFieldTypeInputLevel save(DataverseFieldTypeInputLevel dataverseFieldTypeInputLevel) {
            createdDftils.add( dataverseFieldTypeInputLevel );
            return dataverseFieldTypeInputLevel;
        }
        
        @Override 
        public DataverseFieldTypeInputLevel findByDataverseIdDatasetFieldTypeId(Long dataverseId, Long datasetFieldTypeId) {
            DataverseFieldTypeInputLevel dfil = new DataverseFieldTypeInputLevel();
            return dfil;
        }
        

        @Override
        public void deleteDataverseFieldTypeInputLevelFor(Dataverse d) {
            dftilsDeleted = true;
        }
        
    };
    
    DataverseFacetServiceBean facets = new DataverseFacetServiceBean() {
        @Override
        public DataverseFacet create(int displayOrder, DatasetFieldType fieldType, Dataverse ownerDv) {
            DataverseFacet df = new DataverseFacet();
            df.setDatasetFieldType(fieldType);
            df.setDataverse(ownerDv);
            df.setDisplayOrder(displayOrder);
            createdFacets.add(df);
            return df;
        }
        

        @Override
        public void deleteFacetsFor(Dataverse d) {
            facetsDeleted = true;
        }
        
    };
    
    TestDataverseEngine engine;
    
    
    @BeforeEach
    public void setUp() {
        indexCalled = false;
        dvStore.clear();
        dvByAliasStore.clear();
        isRootDvExists = true;
        facetsDeleted = false;
        createdDftils = new ArrayList<>();
        createdFacets = new ArrayList<>();
        
        engine = new TestDataverseEngine( new TestCommandContext(){
            @Override
            public IndexServiceBean index() {
                return index;
            }

            @Override
            public DataverseRoleServiceBean roles() {
                return roles;
            }

            @Override
            public DataverseServiceBean dataverses() {
                return dataverses;
            }

            @Override
            public DataverseFacetServiceBean facets() {
                return facets;
            }

            @Override
            public DataverseFieldTypeInputLevelServiceBean fieldTypeInputLevels() {
                return dfils;
            }
            
        } );
    }
    

    @Test
    public void testDefaultOptions() throws CommandException {
        Dataverse dv = makeDataverse();
        dv.setCreateDate(null);
        dv.setId(null);
        dv.setCreator(null);
        dv.setDefaultContributorRole(null);
        dv.setOwner( makeDataverse() );
        final DataverseRequest request = makeRequest(makeAuthenticatedUser("jk", "rollin'"));
        
        CreateDataverseCommand sut = new CreateDataverseCommand(dv, request, null, null);
        Dataverse result = engine.submit(sut);
        
        assertNotNull( result.getCreateDate() );
        assertNotNull( result.getId() );
        
        assertEquals( result.getCreator(), request.getUser() );
        assertEquals( Dataverse.DataverseType.UNCATEGORIZED, result.getDataverseType() );
        assertEquals( roles.findBuiltinRoleByAlias(DataverseRole.EDITOR), result.getDefaultContributorRole() );
        
        // Assert that the creator is admin.
        final RoleAssignment roleAssignment = roles.directRoleAssignments(dv).get(0);
        assertEquals( roles.findBuiltinRoleByAlias(DataverseRole.ADMIN), roleAssignment.getRole() );
        assertEquals( dv, roleAssignment.getDefinitionPoint() );
        assertEquals( roleAssignment.getAssigneeIdentifier(), request.getUser().getIdentifier() );
        
        // The following is a pretty wierd way to test that the create date defaults to 
        // now, but it works across date changes.
        assertTrue(Math.abs(System.currentTimeMillis() - result.getCreateDate().toInstant().toEpochMilli()) < 1000,
            "When the supplied creation date is null, date should default to command execution time");
        
        assertTrue( result.isPermissionRoot() );
        assertTrue( result.isThemeRoot() );
        assertTrue( indexCalled );
    }

    @Test
    public void testCustomOptions() throws CommandException {
        Dataverse dv = makeDataverse();
        
        Timestamp creation = timestamp(1990,12,12);
        AuthenticatedUser creator = makeAuthenticatedUser("Joe", "Walsh");
        
        dv.setCreateDate(creation);
        
        dv.setId(null);
        dv.setCreator(creator);
        dv.setDefaultContributorRole(null);
        dv.setOwner( makeDataverse() );
        dv.setDataverseType(Dataverse.DataverseType.JOURNALS);
        dv.setDefaultContributorRole( roles.findBuiltinRoleByAlias(DataverseRole.MANAGER) );
        
        final DataverseRequest request = makeRequest();
        List<DatasetFieldType> expectedFacets = Arrays.asList( makeDatasetFieldType(), makeDatasetFieldType(), makeDatasetFieldType());
        List<DataverseFieldTypeInputLevel> dftils = Arrays.asList( makeDataverseFieldTypeInputLevel(makeDatasetFieldType()),
                                                                    makeDataverseFieldTypeInputLevel(makeDatasetFieldType()),
                                                                    makeDataverseFieldTypeInputLevel(makeDatasetFieldType()));
        
        CreateDataverseCommand sut = new CreateDataverseCommand(dv, request, new LinkedList(expectedFacets), new LinkedList(dftils) );
        Dataverse result = engine.submit(sut);
        
        assertEquals( creation, result.getCreateDate() );
        assertNotNull( result.getId() );
        
        assertEquals( creator, result.getCreator() );
        assertEquals( Dataverse.DataverseType.JOURNALS, result.getDataverseType() );
        assertEquals( roles.findBuiltinRoleByAlias(DataverseRole.MANAGER), result.getDefaultContributorRole() );
        
        // Assert that the creator is admin.
        final RoleAssignment roleAssignment = roles.directRoleAssignments(dv).get(0);
        assertEquals( roles.findBuiltinRoleByAlias(DataverseRole.ADMIN), roleAssignment.getRole() );
        assertEquals( dv, roleAssignment.getDefinitionPoint() );
        assertEquals( roleAssignment.getAssigneeIdentifier(), request.getUser().getIdentifier() );
        
        assertTrue( result.isPermissionRoot() );
        assertTrue( result.isThemeRoot() );
        assertTrue( indexCalled );
        
        assertTrue( facetsDeleted );
        int i=0;
        for ( DataverseFacet df : createdFacets ) {
            assertEquals( i, df.getDisplayOrder() );
            assertEquals( result, df.getDataverse() );
            assertEquals( expectedFacets.get(i), df.getDatasetFieldType() );
            
            i++;
        }
        
        assertTrue( dftilsDeleted );
        for ( DataverseFieldTypeInputLevel dftil : createdDftils ) {
            assertEquals( result, dftil.getDataverse() );
        }
    }
    
    @Test
    void testCantCreateAdditionalRoot() {
        assertThrows(IllegalCommandException.class,
            () -> engine.submit( new CreateDataverseCommand(makeDataverse(), makeRequest(), null, null) )
        );
    }
    
    @Test
    void testGuestCantCreateDataverse() {
        final DataverseRequest request = new DataverseRequest( GuestUser.get(), IpAddress.valueOf("::") );
        isRootDvExists = false;
        assertThrows(IllegalCommandException.class,
            () -> engine.submit(new CreateDataverseCommand(makeDataverse(), request, null, null) )
        );
    }

    @Test
    void testCantCreateAnotherWithSameAlias() {
        
        String alias = "alias";
        final Dataverse dvFirst = makeDataverse();
        dvFirst.setAlias(alias);
        dvFirst.setOwner( makeDataverse() );
        assertThrows(IllegalCommandException.class,
            () -> engine.submit(new CreateDataverseCommand(dvFirst, makeRequest(), null, null) ));
        
        final Dataverse dv = makeDataverse();
        dv.setOwner( makeDataverse() );
        dv.setAlias(alias);
        assertThrows(IllegalCommandException.class,
            () -> engine.submit(new CreateDataverseCommand(dv, makeRequest(), null, null) )
        );
    }
    
}
