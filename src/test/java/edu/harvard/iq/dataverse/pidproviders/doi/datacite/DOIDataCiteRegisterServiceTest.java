package edu.harvard.iq.dataverse.pidproviders.doi.datacite;

import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import static org.mockito.Mockito.verify;

public class DOIDataCiteRegisterServiceTest {

    @Test
    public void testHideIdentifierMarksBareDoiInactive() {
        try (MockedConstruction<DataCiteRESTfullClient> clients = Mockito.mockConstruction(DataCiteRESTfullClient.class)) {
            DOIDataCiteRegisterService registerService = new DOIDataCiteRegisterService("https://mds.test.datacite.org",
                    "https://api.test.datacite.org", "username", "password");

            registerService.hideIdentifier("doi:10.5072/FK2/ABCDEF");

            verify(clients.constructed().get(0)).inactiveDataset("10.5072/FK2/ABCDEF");
        }
    }
}
