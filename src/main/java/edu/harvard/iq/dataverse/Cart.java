/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;
import java.util.List;
import javax.ejb.Remote;

/**
 *
 * @author allegro_l
 */
@Remote
public interface Cart {
//    public void initialize(String person);

//    public void initialize(String person, String id);

    public void addItem(String title);

    public void removeItem(String title);

    public List<String> getContents();

    public void remove();
}
