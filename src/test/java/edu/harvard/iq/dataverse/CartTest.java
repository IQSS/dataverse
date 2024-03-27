package edu.harvard.iq.dataverse;

import java.util.List;
import java.util.Map.Entry;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class CartTest {

    private Cart cart;
    private String title;
    private String persistentId;

    @BeforeEach
    public void setUp() {
        this.cart = new Cart();
        this.title = "title";
        this.persistentId = "persistentId";
    }

    @AfterEach
    public void tearDwon() {
        this.cart = null;
        this.title = null;
        this.persistentId = null;
    }

    @Test
    public void addNonExistingItem() {
        try {
            this.cart.addItem(this.title, this.persistentId);
        } catch (Exception e) {
            fail("Item not added.");
        }
    }

    @Test
    public void addExistingItem() {
        try {
            this.cart.addItem(this.title, this.persistentId);
            this.cart.addItem(this.title, this.persistentId);
        } catch (Exception e) {
            assertEquals(this.title + " already in cart.", e.getMessage());
            return;
        }

        fail("Added same item twice!");
    }

    @Test
    public void removeExistingItem() {
        try {
            this.cart.addItem(this.title, this.persistentId);
            this.cart.removeItem(this.title, this.persistentId);
        } catch (Exception e) {
            fail("Item not removed.");
        }
    }

    @Test
    public void removeNonExistingItem() {
        try {
            this.cart.removeItem(this.title, this.persistentId);
        } catch (Exception e) {
            assertEquals(this.title + " not in cart.", e.getMessage());
            return;
        }

        fail("Non-existing item removed.");
    }

    @Test
    public void getContents() {
        try {
            this.cart.addItem(this.title, this.persistentId);
        } catch (Exception e) {
            fail("Item not added.");
            return;
        }

        List<Entry<String, String>> contents = this.cart.getContents();
        assertEquals(1, contents.size());
        assertEquals(this.title, contents.get(0).getKey());
        assertEquals(this.persistentId, contents.get(0).getValue());
    }

    @Test
    public void checkCartForItem() {
        try {
            this.cart.addItem(this.title, this.persistentId);
        } catch (Exception e) {
            fail("Item not added.");
            return;
        }

        assertTrue(this.cart.checkCartForItem(this.title, this.persistentId));
    }

    @Test
    public void clear() {
        try {
            this.cart.addItem(this.title, this.persistentId);
        } catch (Exception e) {
            fail("Item not added.");
            return;
        }

        assertEquals(1, this.cart.getContents().size());
        this.cart.clear();
        assertEquals(0, this.cart.getContents().size());
    }
}
