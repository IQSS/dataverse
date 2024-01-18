package edu.harvard.iq.dataverse.cache;

import ai.grakn.redismock.RedisServer;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
public class CacheFactoryBeanTest {

    @Mock
    SystemConfig systemConfig;
    @InjectMocks
    CacheFactoryBean cache = new CacheFactoryBean();
    AuthenticatedUser authUser = new AuthenticatedUser();
    String action;
    static RedisServer mockRedisServer;

    @BeforeEach
    public void setup() throws IOException {
        lenient().doReturn(mockRedisServer.getHost()).when(systemConfig).getRedisBaseHost();
        lenient().doReturn(String.valueOf(mockRedisServer.getBindPort())).when(systemConfig).getRedisBasePort();
        lenient().doReturn(null).when(systemConfig).getRedisUser();
        lenient().doReturn(null).when(systemConfig).getRedisPassword();
        lenient().doReturn(30).when(systemConfig).getIntFromCSVStringOrDefault(any(),eq(0), anyInt());
        lenient().doReturn(60).when(systemConfig).getIntFromCSVStringOrDefault(any(),eq(1), anyInt());
        lenient().doReturn(120).when(systemConfig).getIntFromCSVStringOrDefault(any(),eq(2), anyInt());
        lenient().doReturn("").when(systemConfig).getRateLimitsJson();

        cache.init();
        authUser.setRateLimitTier(1); // reset to default
        action = "cmd-" + UUID.randomUUID();
    }
    @BeforeAll
    public static void init() throws IOException {
        mockRedisServer = RedisServer.newRedisServer();
        mockRedisServer.start();
    }
    @AfterAll
    public static void cleanup() {
        if (mockRedisServer != null)
            mockRedisServer.stop();
    }
    @Test
    public void testGuestUserGettingRateLimited() throws InterruptedException {
        User user = GuestUser.get();
        boolean rateLimited = false;
        int cnt = 0;
        for (; cnt <100; cnt++) {
            rateLimited = !cache.checkRate(user, action);
            if (rateLimited) {
                break;
            }
        }
        assertTrue(rateLimited && cnt > 1 && cnt <= 30);
    }

    @Test
    public void testAdminUserExemptFromGettingRateLimited() throws InterruptedException {
        authUser.setSuperuser(true);
        authUser.setUserIdentifier("admin");

        boolean rateLimited = false;
        int cnt = 0;
        for (; cnt <100; cnt++) {
            rateLimited = !cache.checkRate(authUser, action);
            if (rateLimited) {
                break;
            }
        }
        assertTrue(!rateLimited && cnt >= 99);
    }

    @Test
    public void testAuthenticatedUserGettingRateLimited() throws InterruptedException {
        authUser.setSuperuser(false);
        authUser.setUserIdentifier("authUser");
        authUser.setRateLimitTier(2); // 120 cals per hour - 1 added token every 30 seconds
        boolean limited = false;
        int cnt;
        for (cnt = 0; cnt <200; cnt++) {
            limited = !cache.checkRate(authUser, action);
            if (limited) {
                break;
            }
        }
        assertTrue(limited && cnt == 120);

        for (cnt = 0; cnt <60; cnt++) {
            Thread.sleep(1000);// wait for bucket to be replenished (check each second for 1 minute max)
            limited = !cache.checkRate(authUser, action);
            if (!limited) {
                break;
            }
        }
        assertTrue(!limited);
    }

    @Test
    public void testAuthenticatedUserWithRateLimitingOff() throws InterruptedException {
        lenient().doReturn(RateLimitUtil.NO_LIMIT).when(systemConfig).getIntFromCSVStringOrDefault(any(),eq(1), anyInt());
        authUser.setSuperuser(false);
        authUser.setUserIdentifier("user1");
        boolean rateLimited = false;
        int cnt = 0;
        for (; cnt <100; cnt++) {
            rateLimited = !cache.checkRate(authUser, action);
            if (rateLimited) {
                break;
            }
        }
        assertTrue(!rateLimited && cnt > 99);
    }
}
