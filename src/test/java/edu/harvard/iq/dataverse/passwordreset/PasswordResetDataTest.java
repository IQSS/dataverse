/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.passwordreset;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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
