package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.NoOpTestEntityManager;
import edu.harvard.iq.dataverse.engine.TestCommandContext;
import edu.harvard.iq.dataverse.engine.TestDataverseEngine;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.harvest.client.HarvestingClient;
import edu.harvard.iq.dataverse.mocks.MocksFactory;
import edu.harvard.iq.dataverse.pidproviders.PidProvider;
import edu.harvard.iq.dataverse.pidproviders.PidUtil;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import jakarta.persistence.EntityManager;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;

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
    public void testSuccessfulChangeWithoutFilePIDs() throws Exception {
        // given a user that invokes a command request
        AuthenticatedUser superUser = new AuthenticatedUser();
        superUser.setSuperuser(true);
        DataverseRequest request = MocksFactory.makeRequest(superUser);

        // given a simple PID Provider
        PidProvider pidProvider = mock(PidProvider.class);
        when(pidProvider.getProtocol()).thenReturn("PROTO");
        when(pidProvider.getAuthority()).thenReturn("PROTO");
        when(pidProvider.getShoulder()).thenReturn("PROTO");
        when(pidProvider.getSeparator()).thenReturn("PROTO");
        when(pidProvider.getLabel()).thenReturn("PROTO");
        when(pidProvider.getId()).thenReturn("PROTO");
        when(pidProvider.registerWhenPublished()).thenReturn(false);
        PidUtil.addToProviderList(pidProvider);

        GlobalId pid = new GlobalId(pidProvider.getProtocol(),pidProvider.getAuthority(),"OLD-ID", pidProvider.getSeparator(),pidProvider.getUrlPrefix(), pidProvider.getLabel());
        when(pidProvider.parsePersistentId(eq("PROTO"),eq("PROTO"),eq("OLD-ID"))).thenReturn(pid);

        // given a dataset linked with the PID provider
        Dataset ds = MocksFactory.makeDataset();
        ds.setGlobalId(pid);
        ds.setGlobalIdCreateTime(Timestamp.from(Instant.now()));
        ds.setIdentifierRegistered(true);
        when(pidProvider.alreadyRegistered(ds)).thenReturn(true);

        // given a linked dataset with a given dataverse
        Dataverse dv = MocksFactory.makeDataverse();
        ds.setOwner(dv);

        // given a new PID Provider
        PidProvider newPIDProviderMock = mock(PidProvider.class);
        when(newPIDProviderMock.getProtocol()).thenReturn("PROTO1");
        when(newPIDProviderMock.getAuthority()).thenReturn("PROTO1");
        when(newPIDProviderMock.getShoulder()).thenReturn("PROTO1");
        when(newPIDProviderMock.getSeparator()).thenReturn("PROTO1");
        when(newPIDProviderMock.getLabel()).thenReturn("PROTO1");
        when(newPIDProviderMock.getId()).thenReturn("PROTO1");
        when(newPIDProviderMock.registerWhenPublished()).thenReturn(false);
        when(newPIDProviderMock.canManagePID()).thenReturn(true);
        PidUtil.addToProviderList(newPIDProviderMock);

        // create new global ID
        GlobalId pid2 = new GlobalId(newPIDProviderMock.getProtocol(), newPIDProviderMock.getAuthority(),"MYIDENT", newPIDProviderMock.getSeparator(), newPIDProviderMock.getUrlPrefix(), newPIDProviderMock.getLabel());
        when(newPIDProviderMock.parsePersistentId(eq("PROTO1"),eq("PROTO1"),eq("MYIDENT"))).thenReturn(pid2);
        when(newPIDProviderMock.generatePid(ds)).thenAnswer(new Answer<DvObject>() {
            public DvObject answer(InvocationOnMock invocation) {
                Dataset callback = (Dataset) invocation.getArguments()[0];
                callback.setAuthority(pid2.getAuthority());
                callback.setProtocol(pid2.getProtocol());
                callback.setIdentifier(pid2.getIdentifier());
                return callback;
            }
        });

        // mocks and stubs for notification
        RoleAssignment roleAssignment = new RoleAssignment();
        roleAssignment.setAssigneeIdentifier("MY_ID");
        when(drsb.directRoleAssignments(ds)).thenReturn(Lists.list(roleAssignment));
        when(roleAssigneeServiceBean.getRoleAssignee("MY_ID")).thenReturn(superUser);
        when(roleAssigneeServiceBean.getExplicitUsers(any())).thenReturn(List.of(superUser));

        // disable file PIDs for collection
        when(systemConfig.isFilePIDsEnabledForCollection(dv)).thenReturn(false);

        // run reconcile PID command
        ReconcileDatasetPidCommand sut = new ReconcileDatasetPidCommand(request, ds, newPIDProviderMock);
        engine.submit(sut);

        // then the global identifier should no longer be the previous one
        assertNotEquals(pid, ds.getGlobalId());
        // but it should be the newly generated one
        assertEquals(pid2, ds.getGlobalId());
        // which should also be registered and should have a create timestamp
        assertEquals(true,ds.isIdentifierRegistered());
        assertNotEquals(null,ds.getGlobalIdCreateTime());
        // further, the old one should be available as alternative id
        assertEquals(ds.getAlternativePersistentIndentifiers().size(),1);
        assertEquals(ds.getAlternativePersistentIndentifiers().stream().findFirst().get().getIdentifier(),"OLD-ID");

        // further, we should be sure that the user is notified about the change
        verify(notificationService, times(1)).sendNotification(eq(superUser),any(),eq(UserNotification.Type.PIDRECONCILED),any(),any());
        // and that the changes are reflected into the search index
        verify(indexServiceBean, times(1)).asyncIndexDataset(ds,true);
        // and that the old identifier is deleted
        verify(pidProvider, times(1)).deleteIdentifier(ds);

        // clean up PID provider list
        PidUtil.clearPidProviders();
    }

    @Test
    public void testSuccessfulChangeWithoutFilePIDs_existing_altID() throws Exception {
        // given a user that invokes a command request
        AuthenticatedUser superUser = new AuthenticatedUser();
        superUser.setSuperuser(true);
        DataverseRequest request = MocksFactory.makeRequest(superUser);

        // given a simple PID Provider
        PidProvider pidProvider = mock(PidProvider.class);
        when(pidProvider.getProtocol()).thenReturn("PROTO");
        when(pidProvider.getAuthority()).thenReturn("PROTO");
        when(pidProvider.getShoulder()).thenReturn("PROTO");
        when(pidProvider.getSeparator()).thenReturn("PROTO");
        when(pidProvider.getLabel()).thenReturn("PROTO");
        when(pidProvider.getId()).thenReturn("PROTO");
        when(pidProvider.registerWhenPublished()).thenReturn(false);
        PidUtil.addToProviderList(pidProvider);

        GlobalId pid = new GlobalId(pidProvider.getProtocol(),pidProvider.getAuthority(),"OLD-ID", pidProvider.getSeparator(),pidProvider.getUrlPrefix(), pidProvider.getLabel());
        when(pidProvider.parsePersistentId(eq("PROTO"),eq("PROTO"),eq("OLD-ID"))).thenReturn(pid);

        // given a dataset linked with the PID provider
        Dataset ds = MocksFactory.makeDataset();
        AlternativePersistentIdentifier aip = new AlternativePersistentIdentifier();
        aip.setStorageLocationDesignator(true);
        HashSet<AlternativePersistentIdentifier> aip_set = new HashSet<>();
        aip_set.add(aip);
        ds.setAlternativePersistentIndentifiers(aip_set);

        ds.setGlobalId(pid);
        ds.setGlobalIdCreateTime(Timestamp.from(Instant.now()));
        ds.setIdentifierRegistered(true);

        when(pidProvider.alreadyRegistered(ds)).thenReturn(true);
        // given a linked dataset with a given dataverse
        Dataverse dv = MocksFactory.makeDataverse();
        ds.setOwner(dv);

        // given a new PID Provider
        PidProvider newPIDProviderMock = mock(PidProvider.class);
        when(newPIDProviderMock.getProtocol()).thenReturn("PROTO1");
        when(newPIDProviderMock.getAuthority()).thenReturn("PROTO1");
        when(newPIDProviderMock.getShoulder()).thenReturn("PROTO1");
        when(newPIDProviderMock.getSeparator()).thenReturn("PROTO1");
        when(newPIDProviderMock.getLabel()).thenReturn("PROTO1");
        when(newPIDProviderMock.getId()).thenReturn("PROTO1");
        when(newPIDProviderMock.registerWhenPublished()).thenReturn(false);
        when(newPIDProviderMock.canManagePID()).thenReturn(true);
        PidUtil.addToProviderList(newPIDProviderMock);

        // create new global ID
        GlobalId pid2 = new GlobalId(newPIDProviderMock.getProtocol(), newPIDProviderMock.getAuthority(),"MYIDENT", newPIDProviderMock.getSeparator(), newPIDProviderMock.getUrlPrefix(), newPIDProviderMock.getLabel());
        when(newPIDProviderMock.parsePersistentId(eq("PROTO1"),eq("PROTO1"),eq("MYIDENT"))).thenReturn(pid2);
        when(newPIDProviderMock.generatePid(ds)).thenAnswer(new Answer<DvObject>() {
            public DvObject answer(InvocationOnMock invocation) {
                Dataset callback = (Dataset) invocation.getArguments()[0];
                callback.setAuthority(pid2.getAuthority());
                callback.setProtocol(pid2.getProtocol());
                callback.setIdentifier(pid2.getIdentifier());
                return callback;
            }
        });

        // mocks and stubs for notification
        RoleAssignment roleAssignment=new RoleAssignment();
        roleAssignment.setAssigneeIdentifier("MY_ID");
        when(drsb.directRoleAssignments(ds)).thenReturn(Lists.list(roleAssignment));
        when(roleAssigneeServiceBean.getRoleAssignee("MY_ID")).thenReturn(superUser);
        when(roleAssigneeServiceBean.getExplicitUsers(any())).thenReturn(List.of(superUser));

        // disable file PIDs for collection
        when(systemConfig.isFilePIDsEnabledForCollection(dv)).thenReturn(false);

        // run reconcile PID command
        ReconcileDatasetPidCommand sut = new ReconcileDatasetPidCommand(request, ds, newPIDProviderMock);
        engine.submit(sut);

        // then the global identifier should no longer be the previous one
        assertNotEquals(pid,ds.getGlobalId());
        // but it should be the newly generated one
        assertEquals(pid2, ds.getGlobalId());
        // which should also be registered and should have a create timestamp
        assertEquals(true, ds.isIdentifierRegistered());
        assertNotEquals(null, ds.getGlobalIdCreateTime());
        // further, the old one should be available as alternative id
        assertEquals(ds.getAlternativePersistentIndentifiers().size(),2);
        assertEquals(ds.getAlternativePersistentIndentifiers().stream().filter(s->s.isStorageLocationDesignator()).count(),1);
        assertEquals(ds.getAlternativePersistentIndentifiers().stream().filter(s->s.getIdentifier()=="OLD-ID").count(),1);

        // further, we should be sure that the user is notified about the change
        verify(notificationService, times(1)).sendNotification(eq(superUser),any(),eq(UserNotification.Type.PIDRECONCILED),any(),any());
        // and that the changes are reflected into the search index
        verify(indexServiceBean, times(1)).asyncIndexDataset(ds,true);
        // and that the old identifier is deleted
        verify(pidProvider, times(1)).deleteIdentifier(ds);

        // clean up PID provider list
        PidUtil.clearPidProviders();
    }

    @Test
    public void testSuccessfulChangeWithFilePIDs() throws Exception {
        // given a user that invokes a command request
        AuthenticatedUser superUser = new AuthenticatedUser();
        superUser.setSuperuser(true);
        DataverseRequest request = MocksFactory.makeRequest(superUser);
        // given a simple PID Provider
        PidProvider pidProvider = mock(PidProvider.class);
        when(pidProvider.getProtocol()).thenReturn("PROTO");
        when(pidProvider.getAuthority()).thenReturn("PROTO");
        when(pidProvider.getShoulder()).thenReturn("PROTO");
        when(pidProvider.getSeparator()).thenReturn("PROTO");
        when(pidProvider.getLabel()).thenReturn("PROTO");
        when(pidProvider.getId()).thenReturn("PROTO");
        when(pidProvider.registerWhenPublished()).thenReturn(false);
        // register the provider
        PidUtil.addToProviderList(pidProvider);

        GlobalId pid = new GlobalId(pidProvider.getProtocol(),pidProvider.getAuthority(),"OLD-ID", pidProvider.getSeparator(),pidProvider.getUrlPrefix(), pidProvider.getLabel());
        when(pidProvider.parsePersistentId(eq("PROTO"),eq("PROTO"),eq("OLD-ID"))).thenReturn(pid);

        // given a dataset linked with the PID provider
        Dataset ds = MocksFactory.makeDataset();
        ds.setGlobalId(pid);
        ds.setGlobalIdCreateTime(Timestamp.from(Instant.now()));
        ds.setIdentifierRegistered(true);

        // add file with PID to dataset
        ds.setFiles(ds.getFiles().subList(0,1));
        DataFile df = ds.getFiles().get(0);
        GlobalId piddf = new GlobalId(pidProvider.getProtocol(),pidProvider.getAuthority(),"OLD-DF-ID", pidProvider.getSeparator(),pidProvider.getUrlPrefix(), pidProvider.getLabel());
        when(pidProvider.parsePersistentId(eq("PROTO"),eq("PROTO"),eq("OLD-DF-ID"))).thenReturn(piddf);
        df.setGlobalId(piddf);
        when(pidProvider.alreadyRegistered(df)).thenReturn(true).thenReturn(false);
        when(pidProvider.alreadyRegistered(ds)).thenReturn(true);

        // given a linked dataset with a given dataverse
        Dataverse dv = MocksFactory.makeDataverse();
        ds.setOwner(dv);

        // given a new PID Provider
        PidProvider newPIDProviderMock = mock(PidProvider.class);
        when(newPIDProviderMock.getId()).thenReturn("PROTO1");
        when(newPIDProviderMock.getLabel()).thenReturn("PROTO1");
        when(newPIDProviderMock.getProtocol()).thenReturn("PROTO1");
        when(newPIDProviderMock.getAuthority()).thenReturn("PROTO1");
        when(newPIDProviderMock.getShoulder()).thenReturn("PROTO1");
        when(newPIDProviderMock.getSeparator()).thenReturn("PROTO1");
        when(newPIDProviderMock.registerWhenPublished()).thenReturn(false);
        when(newPIDProviderMock.canManagePID()).thenReturn(true);
        PidUtil.addToProviderList(newPIDProviderMock);

        // create new global IDs
        GlobalId pid2 = new GlobalId(newPIDProviderMock.getProtocol(), newPIDProviderMock.getAuthority(),"MYIDENT", newPIDProviderMock.getSeparator(), newPIDProviderMock.getUrlPrefix(), newPIDProviderMock.getLabel());
        when(newPIDProviderMock.parsePersistentId(eq("PROTO1"),eq("PROTO1"),eq("MYIDENT"))).thenReturn(pid2);
        when(newPIDProviderMock.generatePid(ds)).thenAnswer(new Answer<DvObject>() {
            public DvObject answer(InvocationOnMock invocation) {
                Dataset callback = (Dataset) invocation.getArguments()[0];
                callback.setAuthority(pid2.getAuthority());
                callback.setProtocol(pid2.getProtocol());
                callback.setIdentifier(pid2.getIdentifier());
                return callback;
            }
        });

        GlobalId piddf2 = new GlobalId(newPIDProviderMock.getProtocol(),newPIDProviderMock.getAuthority(),"NEW-DF-ID", newPIDProviderMock.getSeparator(),newPIDProviderMock.getUrlPrefix(), newPIDProviderMock.getLabel());
        when(newPIDProviderMock.parsePersistentId(eq("PROTO1"),eq("PROTO1"),eq("NEW-DF-ID"))).thenReturn(piddf2);
        when(newPIDProviderMock.generatePid(df)).thenAnswer(new Answer<DvObject>() {
            public DvObject answer(InvocationOnMock invocation) {
                DataFile callback = (DataFile) invocation.getArguments()[0];
                callback.setAuthority(piddf2.getAuthority());
                callback.setProtocol(piddf2.getProtocol());
                callback.setIdentifier(piddf2.getIdentifier());
                return callback;
            }
        });

        // mocks and stubs for notification
        RoleAssignment roleAssignment=new RoleAssignment();
        roleAssignment.setAssigneeIdentifier("MY_ID");
        when(drsb.directRoleAssignments(ds)).thenReturn(Lists.list(roleAssignment));
        when(roleAssigneeServiceBean.getRoleAssignee("MY_ID")).thenReturn(superUser);
        when(roleAssigneeServiceBean.getExplicitUsers(any())).thenReturn(List.of(superUser));

        // enable file PIDs for collection
        when(systemConfig.isFilePIDsEnabledForCollection(dv)).thenReturn(true);

        // run reconcile PID command
        ReconcileDatasetPidCommand sut = new ReconcileDatasetPidCommand(request, ds, newPIDProviderMock);
        engine.submit(sut);

        // then the global identifier should no longer be the previous one
        assertNotEquals(pid,ds.getGlobalId());
        // but it should be the newly generated one
        assertEquals(pid2,ds.getGlobalId());
        // which should also be registered and should have a create timestamp
        assertEquals(true,ds.isIdentifierRegistered());
        assertNotEquals(null,ds.getGlobalIdCreateTime());

        // further, the old one should be available as alternative id
        assertEquals(ds.getAlternativePersistentIndentifiers().size(),1);
        assertEquals(ds.getAlternativePersistentIndentifiers().stream().findFirst().get().getIdentifier(),"OLD-ID");
        // same for the file PID
        assertNotEquals(piddf, df.getGlobalId());
        assertEquals(piddf2, df.getGlobalId());
        assertEquals(true, df.isIdentifierRegistered());
        assertNotEquals(null, ds.getGlobalIdCreateTime());
        assertEquals(df.getAlternativePersistentIndentifiers().size(),1);
        assertEquals(df.getAlternativePersistentIndentifiers().stream().findFirst().get().getIdentifier(),"OLD-DF-ID");

        // further, we should be sure that the user is notified about the change
        verify(notificationService, times(1)).sendNotification(eq(superUser),any(),eq(UserNotification.Type.PIDRECONCILED),any(),any());
        // and that the changes are reflected into the search index
        verify(indexServiceBean, times(1)).asyncIndexDataset(ds,true);
        // and that the old identifier is deleted
        verify(pidProvider, times(1)).deleteIdentifier(ds);
        verify(pidProvider, times(1)).deleteIdentifier(df);

        // clean up PID provider list
        PidUtil.clearPidProviders();
    }

    @Test
    public void testSuccessfulChangeWithFilePIDs_existing_altID_for_file() throws Exception {
        // given a user that invokes a command request
        AuthenticatedUser superUser = new AuthenticatedUser();
        superUser.setSuperuser(true);
        DataverseRequest request = MocksFactory.makeRequest(superUser);

        // given a simple PID Provider
        PidProvider pidProvider = mock(PidProvider.class);
        when(pidProvider.getProtocol()).thenReturn("PROTO");
        when(pidProvider.getAuthority()).thenReturn("PROTO");
        when(pidProvider.getShoulder()).thenReturn("PROTO");
        when(pidProvider.getSeparator()).thenReturn("PROTO");
        when(pidProvider.getLabel()).thenReturn("PROTO");
        when(pidProvider.getId()).thenReturn("PROTO");
        when(pidProvider.registerWhenPublished()).thenReturn(false);
        PidUtil.addToProviderList(pidProvider);

        GlobalId pid = new GlobalId(pidProvider.getProtocol(),pidProvider.getAuthority(),"OLD-ID", pidProvider.getSeparator(),pidProvider.getUrlPrefix(), pidProvider.getLabel());
        when(pidProvider.parsePersistentId(eq("PROTO"),eq("PROTO"),eq("OLD-ID"))).thenReturn(pid);

        // given a dataset linked with the PID provider
        Dataset ds = MocksFactory.makeDataset();
        ds.setGlobalId(pid);
        ds.setGlobalIdCreateTime(Timestamp.from(Instant.now()));
        ds.setIdentifierRegistered(true);

        // add file with a PID and an alternative ID to dataset
        ds.setFiles(ds.getFiles().subList(0,1));
        DataFile df = ds.getFiles().get(0);
        GlobalId piddf = new GlobalId(pidProvider.getProtocol(),pidProvider.getAuthority(),"OLD-DF-ID", pidProvider.getSeparator(),pidProvider.getUrlPrefix(), pidProvider.getLabel());
        when(pidProvider.parsePersistentId(eq("PROTO"),eq("PROTO"),eq("OLD-DF-ID"))).thenReturn(piddf);
        df.setGlobalId(piddf);

        AlternativePersistentIdentifier aip = new AlternativePersistentIdentifier();
        aip.setStorageLocationDesignator(true);
        HashSet<AlternativePersistentIdentifier> aip_set = new HashSet<>();
        aip_set.add(aip);
        df.setAlternativePersistentIndentifiers(aip_set);

        when(pidProvider.alreadyRegistered(df)).thenReturn(true).thenReturn(false);
        when(pidProvider.alreadyRegistered(ds)).thenReturn(true);

        // given a linked dataset with a given dataverse
        Dataverse dv = MocksFactory.makeDataverse();
        ds.setOwner(dv);

        // given a new PID Provider
        PidProvider newPIDProviderMock = mock(PidProvider.class);
        when(newPIDProviderMock.getId()).thenReturn("PROTO1");
        when(newPIDProviderMock.getLabel()).thenReturn("PROTO1");
        when(newPIDProviderMock.getProtocol()).thenReturn("PROTO1");
        when(newPIDProviderMock.getAuthority()).thenReturn("PROTO1");
        when(newPIDProviderMock.getShoulder()).thenReturn("PROTO1");
        when(newPIDProviderMock.getSeparator()).thenReturn("PROTO1");
        when(newPIDProviderMock.registerWhenPublished()).thenReturn(false);
        when(newPIDProviderMock.canManagePID()).thenReturn(true);
        PidUtil.addToProviderList(newPIDProviderMock);

        // create new global IDs
        GlobalId pid2 = new GlobalId(newPIDProviderMock.getProtocol(), newPIDProviderMock.getAuthority(),"MYIDENT", newPIDProviderMock.getSeparator(), newPIDProviderMock.getUrlPrefix(), newPIDProviderMock.getLabel());
        when(newPIDProviderMock.parsePersistentId(eq("PROTO1"),eq("PROTO1"),eq("MYIDENT"))).thenReturn(pid2);
        when(newPIDProviderMock.generatePid(ds)).thenAnswer(new Answer<DvObject>() {
            public DvObject answer(InvocationOnMock invocation) {
                Dataset callback = (Dataset) invocation.getArguments()[0];
                callback.setAuthority(pid2.getAuthority());
                callback.setProtocol(pid2.getProtocol());
                callback.setIdentifier(pid2.getIdentifier());
                return callback;
            }
        });

        GlobalId piddf2 = new GlobalId(newPIDProviderMock.getProtocol(),newPIDProviderMock.getAuthority(),"NEW-DF-ID", newPIDProviderMock.getSeparator(),newPIDProviderMock.getUrlPrefix(), newPIDProviderMock.getLabel());
        when(newPIDProviderMock.parsePersistentId(eq("PROTO1"),eq("PROTO1"),eq("NEW-DF-ID"))).thenReturn(piddf2);
        when(newPIDProviderMock.generatePid(df)).thenAnswer(new Answer<DvObject>() {
            public DvObject answer(InvocationOnMock invocation) {
                DataFile callback = (DataFile) invocation.getArguments()[0];
                callback.setAuthority(piddf2.getAuthority());
                callback.setProtocol(piddf2.getProtocol());
                callback.setIdentifier(piddf2.getIdentifier());
                return callback;
            }
        });

        // mocks and stubs for notification
        RoleAssignment roleAssignment=new RoleAssignment();
        roleAssignment.setAssigneeIdentifier("MY_ID");
        when(drsb.directRoleAssignments(ds)).thenReturn(Lists.list(roleAssignment));
        when(roleAssigneeServiceBean.getRoleAssignee("MY_ID")).thenReturn(superUser);
        when(roleAssigneeServiceBean.getExplicitUsers(any())).thenReturn(List.of(superUser));

        // enable file PIDs for collection
        when(systemConfig.isFilePIDsEnabledForCollection(dv)).thenReturn(true);

        // run reconcile PID command
        ReconcileDatasetPidCommand sut = new ReconcileDatasetPidCommand(request, ds, newPIDProviderMock);
        engine.submit(sut);

        // then the global identifier should no longer be the previous one
        assertNotEquals(pid, ds.getGlobalId());
        // but it should be the newly generated one
        assertEquals(pid2, ds.getGlobalId());
        // which should also be registered and should have a create timestamp
        assertEquals(true, ds.isIdentifierRegistered());
        assertNotEquals(null, ds.getGlobalIdCreateTime());

        // further, the old one should be available as alternative id
        assertEquals(ds.getAlternativePersistentIndentifiers().size(),1);
        assertEquals(ds.getAlternativePersistentIndentifiers().stream().findFirst().get().getIdentifier(),"OLD-ID");
        // same for the file PID
        assertNotEquals(piddf, df.getGlobalId());
        assertEquals(piddf2, df.getGlobalId());
        assertEquals(true, df.isIdentifierRegistered());
        assertNotEquals(null, ds.getGlobalIdCreateTime());
        assertEquals(df.getAlternativePersistentIndentifiers().size(),2);
        assertEquals(ds.getAlternativePersistentIndentifiers().stream().filter(s->s.isStorageLocationDesignator()).count(),1);
        assertEquals(df.getAlternativePersistentIndentifiers().stream().filter(s->s.getIdentifier()=="OLD-DF-ID").count(),1);

        // further, we should be sure that the user is notified about the change
        verify(notificationService, times(1)).sendNotification(eq(superUser),any(),eq(UserNotification.Type.PIDRECONCILED),any(),any());
        // and that the changes are reflected into the search index
        verify(indexServiceBean, times(1)).asyncIndexDataset(ds,true);
        // and that the old identifier is deleted
        verify(pidProvider, times(1)).deleteIdentifier(ds);
        verify(pidProvider, times(1)).deleteIdentifier(df);

        // clean up PID provider list
        PidUtil.clearPidProviders();
    }

    @Test
    public void testWithFileWithoutGlobalID() throws Exception {
        // given a user that invokes a command request
        AuthenticatedUser superUser = new AuthenticatedUser();
        superUser.setSuperuser(true);
        DataverseRequest request = MocksFactory.makeRequest(superUser);

        // given a simple PID Provider
        PidProvider pidProvider = mock(PidProvider.class);
        when(pidProvider.getProtocol()).thenReturn("PROTO");
        when(pidProvider.getAuthority()).thenReturn("PROTO");
        when(pidProvider.getShoulder()).thenReturn("PROTO");
        when(pidProvider.getSeparator()).thenReturn("PROTO");
        when(pidProvider.getLabel()).thenReturn("PROTO");
        when(pidProvider.getId()).thenReturn("PROTO");
        when(pidProvider.registerWhenPublished()).thenReturn(false);
        PidUtil.addToProviderList(pidProvider);

        GlobalId pid = new GlobalId(pidProvider.getProtocol(),pidProvider.getAuthority(),"OLD-ID", pidProvider.getSeparator(),pidProvider.getUrlPrefix(), pidProvider.getLabel());
        when(pidProvider.parsePersistentId(eq("PROTO"),eq("PROTO"),eq("OLD-ID"))).thenReturn(pid);

        // given a dataset linked with the PID provider
        Dataset ds = MocksFactory.makeDataset();
        ds.setGlobalId(pid);
        ds.setGlobalIdCreateTime(Timestamp.from(Instant.now()));
        ds.setIdentifierRegistered(true);

        // add file without global ID to dataset
        ds.setFiles(ds.getFiles().subList(0,1));
        DataFile df = ds.getFiles().get(0);
        when(pidProvider.alreadyRegistered(df)).thenReturn(true).thenReturn(false);
        when(pidProvider.alreadyRegistered(ds)).thenReturn(true);

        // given a linked dataset with a given dataverse
        Dataverse dv = MocksFactory.makeDataverse();
        ds.setOwner(dv);

        // given a new PID Provider
        PidProvider newPIDProviderMock = mock(PidProvider.class);
        when(newPIDProviderMock.getId()).thenReturn("PROTO1");
        when(newPIDProviderMock.getLabel()).thenReturn("PROTO1");
        when(newPIDProviderMock.getProtocol()).thenReturn("PROTO1");
        when(newPIDProviderMock.getAuthority()).thenReturn("PROTO1");
        when(newPIDProviderMock.getShoulder()).thenReturn("PROTO1");
        when(newPIDProviderMock.getSeparator()).thenReturn("PROTO1");
        when(newPIDProviderMock.registerWhenPublished()).thenReturn(false);
        when(newPIDProviderMock.canManagePID()).thenReturn(true);
        PidUtil.addToProviderList(newPIDProviderMock);

        // create new global IDs
        GlobalId pid2 = new GlobalId(newPIDProviderMock.getProtocol(), newPIDProviderMock.getAuthority(),"MYIDENT", newPIDProviderMock.getSeparator(), newPIDProviderMock.getUrlPrefix(), newPIDProviderMock.getLabel());
        when(newPIDProviderMock.parsePersistentId(eq("PROTO1"),eq("PROTO1"),eq("MYIDENT"))).thenReturn(pid2);
        when(newPIDProviderMock.generatePid(ds)).thenAnswer(new Answer<DvObject>() {
            public DvObject answer(InvocationOnMock invocation) {
                Dataset callback = (Dataset) invocation.getArguments()[0];
                callback.setAuthority(pid2.getAuthority());
                callback.setProtocol(pid2.getProtocol());
                callback.setIdentifier(pid2.getIdentifier());
                return callback;
            }
        });

        GlobalId piddf2 = new GlobalId(newPIDProviderMock.getProtocol(),newPIDProviderMock.getAuthority(),"NEW-DF-ID", newPIDProviderMock.getSeparator(),newPIDProviderMock.getUrlPrefix(), newPIDProviderMock.getLabel());
        when(newPIDProviderMock.parsePersistentId(eq("PROTO1"),eq("PROTO1"),eq("NEW-DF-ID"))).thenReturn(piddf2);
        when(newPIDProviderMock.generatePid(df)).thenAnswer(new Answer<DvObject>() {
            public DvObject answer(InvocationOnMock invocation) {
                DataFile callback = (DataFile) invocation.getArguments()[0];
                callback.setAuthority(piddf2.getAuthority());
                callback.setProtocol(piddf2.getProtocol());
                callback.setIdentifier(piddf2.getIdentifier());
                return callback;
            }
        });

        // mocks and stubs for notification
        RoleAssignment roleAssignment=new RoleAssignment();
        roleAssignment.setAssigneeIdentifier("MY_ID");
        when(drsb.directRoleAssignments(ds)).thenReturn(Lists.list(roleAssignment));
        when(roleAssigneeServiceBean.getRoleAssignee("MY_ID")).thenReturn(superUser);
        when(roleAssigneeServiceBean.getExplicitUsers(any())).thenReturn(List.of(superUser));

        // enable file PIDs for collection
        when(systemConfig.isFilePIDsEnabledForCollection(dv)).thenReturn(true);

        // run reconcile PID command
        ReconcileDatasetPidCommand sut = new ReconcileDatasetPidCommand(request, ds, newPIDProviderMock);
        engine.submit(sut);

        // then the global identifier should no longer be the previous one
        assertNotEquals(pid, ds.getGlobalId());
        // but it should be the newly generated one
        assertEquals(pid2, ds.getGlobalId());
        // which should also be registered and should have a create timestamp
        assertTrue(ds.isIdentifierRegistered());
        assertNotEquals(null, ds.getGlobalIdCreateTime());

        // further, the old one should be available as alternative id
        assertEquals(ds.getAlternativePersistentIndentifiers().size(),1);
        assertEquals(ds.getAlternativePersistentIndentifiers().stream().findFirst().get().getIdentifier(),"OLD-ID");

        // file should still not have a PID
        assertNull(df.getGlobalId());
        assertNull(df.getAlternativePersistentIndentifiers());

        // further, we should be sure that the user is notified about the change
        verify(notificationService, times(1)).sendNotification(eq(superUser),any(),eq(UserNotification.Type.PIDRECONCILED),any(),any());
        // and that the changes are reflected into the search index
        verify(indexServiceBean, times(1)).asyncIndexDataset(ds,true);
        // and that the old identifier is deleted
        verify(pidProvider, times(1)).deleteIdentifier(ds);
        verify(pidProvider, times(0)).deleteIdentifier(df);

        // clean up PID provider list
        PidUtil.clearPidProviders();
    }

    @Test
    public void testGuardNoOp() throws CommandException {
        // given a user
        AuthenticatedUser superUser = new AuthenticatedUser();
        superUser.setSuperuser(true);
        DataverseRequest request = MocksFactory.makeRequest(superUser);

        // given a PID Provider
        PidProvider pidProvider = mock(PidProvider.class);
        when(pidProvider.getProtocol()).thenReturn("PROTO");
        when(pidProvider.getAuthority()).thenReturn("PROTO");
        when(pidProvider.getShoulder()).thenReturn("PROTO");
        when(pidProvider.getSeparator()).thenReturn("PROTO");
        when(pidProvider.getLabel()).thenReturn("PROTO");
        when(pidProvider.getId()).thenReturn("PROTO");
        when(pidProvider.registerWhenPublished()).thenReturn(true);
        PidUtil.addToProviderList(pidProvider);

        // given a dataset linked with a PidProvider and corresponding globalId
        Dataset ds = MocksFactory.makeDataset();

        // given a linked dataset with a given dataverse
        Dataverse dv = MocksFactory.makeDataverse();
        ds.setOwner(dv);
        GlobalId pid = new GlobalId(pidProvider.getProtocol(),pidProvider.getAuthority(),"OLD-ID", pidProvider.getSeparator(),pidProvider.getUrlPrefix(), pidProvider.getLabel());
        when(pidProvider.parsePersistentId(eq("PROTO"),eq("PROTO"),eq("OLD-ID"))).thenReturn(pid);
        ds.setGlobalId(pid);

        // run reconcile PID command
        ReconcileDatasetPidCommand sut = new ReconcileDatasetPidCommand(request, ds, pidProvider);

        // expect an error to be thrown
        assertThrows(IllegalCommandException.class, () -> engine.submit(sut),"Should throw ICE when no change");

        // clean up PID provider list
        PidUtil.clearPidProviders();
    }

    @Test
    public void testGuardAlreadyPublished() throws CommandException {
        // given a user
        AuthenticatedUser superUser = new AuthenticatedUser();
        superUser.setSuperuser(true);
        DataverseRequest request = MocksFactory.makeRequest(superUser);

        // given a PID Provider
        PidProvider pidProvider = mock(PidProvider.class);
        when(pidProvider.getProtocol()).thenReturn("PROTO");
        when(pidProvider.getAuthority()).thenReturn("PROTO");
        when(pidProvider.getShoulder()).thenReturn("PROTO");
        when(pidProvider.getSeparator()).thenReturn("PROTO");
        when(pidProvider.getLabel()).thenReturn("PROTO");
        when(pidProvider.registerWhenPublished()).thenReturn(true);
        PidUtil.addToProviderList(pidProvider);

        // given a dataset linked with the PID provider
        Dataset ds = MocksFactory.makeDataset();

        // make dataset published
        ds.setPublicationDate(Timestamp.from(Instant.now()));

        // given a linked dataset with a given dataverse
        Dataverse dv = MocksFactory.makeDataverse();
        ds.setOwner(dv);

        // run reconcile PID command
        ReconcileDatasetPidCommand sut = new ReconcileDatasetPidCommand(request, ds, pidProvider);

        // expect an error to be thrown
        assertThrows(IllegalCommandException.class, () -> engine.submit(sut),"Should throw ICE when already published");

        // clean up PID provider list
        PidUtil.clearPidProviders();
    }

    @Test
    public void testGuardHarvested() throws CommandException {
        // given a user
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
        PidUtil.addToProviderList(pidProvider);

        // given a dataset linked with the PID provider
        Dataset ds = MocksFactory.makeDataset();

        // make dataset harvested
        ds.setHarvestedFrom(mock(HarvestingClient.class));

        // given a linked dataset with a given dataverse
        Dataverse dv = MocksFactory.makeDataverse();
        ds.setOwner(dv);

        // run reconcile PID command
        ReconcileDatasetPidCommand sut = new ReconcileDatasetPidCommand(request, ds, pidProvider);

        // expect an error to be thrown
        assertThrows(IllegalCommandException.class, () -> engine.submit(sut),"Should throw ICE when harvested");

        // clean up PID provider list
        PidUtil.clearPidProviders();
    }

}
