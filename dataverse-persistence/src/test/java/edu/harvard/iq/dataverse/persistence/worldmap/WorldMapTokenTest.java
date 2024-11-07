/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.persistence.worldmap;

import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author raprasad
 */
public class WorldMapTokenTest {

    private TokenApplicationType makeTokenApplicationType(int timeLimitMinutes) {
        TokenApplicationType tat = new TokenApplicationType();
        tat.setName("GeoConnect");
        tat.setContactEmail("info@iq.harvard.edu");
        tat.setHostname("geoconnect.datascience.iq.harvard.edu");
        tat.setIpAddress("127.0.0.1");
        tat.setTimeLimitMinutes(timeLimitMinutes);
        return tat;
    }

    private WorldMapToken makeNewToken(TokenApplicationType tat) {
        WorldMapToken token;
        token = new WorldMapToken();
        token.setApplication(tat);
        token.setDatafile(new DataFile());
        token.setDataverseUser(new AuthenticatedUser());
        token.refreshToken();
        token.setToken();
        return token;
    }

    @Tag("NonEssentialTests")
    @Test
    public void testTokenValues() {
        TokenApplicationType tat = this.makeTokenApplicationType(30);

        WorldMapToken token = this.makeNewToken(tat);
        String token_str1 = token.getToken();

        // Should only be able to set token value once--it doesn't "reset"
        token.setToken();
        assertEquals(token.getToken().equalsIgnoreCase(token_str1), true);


        WorldMapToken token2 = this.makeNewToken(tat);
        WorldMapToken token3 = this.makeNewToken(tat);
        assertEquals(token2.getToken().equalsIgnoreCase(token.getToken()), false);
        assertEquals(token2.getToken().equalsIgnoreCase(token3.getToken()), false);
    }

    @Tag("NonEssentialTests")
    @Test
    public void testTokenTimes() {

        TokenApplicationType tat = this.makeTokenApplicationType(30);

        assertEquals(30 * 60, tat.getTimeLimitSeconds());
        tat.setTimeLimitMinutes(1);
        assertEquals(1 * 60, tat.getTimeLimitSeconds());

        tat.setTimeLimitMinutes(30);
        WorldMapToken token = this.makeNewToken(tat);
        assertEquals(token.hasTokenExpired(), false);

        assertEquals(token.hasTokenExpired(getFutureTimeStamp(10)), false);
        assertEquals(token.hasTokenExpired(), false);
        assertEquals(token.hasTokenExpired(getFutureTimeStamp(30)), false);
        assertEquals(token.hasTokenExpired(getFutureTimeStamp(31)), true);
        assertEquals(token.hasTokenExpired(getFutureTimeStamp(45)), true);
        token.getApplication().setTimeLimitMinutes(10);
        assertEquals(token.getApplication().getTimeLimitMinutes(), 10);
        assertEquals(token.hasTokenExpired(null), true);
        token.refreshToken();
        assertEquals(token.hasTokenExpired(getFutureTimeStamp(5)), true);
        assertEquals(token.hasTokenExpired(), true);
        WorldMapToken token2 = this.makeNewToken(tat);
        assertEquals(token2.hasTokenExpired(), false);
        token2.setHasExpired(true);
        assertEquals(token2.hasTokenExpired(getFutureTimeStamp(1)), true);


    }

    private Timestamp getFutureTimeStamp(int futuremMinutes) {

        long ONE_MINUTE_IN_MILLIS = 60000;//millisecs
        long currentTimeMillisec = new Date().getTime();
        long inFutureMinutesMillisec = currentTimeMillisec + (futuremMinutes * ONE_MINUTE_IN_MILLIS);
        return new Timestamp(new Date(inFutureMinutesMillisec).getTime());

    }
}
