package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.NoOpTestEntityManager;
import edu.harvard.iq.dataverse.engine.TestCommandContext;
import edu.harvard.iq.dataverse.engine.TestDataverseEngine;
import edu.harvard.iq.dataverse.engine.TestEntityManager;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.harvest.client.HarvestingClient;
import edu.harvard.iq.dataverse.mocks.MocksFactory;
import edu.harvard.iq.dataverse.pidproviders.PidProvider;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 *
 * @author jdarms
 */
public class ReconcilePIDCommandTest {

    TestDataverseEngine engine;
    DataverseRoleServiceBean drsb;
    SystemConfig systemConfig;
    IndexServiceBean indexServiceBean;
    RoleAssigneeServiceBean roleAssigneeServiceBean;
    UserNotificationServiceBean notificationService;
    @BeforeEach
    public void setUp() {
        drsb= mock(DataverseRoleServiceBean.class);
        systemConfig =mock(SystemConfig.class);
        indexServiceBean=mock(IndexServiceBean.class);
        roleAssigneeServiceBean=mock(RoleAssigneeServiceBean.class);
        notificationService =mock(UserNotificationServiceBean.class);
        engine = new TestDataverseEngine(new TestCommandContext() {
            @Override
            public DataverseRoleServiceBean roles() {
                return drsb;
            }

            @Override
            public RoleAssigneeServiceBean roleAssignees() {
                return roleAssigneeServiceBean;
            }

            @Override
            public EntityManager em() {
                return new NoOpTestEntityManager();

            }

            @Override
            public IndexServiceBean index() {
                return indexServiceBean;
            }

            @Override
            public SystemConfig systemConfig() {
                return  systemConfig;
            }

            @Override
            public UserNotificationServiceBean notifications() {
                return notificationService;
            }
        });
    }

    @Test
    public void testSucessfullChangeWithoutFilePIDs() throws Exception {
        // given
        //
        AuthenticatedUser superUser = new AuthenticatedUser();
        superUser.setSuperuser(true);
        DataverseRequest request = MocksFactory.makeRequest(superUser);
        // given a PID Provider
        PidProvider pidProvider = mock(PidProvider.class);
        when(pidProvider.getProtocol()).thenReturn("PROTO");
        when(pidProvider.getAuthority()).thenReturn("PROTO");
        when(pidProvider.getShoulder()).thenReturn("PROTO");
        when(pidProvider.getSeparator()).thenReturn("PROTO");
        when(pidProvider.registerWhenPublished()).thenReturn(false);



        // given a dataset linked with the PIDprovider
        Dataset ds = MocksFactory.makeDataset();
        ds.setPidGenerator(pidProvider);
        GlobalId pid = new GlobalId(pidProvider.getProtocol(),pidProvider.getAuthority(),"OLD-ID", pidProvider.getSeparator(),pidProvider.getUrlPrefix(), pidProvider.getLabel());
        ds.setGlobalId(pid);
        ds.setGlobalIdCreateTime(Timestamp.from(Instant.now()));
        ds.setIdentifierRegistered(true);
        when(pidProvider.alreadyRegistered(ds)).thenReturn(true);
        // given a linked dataset with a givendataverse and
        Dataverse dv = MocksFactory.makeDataverse();
        dv.setPidGenerator(pidProvider);
        dv.setFilePIDsEnabled(false);
        ds.setOwner(dv);

        // given a new PID Provider
        PidProvider pidProvider2 = mock(PidProvider.class);
        when(pidProvider2.getProtocol()).thenReturn("PROTO1");
        when(pidProvider2.getAuthority()).thenReturn("PROTO1");
        when(pidProvider2.getShoulder()).thenReturn("PROTO1");
        when(pidProvider2.getSeparator()).thenReturn("PROTO1");
        when(pidProvider2.registerWhenPublished()).thenReturn(false);
        when(pidProvider2.canManagePID()).thenReturn(true);
        when(pidProvider2.generatePid(ds)).thenAnswer(new Answer<DvObject>() {
            public DvObject answer(InvocationOnMock invocation) {
                Dataset callback = (Dataset) invocation.getArguments()[0];
                callback.setAuthority(pidProvider2.getAuthority());
                callback.setProtocol(pidProvider2.getProtocol());
                callback.setIdentifier("MYIDENT");
                return callback;
            }
        });
        RoleAssignment roleAssignment=new RoleAssignment();
        roleAssignment.setAssigneeIdentifier("MY_ID");
        //when
        when(drsb.directRoleAssignments(ds)).thenReturn(Lists.list(roleAssignment));
        when(roleAssigneeServiceBean.getRoleAssignee("MY_ID")).thenReturn(superUser);
        when(roleAssigneeServiceBean.getExplicitUsers(any())).thenReturn(List.of(superUser));
        when(systemConfig.isFilePIDsEnabledForCollection(dv)).thenReturn(false);
        ReconcileDatasetPidCommand sut = new ReconcileDatasetPidCommand(request, ds, pidProvider2);
        engine.submit(sut);

        //then
        // ensure new PID is set and provider is updated
        assertEquals(pidProvider2,ds.getEffectivePidGenerator());
        assertEquals(true,ds.isIdentifierRegistered());
        assertEquals("MYIDENT",ds.getIdentifier());
        assertNotEquals(pid,ds.getGlobalId()); // must be different that old one!
        // ensure the old one is available as
        assertEquals(ds.getAlternativePersistentIndentifiers().size(),1);
        assertEquals(ds.getAlternativePersistentIndentifiers().stream().findFirst().get().getIdentifier(),"OLD-ID");

        verify(pidProvider,times(1)).alreadyRegistered(ds);
        verify(pidProvider,times(1)).deleteIdentifier(ds);
        verify(indexServiceBean,times(1)).asyncIndexDataset(ds,true);
        verify(notificationService,times(1)).sendNotification(eq(superUser),any(),eq(UserNotification.Type.PIDRECONCILED),any(),any());
    }

    @Test
    public void testSucessfullChangeWithFilePIDs() throws Exception {
        // given
        //
        AuthenticatedUser superUser = new AuthenticatedUser();
        superUser.setSuperuser(true);
        DataverseRequest request = MocksFactory.makeRequest(superUser);
        // given a PID Provider
        PidProvider pidProvider = mock(PidProvider.class);
        when(pidProvider.getProtocol()).thenReturn("PROTO");
        when(pidProvider.getAuthority()).thenReturn("PROTO");
        when(pidProvider.getShoulder()).thenReturn("PROTO");
        when(pidProvider.getSeparator()).thenReturn("PROTO");
        when(pidProvider.registerWhenPublished()).thenReturn(false);



        // given a dataset linked with the PIDprovider
        Dataset ds = MocksFactory.makeDataset();
        ds.setFiles(ds.getFiles().subList(0,1));
        DataFile df = ds.getFiles().get(0);
        GlobalId piddf = new GlobalId(pidProvider.getProtocol(),pidProvider.getAuthority(),"OLD-DF-ID", pidProvider.getSeparator(),pidProvider.getUrlPrefix(), pidProvider.getLabel());
        df.setGlobalId(piddf);
        when(pidProvider.alreadyRegistered(df)).thenReturn(true);

        ds.setPidGenerator(pidProvider);
        GlobalId pid = new GlobalId(pidProvider.getProtocol(),pidProvider.getAuthority(),"OLD-ID", pidProvider.getSeparator(),pidProvider.getUrlPrefix(), pidProvider.getLabel());
        ds.setGlobalId(pid);
        ds.setGlobalIdCreateTime(Timestamp.from(Instant.now()));
        ds.setIdentifierRegistered(true);
        when(pidProvider.alreadyRegistered(ds)).thenReturn(true);
        // given a linked dataset with a givendataverse and
        Dataverse dv = MocksFactory.makeDataverse();
        dv.setPidGenerator(pidProvider);
        dv.setFilePIDsEnabled(false);
        ds.setOwner(dv);


        // given a new PID Provider
        PidProvider pidProvider2 = mock(PidProvider.class);
        when(pidProvider2.getProtocol()).thenReturn("PROTO1");
        when(pidProvider2.getAuthority()).thenReturn("PROTO1");
        when(pidProvider2.getShoulder()).thenReturn("PROTO1");
        when(pidProvider2.getSeparator()).thenReturn("PROTO1");
        when(pidProvider2.registerWhenPublished()).thenReturn(false);
        when(pidProvider2.canManagePID()).thenReturn(true);

        when(pidProvider2.generatePid(ds)).thenAnswer(new Answer<DvObject>() {
            public DvObject answer(InvocationOnMock invocation) {
                Dataset callback = (Dataset) invocation.getArguments()[0];
                callback.setAuthority(pidProvider2.getAuthority());
                callback.setProtocol(pidProvider2.getProtocol());
                callback.setIdentifier("MYIDENT");
                return callback;
            }
        });
        when(pidProvider.alreadyRegistered(df)).thenReturn(false);
        when(pidProvider2.generatePid(df)).thenAnswer(new Answer<DvObject>() {
            public DvObject answer(InvocationOnMock invocation) {
                DataFile callback = (DataFile) invocation.getArguments()[0];
                callback.setAuthority(pidProvider2.getAuthority());
                callback.setProtocol(pidProvider2.getProtocol());
                callback.setIdentifier("MYIDENT_DF");
                return callback;
            }
        });
        RoleAssignment roleAssignment=new RoleAssignment();
        roleAssignment.setAssigneeIdentifier("MY_ID");
        //when
        when(drsb.directRoleAssignments(ds)).thenReturn(Lists.list(roleAssignment));
        when(roleAssigneeServiceBean.getRoleAssignee("MY_ID")).thenReturn(superUser);
        when(roleAssigneeServiceBean.getExplicitUsers(any())).thenReturn(List.of(superUser));
        when(systemConfig.isFilePIDsEnabledForCollection(dv)).thenReturn(true);
        ReconcileDatasetPidCommand sut = new ReconcileDatasetPidCommand(request, ds, pidProvider2);
        engine.submit(sut);

        //then
        // ensure new PID is set and provider is updated
        assertEquals(pidProvider2,ds.getEffectivePidGenerator());
        assertEquals(pidProvider2,df.getEffectivePidGenerator());
        assertEquals(true,ds.isIdentifierRegistered());
        assertEquals(true,df.isIdentifierRegistered());
        assertEquals("MYIDENT",ds.getIdentifier());
        assertEquals("MYIDENT_DF",df.getIdentifier());
        assertNotEquals(pid,ds.getGlobalId()); // must be different that old one!
        // ensure the old one is available as
        assertEquals(ds.getAlternativePersistentIndentifiers().size(),1);
        assertEquals(ds.getAlternativePersistentIndentifiers().stream().findFirst().get().getIdentifier(),"OLD-ID");
        assertEquals(df.getAlternativePersistentIndentifiers().size(),1);
        assertEquals(df.getAlternativePersistentIndentifiers().stream().findFirst().get().getIdentifier(),"OLD-DF-ID");

        verify(pidProvider,times(1)).alreadyRegistered(ds);
        verify(pidProvider,times(1)).deleteIdentifier(ds);
        verify(indexServiceBean,times(1)).asyncIndexDataset(ds,true);
        verify(notificationService,times(1)).sendNotification(eq(superUser),any(),eq(UserNotification.Type.PIDRECONCILED),any(),any());


    }

    @Test
    public void testGuardNoOp() throws CommandException {
        // given
        //
        AuthenticatedUser superUser = new AuthenticatedUser();
        superUser.setSuperuser(true);
        DataverseRequest request = MocksFactory.makeRequest(superUser);
        // given a PID Provider
        PidProvider pidProvider = mock(PidProvider.class);
        when(pidProvider.getProtocol()).thenReturn("PROTO");
        when(pidProvider.getAuthority()).thenReturn("PROTO");
        when(pidProvider.getShoulder()).thenReturn("PROTO");
        when(pidProvider.getSeparator()).thenReturn("PROTO");
        when(pidProvider.registerWhenPublished()).thenReturn(true);

        // given a dataset linked with a PidProvider and corresponding globalId
        Dataset ds = MocksFactory.makeDataset();
        // a
        ds.setPidGenerator(pidProvider);

        // given a linked dataset with a givendataverse and
        Dataverse dv = MocksFactory.makeDataverse();
        dv.setPidGenerator(pidProvider);
        ds.setOwner(dv);



        ReconcileDatasetPidCommand sut = new ReconcileDatasetPidCommand(request, ds, pidProvider);
        assertThrows(IllegalCommandException.class, () -> engine.submit(sut),"Should throw ICE when no change");
    }
    @Test
    public void testGuardAlreadyPublished() throws CommandException {
        // given
        //
        AuthenticatedUser superUser = new AuthenticatedUser();
        superUser.setSuperuser(true);
        DataverseRequest request = MocksFactory.makeRequest(superUser);
        // given a PID Provider
        PidProvider pidProvider = mock(PidProvider.class);
        when(pidProvider.getProtocol()).thenReturn("PROTO");
        when(pidProvider.getAuthority()).thenReturn("PROTO");
        when(pidProvider.getShoulder()).thenReturn("PROTO");
        when(pidProvider.getSeparator()).thenReturn("PROTO");
        when(pidProvider.registerWhenPublished()).thenReturn(true);

        // given a dataset linked with the PIDprovider
        Dataset ds = MocksFactory.makeDataset();
        ds.setPidGenerator(pidProvider);
        ds.setPublicationDate(Timestamp.from(Instant.now()));

        // given a linked dataset with a givendataverse and
        Dataverse dv = MocksFactory.makeDataverse();
        dv.setPidGenerator(pidProvider);
        ds.setOwner(dv);




        ReconcileDatasetPidCommand sut = new ReconcileDatasetPidCommand(request, ds, pidProvider);
        assertThrows(IllegalCommandException.class, () -> engine.submit(sut),"Should throw ICE when already published");
    }
    @Test
    public void testGuardHarvested() throws CommandException {
        // given
        //
        AuthenticatedUser superUser = new AuthenticatedUser();
        superUser.setSuperuser(true);
        DataverseRequest request = MocksFactory.makeRequest(superUser);
        // given a PID Provider
        PidProvider pidProvider = mock(PidProvider.class);
        when(pidProvider.getProtocol()).thenReturn("PROTO");
        when(pidProvider.getAuthority()).thenReturn("PROTO");
        when(pidProvider.getShoulder()).thenReturn("PROTO");
        when(pidProvider.getSeparator()).thenReturn("PROTO");
        when(pidProvider.registerWhenPublished()).thenReturn(true);

        // given a dataset linked with the PIDprovider
        Dataset ds = MocksFactory.makeDataset();
        ds.setHarvestedFrom(mock(HarvestingClient.class));
        ds.setPidGenerator(pidProvider);

        // given a linked dataset with a givendataverse and
        Dataverse dv = MocksFactory.makeDataverse();
        dv.setPidGenerator(pidProvider);
        ds.setOwner(dv);

        ReconcileDatasetPidCommand sut = new ReconcileDatasetPidCommand(request, ds, pidProvider);
        assertThrows(IllegalCommandException.class, () -> engine.submit(sut),"Should throw ICE when already published");
    }

}
