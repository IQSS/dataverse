/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.worldmapauth;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.NonEssentialTests;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import java.sql.Timestamp;
import java.util.Date;
import javax.ejb.embeddable.EJBContainer;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 *
 * @author raprasad
 */
public class WorldMapTokenTest {
 
    private static EJBContainer c;

   
    public void msg(String s){
       System.out.println(s);
    }
 
    public void msgt(String s){
        msg("------------------------------------------------------------");
        msg(s);
        msg("------------------------------------------------------------");
    }

     
    private TokenApplicationType makeTokenApplicationType(int timeLimitMinutes){
        TokenApplicationType tat = new TokenApplicationType();
        tat.setName("GeoConnect");
        tat.setContactEmail("info@iq.harvard.edu");
        tat.setHostname("geoconnect.datascience.iq.harvard.edu");
        tat.setIpAddress("127.0.0.1");
        tat.setTimeLimitMinutes(timeLimitMinutes);
        return tat;
    }
    private WorldMapToken makeNewToken(TokenApplicationType tat){
        WorldMapToken token;
        token = new WorldMapToken();
        token.setApplication(tat);
        token.setDatafile(new DataFile());
        token.setDataverseUser(new AuthenticatedUser());
        token.refreshToken();
        token.setToken();        
        return token;
    }
    
    @Category(NonEssentialTests.class)
    @Test
    public void testTokenValues(){
        msgt("WorldMapTokenTest!");
        TokenApplicationType tat = this.makeTokenApplicationType(30);

        WorldMapToken token = this.makeNewToken(tat);
        String token_str1 = token.getToken();
 
        // Should only be able to set token value once--it doesn't "reset"
        token.setToken();        
        assertEquals(token.getToken().equalsIgnoreCase(token_str1), true);
   
        
        WorldMapToken token2  = this.makeNewToken(tat);
        WorldMapToken token3  = this.makeNewToken(tat);
        assertEquals(token2.getToken().equalsIgnoreCase(token.getToken()), false);
        assertEquals(token2.getToken().equalsIgnoreCase(token3.getToken()), false);
    }
    
    @Category(NonEssentialTests.class)
    @Test
    public void testTokenTimes(){
        msgt("testTokenTimes");
       
        TokenApplicationType tat = this.makeTokenApplicationType(30);

        assertEquals(30*60, tat.getTimeLimitSeconds());
        msg("time limit seconds: " + tat.getTimeLimitSeconds());
        tat.setTimeLimitMinutes(1);
        msg("time limit seconds (2): " + tat.getTimeLimitSeconds());
        assertEquals(1*60, tat.getTimeLimitSeconds());
        
        tat.setTimeLimitMinutes(30);
        WorldMapToken token = this.makeNewToken(tat);
        assertEquals(token.hasTokenExpired(), false);

        //msg("Future token time (31 min): " + getFutureTimeStamp(31));
        msg("token time limit (minutes): " + token.getApplication().getTimeLimitMinutes());
        msg("Did token expire in 10 minutes? (should be no)");
        //msg("expired? " +  token.hasTokenExpired(getFutureTimeStamp(10)));

        assertEquals(token.hasTokenExpired(getFutureTimeStamp(10)), false);

        msg("Did token expire? (automatically check current time)");
        assertEquals(token.hasTokenExpired(), false);

        msg("Did token expire at 30 minutes? (should be no)");
        assertEquals(token.hasTokenExpired(getFutureTimeStamp(30)), false);

        msg("Did token expire in 31 minutes? (should be yes)");
        assertEquals(token.hasTokenExpired(getFutureTimeStamp(31)), true);

        msg("Did token expire in 45 minutes? (should be yes)");
        assertEquals(token.hasTokenExpired(getFutureTimeStamp(45)), true);

        msg("token time limit (minutes): 10 minutes");
        token.getApplication().setTimeLimitMinutes(10);
        assertEquals(token.getApplication().getTimeLimitMinutes(), 10);

        msg("Did token expire if null sent? (should be yes)");
        assertEquals(token.hasTokenExpired(null), true);

        msg("Refresh token (but fails b/c sending null automatically expires it");
        token.refreshToken();
        msg("Did token expire in 5 minutes? (should be no)");
        assertEquals(token.hasTokenExpired(getFutureTimeStamp(5)), true);
        msg("Did token expire? (auto-check current time)");
        assertEquals(token.hasTokenExpired(), true);
        
        msgt("Get a new Token");
        WorldMapToken token2 = this.makeNewToken(tat);


        msg("Did token expire? (automatically check current time)");
        assertEquals(token2.hasTokenExpired(), false);

        msg("Manually expire token: setHasExpired(true)");
        token2.setHasExpired(true);
        msg("Did token expire in 1 minute? (should be yes--b/c manually expired)");
        assertEquals(token2.hasTokenExpired(getFutureTimeStamp(1)), true);
        
        
    }
    
    private Timestamp getFutureTimeStamp(int futuremMinutes){

        long ONE_MINUTE_IN_MILLIS=60000;//millisecs
        long currentTimeMillisec = new Date().getTime();
        long inFutureMinutesMillisec = currentTimeMillisec + (futuremMinutes * ONE_MINUTE_IN_MILLIS);
        return new Timestamp(new Date(inFutureMinutesMillisec).getTime());
        
    }
}
