
package edu.harvard.iq.dataverse.api;

import org.junit.jupiter.api.Test;
import static edu.harvard.iq.dataverse.workflow.internalspi.COARNotifyRelationshipAnnouncementStep.DATACITE_URI_PREFIX;
import io.restassured.response.Response;

import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;

public class LDNInboxIT {

    private static String datasetPid ="doi:10.5072/F2ABCDEF";

    //Simple test to assure that the default with no hosts allowed causes a message to get rejected.
    @Test
    public void testRejectMessageFromNonAllowedHost() {
        String message = LDNInboxTest.createRelationshipAnnouncementMessage("https://doi.org/10.1234/test", datasetPid,
                DATACITE_URI_PREFIX + "Cites");

        // Send the message - should be rejected
        Response response = UtilIT.sendMessageToLDNInbox(message);

        response.then().assertThat().statusCode(FORBIDDEN.getStatusCode());
    }
}