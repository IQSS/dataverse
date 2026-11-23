package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.engine.TestCommandContext;
import edu.harvard.iq.dataverse.harvest.client.HarvestingClient;
import edu.harvard.iq.dataverse.mocks.MocksFactory;
import edu.harvard.iq.dataverse.pidproviders.PidProvider;
import edu.harvard.iq.dataverse.pidproviders.PidUtil;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class DeaccessionDatasetVersionCommandTest {

    private static final String PROVIDER_ID = "test";
    private static final String UNKNOWN_PROVIDER_ID = "unknown";

    IndexServiceBean indexService;
    TestCommandContext ctxt;
    PidProvider pidProvider;

    Dataset dataset;
    DatasetVersion version;

    @BeforeEach
    public void setUp() {
        indexService = mock(IndexServiceBean.class);
        ctxt = new TestCommandContext() {
            @Override
            public IndexServiceBean index() {
                return indexService;
            }
        };

        // given a PID provider that manages all PIDs, except those starting with "UNKNOWN"
        pidProvider = mock(PidProvider.class);
        when(pidProvider.getId()).thenReturn(PROVIDER_ID);
        when(pidProvider.parsePersistentId(anyString(), anyString(), anyString())).thenAnswer(invocation -> {
            String identifier = invocation.getArgument(2);
            String providerId = identifier.startsWith("UNKNOWN") ? UNKNOWN_PROVIDER_ID : PROVIDER_ID;
            return new GlobalId(invocation.getArgument(0), invocation.getArgument(1), identifier, "/",
                    "https://doi.org/", providerId);
        });
        PidUtil.addToProviderList(pidProvider);

        // given a dataset with a registered PID and a (now) deaccessioned version
        dataset = new Dataset();
        dataset.setId(MocksFactory.nextId());
        setPid(dataset, "DS", true);
        version = dataset.getLatestVersion();
        version.setVersionState(DatasetVersion.VersionState.DEACCESSIONED);
    }

    @AfterEach
    public void tearDown() {
        PidUtil.clearPidProviders();
    }

    @Test
    public void testOnSuccessUpdatesPidsOfDatasetAndFilesInVersion() throws Exception {
        DataFile registeredFile = addFile(version, "FILE1", true);
        DataFile unregisteredFile = addFile(version, "FILE2", false);
        DataFile fileWithoutPid = addFile(version, null, false);
        DataFile fileWithUnknownProvider = addFile(version, "UNKNOWN", true);
        // a file of the dataset that is not part of the deaccessioned version
        DataFile otherFile = addFile(new DatasetVersion(), "FILE3", true);

        assertTrue(makeCommand().onSuccess(ctxt, version));

        verify(indexService).asyncIndexDataset(dataset, true);
        verify(pidProvider).deaccessionIdentifier(dataset);
        verify(pidProvider).deaccessionIdentifier(registeredFile);
        verify(pidProvider, never()).deaccessionIdentifier(unregisteredFile);
        verify(pidProvider, never()).deaccessionIdentifier(fileWithoutPid);
        verify(pidProvider, never()).deaccessionIdentifier(fileWithUnknownProvider);
        verify(pidProvider, never()).deaccessionIdentifier(otherFile);
    }

    @Test
    public void testOnSuccessSkipsHarvestedDatasets() throws Exception {
        addFile(version, "FILE1", true);
        dataset.setHarvestedFrom(new HarvestingClient());

        assertTrue(makeCommand().onSuccess(ctxt, version));

        verify(indexService).asyncIndexDataset(dataset, true);
        verify(pidProvider, never()).deaccessionIdentifier(any());
    }

    @Test
    public void testOnSuccessIgnoresPidProviderFailures() throws Exception {
        DataFile registeredFile = addFile(version, "FILE1", true);
        doThrow(new Exception("PID provider unavailable")).when(pidProvider).deaccessionIdentifier(any());

        assertTrue(makeCommand().onSuccess(ctxt, version));

        // a failure for the dataset doesn't stop the files from being updated
        verify(pidProvider).deaccessionIdentifier(dataset);
        verify(pidProvider).deaccessionIdentifier(registeredFile);
    }

    @Test
    public void testOnSuccessWithProviderWithoutDeaccessionSupport() throws Exception {
        // providers that don't override deaccessionIdentifier get the no-op default
        doCallRealMethod().when(pidProvider).deaccessionIdentifier(any());

        assertTrue(makeCommand().onSuccess(ctxt, version));

        verify(pidProvider).deaccessionIdentifier(dataset);
    }

    private DeaccessionDatasetVersionCommand makeCommand() {
        return new DeaccessionDatasetVersionCommand(MocksFactory.makeRequest(), version);
    }

    private DataFile addFile(DatasetVersion datasetVersion, String identifier, boolean registered) {
        DataFile dataFile = new DataFile();
        // DvObjects are equal when their ids are, so give each file its own
        dataFile.setId(MocksFactory.nextId());
        dataFile.setOwner(dataset);
        dataset.getFiles().add(dataFile);
        if (identifier != null) {
            setPid(dataFile, identifier, registered);
        }
        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setDataFile(dataFile);
        fileMetadata.setDatasetVersion(datasetVersion);
        dataFile.setFileMetadatas(new ArrayList<>(List.of(fileMetadata)));
        datasetVersion.getFileMetadatas().add(fileMetadata);
        return dataFile;
    }

    private static void setPid(DvObject dvObject, String identifier, boolean registered) {
        dvObject.setProtocol("doi");
        dvObject.setAuthority("10.5072");
        dvObject.setIdentifier(identifier);
        dvObject.setIdentifierRegistered(registered);
    }
}
