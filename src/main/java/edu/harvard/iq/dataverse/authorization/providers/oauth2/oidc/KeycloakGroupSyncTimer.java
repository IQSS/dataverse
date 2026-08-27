package edu.harvard.iq.dataverse.authorization.providers.oauth2.oidc;

import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.SystemConfig;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.Timeout;
import jakarta.ejb.Timer;
import jakarta.ejb.TimerConfig;
import jakarta.ejb.TimerService;

import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Periodically reconciles every tenant against Keycloak.
 * <p>
 * The login sync only sees people who log in. This timer is what catches the rest: someone
 * demoted in Keycloak who never comes back, a change made straight in the Keycloak console, a
 * tenant whose attribute was fixed after the fact. Its interval is therefore the installation's
 * worst-case delay for a permission change to take effect.
 * <p>
 * The timer is non-persistent, so it lives and dies with the deployment and never accumulates
 * across redeploys. In a cluster it only runs on the node designated as the timer server, the
 * same rule the harvesting and saved-search timers follow.
 */
@Singleton
@Startup
public class KeycloakGroupSyncTimer {

    private static final Logger logger = Logger.getLogger(KeycloakGroupSyncTimer.class.getName());

    private static final String TIMER_INFO = "KeycloakGroupSyncTimer";

    private static final int DEFAULT_INTERVAL_MINUTES = 15;

    @Resource
    TimerService timerService;

    @EJB
    KeycloakGroupSyncServiceBean syncService;

    @EJB
    SystemConfig systemConfig;

    @PostConstruct
    void init() {
        if (!syncService.isEnabled()) {
            return;
        }
        if (!systemConfig.isTimerServer()) {
            logger.info("Keycloak group sync is enabled but this node is not the timer server, "
                    + "so the reconciliation sweep will not run here.");
            return;
        }

        int minutes = JvmSettings.OIDC_SYNC_INTERVAL.lookupOptional(Integer.class)
                .orElse(DEFAULT_INTERVAL_MINUTES);
        if (minutes <= 0) {
            logger.info("Keycloak reconciliation sweep disabled (interval-minutes=" + minutes
                    + "). Authorizations will only be synchronised at login.");
            return;
        }

        long intervalMillis = Duration.ofMinutes(minutes).toMillis();
        // Wait a full interval before the first run: startup is busy enough already.
        timerService.createIntervalTimer(intervalMillis, intervalMillis,
                new TimerConfig(TIMER_INFO, false));
        logger.info("Keycloak reconciliation sweep scheduled every " + minutes + " minute(s).");
    }

    @Timeout
    public void run(Timer timer) {
        // Never let a failure escape: an exception out of a timeout callback can cost us the
        // timer, and then permissions would silently stop being reconciled.
        try {
            syncService.reconcileAll();
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Keycloak reconciliation sweep failed; will retry on the next run.", ex);
        }
    }
}
