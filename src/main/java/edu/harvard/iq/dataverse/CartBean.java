package edu.harvard.iq.dataverse;
import edu.harvard.iq.dataverse.Dataset;

import java.util.ArrayList;
import java.util.List;
import javax.ejb.Remove;
import javax.ejb.Stateless;
import javax.inject.Named;
import java.util.Map.Entry;
import java.util.AbstractMap.SimpleEntry;
import java.util.ListIterator;
/**
 *
 * @author allegro_l
 */
@Stateless
@Named
public class CartBean implements Cart {
    Dataset dataset;
    List<Entry<String,String>> contents = new ArrayList<>();

    public Entry<String,String> createEntry(String title, String containerName) {
        Entry<String,String> entry = new SimpleEntry<>(title,containerName);
        return entry;
    }
    
    public void addItem(String title, String containerName) throws Exception{
        if (!contents.contains(createEntry(title, containerName))) {
            contents.add(createEntry(title, containerName));
        } else {
            throw new Exception(title + "already in cart.");
        }
    }

    public void removeItem(String title, String containerName) throws Exception{
        boolean result = contents.remove(createEntry(title, containerName));
        if (result == false) {
            throw new Exception(title + " not in cart.");
        }
    }

    public List<Entry<String,String>> getContents() {
        return contents;
    }

    @Remove
    public void remove() {
        contents = null;
    }
}
