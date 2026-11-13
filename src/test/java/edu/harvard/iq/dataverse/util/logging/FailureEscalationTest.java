package edu.harvard.iq.dataverse.util.logging;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.OptionalInt;
import java.util.logging.Level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FailureEscalationTest {
    
    @Nested
    class DeactivatedEscalation {
        
        @ParameterizedTest
        @ValueSource(ints = {0, -5})
        void alwaysReturnsFine(int threshold) {
            FailureEscalation escalation = new FailureEscalation(threshold);
            
            assertEquals(Level.FINE, escalation.incrementAndGetLevel());
            assertEquals(Level.FINE, escalation.incrementAndGetLevel());
        }
        
        @Test
        void recordSuccessNeverReportsRecovery() {
            FailureEscalation escalation = new FailureEscalation(0);
            escalation.incrementAndGetLevel();
            escalation.incrementAndGetLevel();
            
            assertTrue(escalation.recordSuccess().isEmpty());
        }
        
        @Test
        void keepsNoBookkeeping() {
            FailureEscalation escalation = new FailureEscalation(0);
            escalation.incrementAndGetLevel();
            
            assertEquals(0, escalation.currentStreak());
        }
    }
    
    @Nested
    class EscalationThreshold {
        
        @Test
        void staysFineBelowThreshold() {
            FailureEscalation escalation = new FailureEscalation(3);
            
            assertEquals(Level.FINE, escalation.incrementAndGetLevel());
            assertEquals(Level.FINE, escalation.incrementAndGetLevel());
        }
        
        @Test
        void warnsExactlyAtThreshold() {
            FailureEscalation escalation = new FailureEscalation(3);
            escalation.incrementAndGetLevel();
            escalation.incrementAndGetLevel();
            
            assertEquals(Level.WARNING, escalation.incrementAndGetLevel());
        }
        
        @Test
        void thresholdOneWarnsOnFirstFailure() {
            FailureEscalation escalation = new FailureEscalation(1);
            
            assertEquals(Level.WARNING, escalation.incrementAndGetLevel());
        }
        
        @Test
        void smallRepeatEveryMustNotWarnBelowThreshold() {
            // Regression test: (count - threshold) % repeatEvery can be zero below the
            // threshold; without the explicit guard this warned on the very first failure.
            FailureEscalation escalation = new FailureEscalation(3, 1);
            
            assertEquals(Level.FINE, escalation.incrementAndGetLevel());
            assertEquals(Level.FINE, escalation.incrementAndGetLevel());
            assertEquals(Level.WARNING, escalation.incrementAndGetLevel());
        }
    }
    
    @Nested
    class FloodSuppression {
        
        @Test
        void demotesBetweenRepeatsAndWarnsOnEveryNth() {
            FailureEscalation escalation = new FailureEscalation(2, 3);
            
            assertEquals(Level.FINE, escalation.incrementAndGetLevel());    // 1: below threshold
            assertEquals(Level.WARNING, escalation.incrementAndGetLevel()); // 2: threshold hit
            assertEquals(Level.FINE, escalation.incrementAndGetLevel());    // 3: suppressed
            assertEquals(Level.FINE, escalation.incrementAndGetLevel());    // 4: suppressed
            assertEquals(Level.WARNING, escalation.incrementAndGetLevel()); // 5: repeat
            assertEquals(Level.FINE, escalation.incrementAndGetLevel());    // 6: suppressed
        }
        
        @Test
        void repeatEveryOneWarnsOnEveryEscalatedFailure() {
            FailureEscalation escalation = new FailureEscalation(2, 1);
            escalation.incrementAndGetLevel();
            
            assertEquals(Level.WARNING, escalation.incrementAndGetLevel());
            assertEquals(Level.WARNING, escalation.incrementAndGetLevel());
            assertEquals(Level.WARNING, escalation.incrementAndGetLevel());
        }
        
        @Test
        void repeatEveryBelowOneIsClampedToOne() {
            FailureEscalation escalation = new FailureEscalation(1, 0);
            
            assertEquals(Level.WARNING, escalation.incrementAndGetLevel());
            assertEquals(Level.WARNING, escalation.incrementAndGetLevel());
        }
        
        @Test
        void singleArgConstructorRepeatsEveryThresholdFailures() {
            FailureEscalation escalation = new FailureEscalation(2);
            escalation.incrementAndGetLevel();
            
            assertEquals(Level.WARNING, escalation.incrementAndGetLevel()); // 2: threshold hit
            assertEquals(Level.FINE, escalation.incrementAndGetLevel());    // 3: suppressed
            assertEquals(Level.WARNING, escalation.incrementAndGetLevel()); // 4: repeat
        }
    }
    
    @Nested
    class Recovery {
        
        @Test
        void successWithoutAnyFailuresReportsNothing() {
            FailureEscalation escalation = new FailureEscalation(2);
            
            assertTrue(escalation.recordSuccess().isEmpty());
        }
        
        @Test
        void successBelowThresholdReportsNothing() {
            FailureEscalation escalation = new FailureEscalation(3);
            escalation.incrementAndGetLevel();
            escalation.incrementAndGetLevel();
            
            assertTrue(escalation.recordSuccess().isEmpty());
        }
        
        @Test
        void successAfterEscalationReportsClearedStreakLength() {
            FailureEscalation escalation = new FailureEscalation(2);
            escalation.incrementAndGetLevel();
            escalation.incrementAndGetLevel();
            escalation.incrementAndGetLevel();
            
            assertEquals(OptionalInt.of(3), escalation.recordSuccess());
        }
        
        @Test
        void successResetsTheStreak() {
            FailureEscalation escalation = new FailureEscalation(2);
            escalation.incrementAndGetLevel();
            escalation.incrementAndGetLevel();
            escalation.recordSuccess();
            
            assertEquals(Level.FINE, escalation.incrementAndGetLevel());    // streak restarted at 1
            assertEquals(Level.WARNING, escalation.incrementAndGetLevel()); // threshold applies anew
        }
        
        @Test
        void secondSuccessDoesNotReportRecoveryTwice() {
            FailureEscalation escalation = new FailureEscalation(1);
            escalation.incrementAndGetLevel();
            escalation.recordSuccess();
            
            assertTrue(escalation.recordSuccess().isEmpty());
        }
    }
    
    @Nested
    class StreakGauge {
        
        @Test
        void reflectsFailureCount() {
            FailureEscalation escalation = new FailureEscalation(5);
            escalation.incrementAndGetLevel();
            escalation.incrementAndGetLevel();
            
            assertEquals(2, escalation.currentStreak());
        }
        
        @Test
        void resetsToZeroOnSuccess() {
            FailureEscalation escalation = new FailureEscalation(5);
            escalation.incrementAndGetLevel();
            escalation.recordSuccess();
            
            assertEquals(0, escalation.currentStreak());
        }
    }
}