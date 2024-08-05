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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        // a user that invokes a command request
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
        // given a dataset linked with the PIDprovider
        Dataset ds = MocksFactory.makeDataset();
        ds.setGlobalId(pid);
        ds.setGlobalIdCreateTime(Timestamp.from(Instant.now()));
        ds.setIdentifierRegistered(true);

        when(pidProvider.alreadyRegistered(ds)).thenReturn(true);
        // given a linked dataset with a givendataverse and
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
        // register the provider
        PidUtil.addToProviderList(newPIDProviderMock);
        // create new GloablID and register with the Utils.
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


        //when
        // disable file pids for collection
        when(systemConfig.isFilePIDsEnabledForCollection(dv)).thenReturn(false);

        ReconcileDatasetPidCommand sut = new ReconcileDatasetPidCommand(request, ds, newPIDProviderMock);
        engine.submit(sut);

        //then
        // the global identifier is no longer the previous one
        assertNotEquals(pid,ds.getGlobalId()); // must be different that old one!
        // but it should be the newly generated one.
        assertEquals(pid2,ds.getGlobalId());
        // should also be registered and should have a time stamp
        assertEquals(true,ds.isIdentifierRegistered());
        assertNotEquals(null,ds.getGlobalIdCreateTime());
        // further the old one should is available as alternative id
        assertEquals(ds.getAlternativePersistentIndentifiers().size(),1);
        assertEquals(ds.getAlternativePersistentIndentifiers().stream().findFirst().get().getIdentifier(),"OLD-ID");

        // further we should be sure that the user is notified about the change
        verify(notificationService,times(1)).sendNotification(eq(superUser),any(),eq(UserNotification.Type.PIDRECONCILED),any(),any());
        // and that the changed are reflected into the search index.
        verify(indexServiceBean,times(1)).asyncIndexDataset(ds,true);
        // and that old identifier is deleted
        verify(pidProvider,times(1)).deleteIdentifier(ds);

    }

    @Test
    public void testSucessfullChangeWithoutFilePIDs_existing_altID() throws Exception {
        // given
        // a user that invokes a command request
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
        // given a dataset linked with the PIDprovider
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
        // given a linked dataset with a givendataverse and
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
        // register the provider
        PidUtil.addToProviderList(newPIDProviderMock);
        // create new GloablID and register with the Utils.
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


        //when
        // disable file pids for collection
        when(systemConfig.isFilePIDsEnabledForCollection(dv)).thenReturn(false);

        ReconcileDatasetPidCommand sut = new ReconcileDatasetPidCommand(request, ds, newPIDProviderMock);
        engine.submit(sut);

        //then
        // the global identifier is no longer the previous one
        assertNotEquals(pid,ds.getGlobalId()); // must be different that old one!
        // but it should be the newly generated one.
        assertEquals(pid2,ds.getGlobalId());
        // should also be registered and should have a time stamp
        assertEquals(true,ds.isIdentifierRegistered());
        assertNotEquals(null,ds.getGlobalIdCreateTime());
        // further the old one should is available as alternative id
        assertEquals(ds.getAlternativePersistentIndentifiers().size(),2);
        assertEquals(ds.getAlternativePersistentIndentifiers().stream().filter(s->s.isStorageLocationDesignator()).count(),1);
        assertEquals(ds.getAlternativePersistentIndentifiers().stream().filter(s->s.getIdentifier()=="OLD-ID").count(),1);

        // further we should be sure that the user is notified about the change
        verify(notificationService,times(1)).sendNotification(eq(superUser),any(),eq(UserNotification.Type.PIDRECONCILED),any(),any());
        // and that the changed are reflected into the search index.
        verify(indexServiceBean,times(1)).asyncIndexDataset(ds,true);
        // and that old identifier is deleted
        verify(pidProvider,times(1)).deleteIdentifier(ds);

    }


    @Test
    public void testSucessfullChangeWithFilePIDs() throws Exception {
        // given
        // a user that invokes a command request
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

        // given a dataset linked with the PIDprovider
        Dataset ds = MocksFactory.makeDataset();
        ds.setGlobalId(pid);
        ds.setGlobalIdCreateTime(Timestamp.from(Instant.now()));
        ds.setIdentifierRegistered(true);
        // set file
        ds.setFiles(ds.getFiles().subList(0,1));
        DataFile df = ds.getFiles().get(0);
        GlobalId piddf = new GlobalId(pidProvider.getProtocol(),pidProvider.getAuthority(),"OLD-DF-ID", pidProvider.getSeparator(),pidProvider.getUrlPrefix(), pidProvider.getLabel());
        when(pidProvider.parsePersistentId(eq("PROTO"),eq("PROTO"),eq("OLD-DF-ID"))).thenReturn(piddf);
        df.setGlobalId(piddf);
        when(pidProvider.alreadyRegistered(df)).thenReturn(true).thenReturn(false);
        ///
        when(pidProvider.alreadyRegistered(ds)).thenReturn(true);
        // given a linked dataset with a givendataverse and
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
        // register the provider
        PidUtil.addToProviderList(newPIDProviderMock);
        // create new GloablID and register with the Utils.
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


        //when
        // disable file pids for collection
        when(systemConfig.isFilePIDsEnabledForCollection(dv)).thenReturn(true);

        ReconcileDatasetPidCommand sut = new ReconcileDatasetPidCommand(request, ds, newPIDProviderMock);
        engine.submit(sut);

        //then
        // the global identifier is no longer the previous one
        assertNotEquals(pid,ds.getGlobalId()); // must be different that old one!
        // but it should be the newly generated one.
        assertEquals(pid2,ds.getGlobalId());
        // should also be registered and should have a time stamp
        assertEquals(true,ds.isIdentifierRegistered());
        assertNotEquals(null,ds.getGlobalIdCreateTime());

        // further the old one should is available as alternative id
        assertEquals(ds.getAlternativePersistentIndentifiers().size(),1);
        assertEquals(ds.getAlternativePersistentIndentifiers().stream().findFirst().get().getIdentifier(),"OLD-ID");
        // same for file
        assertNotEquals(piddf,df.getGlobalId()); // must be different that old one!
        assertEquals(piddf2,df.getGlobalId());
        assertEquals(true,df.isIdentifierRegistered());
        assertNotEquals(null,ds.getGlobalIdCreateTime());
        assertEquals(df.getAlternativePersistentIndentifiers().size(),1);
        assertEquals(df.getAlternativePersistentIndentifiers().stream().findFirst().get().getIdentifier(),"OLD-DF-ID");

        // further we should be sure that the user is notified about the change
        verify(notificationService,times(1)).sendNotification(eq(superUser),any(),eq(UserNotification.Type.PIDRECONCILED),any(),any());
        // and that the changed are reflected into the search index.
        verify(indexServiceBean,times(1)).asyncIndexDataset(ds,true);
        // and that old identifier is deleted
        verify(pidProvider,times(1)).deleteIdentifier(ds);
        verify(pidProvider,times(1)).deleteIdentifier(df);
    }

    @Test
    public void testSucessfullChangeWithFilePIDs_existing_altID_for_file() throws Exception {
        // given
        // a user that invokes a command request
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

        // given a dataset linked with the PIDprovider
        Dataset ds = MocksFactory.makeDataset();
        ds.setGlobalId(pid);
        ds.setGlobalIdCreateTime(Timestamp.from(Instant.now()));
        ds.setIdentifierRegistered(true);
        // set file
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

        ///
        when(pidProvider.alreadyRegistered(ds)).thenReturn(true);
        // given a linked dataset with a givendataverse and
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
        // register the provider
        PidUtil.addToProviderList(newPIDProviderMock);
        // create new GloablID and register with the Utils.
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


        //when
        // disable file pids for collection
        when(systemConfig.isFilePIDsEnabledForCollection(dv)).thenReturn(true);

        ReconcileDatasetPidCommand sut = new ReconcileDatasetPidCommand(request, ds, newPIDProviderMock);
        engine.submit(sut);

        //then
        // the global identifier is no longer the previous one
        assertNotEquals(pid,ds.getGlobalId()); // must be different that old one!
        // but it should be the newly generated one.
        assertEquals(pid2,ds.getGlobalId());
        // should also be registered and should have a time stamp
        assertEquals(true,ds.isIdentifierRegistered());
        assertNotEquals(null,ds.getGlobalIdCreateTime());

        // further the old one should is available as alternative id
        assertEquals(ds.getAlternativePersistentIndentifiers().size(),1);
        assertEquals(ds.getAlternativePersistentIndentifiers().stream().findFirst().get().getIdentifier(),"OLD-ID");
        // same for file
        assertNotEquals(piddf,df.getGlobalId()); // must be different that old one!
        assertEquals(piddf2,df.getGlobalId());
        assertEquals(true,df.isIdentifierRegistered());
        assertNotEquals(null,ds.getGlobalIdCreateTime());
        assertEquals(df.getAlternativePersistentIndentifiers().size(),2);
        assertEquals(ds.getAlternativePersistentIndentifiers().stream().filter(s->s.isStorageLocationDesignator()).count(),1);
        assertEquals(df.getAlternativePersistentIndentifiers().stream().filter(s->s.getIdentifier()=="OLD-DF-ID").count(),1);

        // further we should be sure that the user is notified about the change
        verify(notificationService,times(1)).sendNotification(eq(superUser),any(),eq(UserNotification.Type.PIDRECONCILED),any(),any());
        // and that the changed are reflected into the search index.
        verify(indexServiceBean,times(1)).asyncIndexDataset(ds,true);
        // and that old identifier is deleted
        verify(pidProvider,times(1)).deleteIdentifier(ds);
        verify(pidProvider,times(1)).deleteIdentifier(df);
    }

    @Test
    public void testWithFileWihtoutGlobalID() throws Exception {
        // given
        // a user that invokes a command request
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

        // given a dataset linked with the PIDprovider
        Dataset ds = MocksFactory.makeDataset();
        ds.setGlobalId(pid);
        ds.setGlobalIdCreateTime(Timestamp.from(Instant.now()));
        ds.setIdentifierRegistered(true);
        // set file
        ds.setFiles(ds.getFiles().subList(0,1));
        DataFile df = ds.getFiles().get(0);
        //GlobalId piddf = new GlobalId(pidProvider.getProtocol(),pidProvider.getAuthority(),"OLD-DF-ID", pidProvider.getSeparator(),pidProvider.getUrlPrefix(), pidProvider.getLabel());
        //when(pidProvider.parsePersistentId(eq("PROTO"),eq("PROTO"),eq("OLD-DF-ID"))).thenReturn(piddf);
        //df.setGlobalId(piddf);
        when(pidProvider.alreadyRegistered(df)).thenReturn(true).thenReturn(false);
        ///
        when(pidProvider.alreadyRegistered(ds)).thenReturn(true);
        // given a linked dataset with a givendataverse and
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
        // register the provider
        PidUtil.addToProviderList(newPIDProviderMock);
        // create new GloablID and register with the Utils.
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


        //when
        // disable file pids for collection
        when(systemConfig.isFilePIDsEnabledForCollection(dv)).thenReturn(true);

        ReconcileDatasetPidCommand sut = new ReconcileDatasetPidCommand(request, ds, newPIDProviderMock);
        engine.submit(sut);

        //then
        // the global identifier is no longer the previous one
        assertNotEquals(pid,ds.getGlobalId()); // must be different that old one!
        // but it should be the newly generated one.
        assertEquals(pid2,ds.getGlobalId());
        // should also be registered and should have a time stamp
        assertEquals(true,ds.isIdentifierRegistered());
        assertNotEquals(null,ds.getGlobalIdCreateTime());

        // further the old one should is available as alternative id
        assertEquals(ds.getAlternativePersistentIndentifiers().size(),1);
        assertEquals(ds.getAlternativePersistentIndentifiers().stream().findFirst().get().getIdentifier(),"OLD-ID");
        // same for file
        //assertNotEquals(piddf,df.getGlobalId()); // must be different that old one!
        assertEquals(null,df.getGlobalId());
        assertEquals(df.getAlternativePersistentIndentifiers(),null);


        // further we should be sure that the user is notified about the change
        verify(notificationService,times(1)).sendNotification(eq(superUser),any(),eq(UserNotification.Type.PIDRECONCILED),any(),any());
        // and that the changed are reflected into the search index.
        verify(indexServiceBean,times(1)).asyncIndexDataset(ds,true);
        // and that old identifier is deleted
        verify(pidProvider,times(1)).deleteIdentifier(ds);
        verify(pidProvider,times(0)).deleteIdentifier(df);
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
        when(pidProvider.getLabel()).thenReturn("PROTO");
        when(pidProvider.getId()).thenReturn("PROTO");
        when(pidProvider.registerWhenPublished()).thenReturn(true);
        // register the provider
        PidUtil.addToProviderList(pidProvider);

        // given a dataset linked with a PidProvider and corresponding globalId
        Dataset ds = MocksFactory.makeDataset();

        // given a linked dataset with a givendataverse and
        Dataverse dv = MocksFactory.makeDataverse();
        ds.setOwner(dv);
        GlobalId pid = new GlobalId(pidProvider.getProtocol(),pidProvider.getAuthority(),"OLD-ID", pidProvider.getSeparator(),pidProvider.getUrlPrefix(), pidProvider.getLabel());
        when(pidProvider.parsePersistentId(eq("PROTO"),eq("PROTO"),eq("OLD-ID"))).thenReturn(pid);
        ds.setGlobalId(pid);

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
        when(pidProvider.getLabel()).thenReturn("PROTO");
        when(pidProvider.registerWhenPublished()).thenReturn(true);

        // given a dataset linked with the PIDprovider
        Dataset ds = MocksFactory.makeDataset();
        ds.setPublicationDate(Timestamp.from(Instant.now()));

        // given a linked dataset with a givendataverse and
        Dataverse dv = MocksFactory.makeDataverse();
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

        // given a linked dataset with a givendataverse and
        Dataverse dv = MocksFactory.makeDataverse();
        ds.setOwner(dv);

        ReconcileDatasetPidCommand sut = new ReconcileDatasetPidCommand(request, ds, pidProvider);
        assertThrows(IllegalCommandException.class, () -> engine.submit(sut),"Should throw ICE when already published");
    }

}
