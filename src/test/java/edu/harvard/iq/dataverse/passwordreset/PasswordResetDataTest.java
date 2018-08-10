/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.passwordreset;

import edu.harvard.iq.dataverse.EssentialTests;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.experimental.categories.Category;

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

    @Category(EssentialTests.class)
    @Test
    public void testNewTokenNotExpired() {
        System.out.println("newTokenNotExpired");
        PasswordResetData instance = new PasswordResetData(null);
        boolean expResult = false;
        boolean result = instance.isExpired();
        assertEquals(expResult, result);
    }

    /**
     * @todo How do we test an expired token?
     */
}
