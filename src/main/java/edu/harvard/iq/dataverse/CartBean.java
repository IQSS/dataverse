/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.util.ArrayList;
import java.util.List;
import javax.ejb.Remove;
import javax.ejb.Stateless;
import javax.inject.Named;
/**
 *
 * @author allegro_l
 */
@Stateless
@Named
public class CartBean implements Cart {

    // Add business logic below. (Right-click in editor and choose
    // "Insert Code > Add Business Method")
    String customerName;
    String customerId;
    List<String> contents = new ArrayList<String>();

//    public void initialize(String person) throws BookException {
//        if (person == null) {
//            throw new BookException("Null person not allowed.");
//        } else {
//            customerName = person;
//        }
//
//        customerId = "0";
//        contents = new ArrayList<String>();
//    }
//
//    public void initialize(String person, String id)
//            throws BookException {
//        if (person == null) {
//            throw new BookException("Null person not allowed.");
//        } else {
//
//            customerName = person;
//        }
//
//        IdVerifier idChecker = new IdVerifier();
//
//        if (idChecker.validate(id)) {
//            customerId = id;
//        } else {
//            throw new BookException("Invalid id: " + id);
//        }
//
//        contents = new ArrayList<String>();
//    }

    public void addItem(String title) {
        if (!contents.contains(title))
            contents.add(title);
    }

    public void removeItem(String title){
        boolean result = contents.remove(title);
        if (result == false) {
//            throw new BookException(title + " not in cart.");
        }
    }

    public List<String> getContents() {
        return contents;
    }

    @Remove
    public void remove() {
        contents = null;
    }
}
