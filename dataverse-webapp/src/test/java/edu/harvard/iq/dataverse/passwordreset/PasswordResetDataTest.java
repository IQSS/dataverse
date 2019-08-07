/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.passwordreset;

import edu.harvard.iq.dataverse.persistence.user.BuiltinUser;
import edu.harvard.iq.dataverse.persistence.user.PasswordResetData;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class PasswordResetDataTest {

    public PasswordResetDataTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
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
