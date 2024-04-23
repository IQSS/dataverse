package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddress;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.engine.TestCommandContext;
import edu.harvard.iq.dataverse.engine.TestDataverseEngine;
import edu.harvard.iq.dataverse.engine.TestEntityManager;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.mocks.MocksFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 * @author michael
 */
public class CreateRoleCommandTest {
    
    boolean saveCalled = false;
    
    TestDataverseEngine engine = new TestDataverseEngine( new TestCommandContext(){
        @Override
        public DataverseRoleServiceBean roles() {
            return new DataverseRoleServiceBean() {
                @Override
                public DataverseRole save(DataverseRole aRole) {
                    saveCalled = true;
                    return aRole;
                }
            };
        }
        
        @Override 
        public EntityManager em() {
            return new LocalTestEntityManager();
            
        }
    });
    
    @BeforeEach
    public void before() {
        saveCalled = false;
    }
    
    @Test
    void testNonSuperUsersCantAddRoles() {
        DataverseRole dvr = new DataverseRole();
        dvr.setAlias("roleTest");
        dvr.setName("Tester Role");
        dvr.addPermission(Permission.AddDataset);
        
        Dataverse dv = MocksFactory.makeDataverse();
        dvr.setOwner(dv);
        
        AuthenticatedUser normalUser = new AuthenticatedUser();
        normalUser.setSuperuser(false);
        
        CreateRoleCommand sut = new CreateRoleCommand(dvr, new DataverseRequest(normalUser,IpAddress.valueOf("89.17.33.33")), dv);
        assertThrows(IllegalCommandException.class, () -> engine.submit(sut));
    }
   
    @Test
    public void testSuperUsersAddRoles() throws CommandException {
        DataverseRole dvr = new DataverseRole();
        dvr.setAlias("roleTest");
        dvr.setName("Tester Role");
        dvr.addPermission(Permission.AddDataset);
        
        Dataverse dv = MocksFactory.makeDataverse();
        dvr.setOwner(dv);
        
        AuthenticatedUser normalUser = new AuthenticatedUser();
        normalUser.setSuperuser(true);
        
        CreateRoleCommand sut = new CreateRoleCommand(dvr, new DataverseRequest(normalUser,IpAddress.valueOf("89.17.33.33")), dv);
        engine.submit(sut);
        assertTrue(saveCalled, "CreateRoleCommand did not call save on the created role.");
    
    }
    
    @Test
    void testGuestUsersCantAddRoles() {
        DataverseRole dvr = new DataverseRole();
        dvr.setAlias("roleTest");
        dvr.setName("Tester Role");
        dvr.addPermission(Permission.AddDataset);
        
        Dataverse dv = MocksFactory.makeDataverse();
        dvr.setOwner(dv);
        
        CreateRoleCommand sut = new CreateRoleCommand(dvr, new DataverseRequest(GuestUser.get(),IpAddress.valueOf("89.17.33.33")), dv);
        assertThrows(IllegalCommandException.class, () -> engine.submit(sut));
    }
    
    private class LocalTestEntityManager extends TestEntityManager {

        @Override
        public <T> T merge(T entity) {
            return entity;
        }

        @Override
        public void persist(Object entity) {
            //
        }

        @Override
        public void flush() {
            //nothing to do here
        }

        @Override
        public <T> TypedQuery<T> createNamedQuery(String name, Class<T> resultClass) {
            //Mocking a query to return no results when 
            //checking for existing role in DB
            TypedQuery mockedQuery = mock(TypedQuery.class);
            when(mockedQuery.setParameter(ArgumentMatchers.anyString(), ArgumentMatchers.any())).thenReturn(mockedQuery);
            when(mockedQuery.getSingleResult()).thenReturn(null);
            return mockedQuery;
        }

    }
    
}
