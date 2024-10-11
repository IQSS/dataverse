/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.passwordreset;

import edu.harvard.iq.dataverse.persistence.user.BuiltinUser;
import edu.harvard.iq.dataverse.persistence.user.PasswordResetData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PasswordResetDataTest {

    public PasswordResetDataTest() {
    }

    @BeforeAll
    public static void setUpClass() {
    }

    @AfterAll
    public static void tearDownClass() {
    }

    @BeforeEach
    public void setUp() {
    }

    @AfterEach
    public void tearDown() {
    }

    @Test
    public void testSetExpiredToSystemDefault() {
        // given
        PasswordResetData instance = new PasswordResetData(new BuiltinUser());
        long defValueOfPasswordReset = 60;

        // when
        instance.setExpires(new Timestamp(
                instance.getCreated().getTime() +
                        TimeUnit.MINUTES.toMillis(defValueOfPasswordReset)));
        long calculatedDefault = instance.getExpires().getTime() - instance.getCreated().getTime();

        // then
        assertEquals(calculatedDefault, TimeUnit.MINUTES.toMillis(defValueOfPasswordReset));
    }

    /**
     * @todo How do we test an expired token?
     */
}
