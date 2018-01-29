package edu.harvard.iq.dataverse;

import java.util.ArrayList;
import java.util.List;
import javax.ejb.Stateless;
import javax.inject.Named;
import java.util.Map.Entry;
import java.util.AbstractMap.SimpleEntry;
/**
 *
 * @author allegro_l
 */
@Stateless
@Named
public class CartBean implements Cart {
    List<Entry<String,String>> contents = new ArrayList<>();

    @Override
    public Entry<String,String> createEntry(String title, String containerName) {
        Entry<String,String> entry = new SimpleEntry<>(title,containerName);
        return entry;
    }
    
    @Override
    public void addItem(String title, String containerName) throws Exception{
        if (!contents.contains(createEntry(title, containerName))) {
            contents.add(createEntry(title, containerName));
        } else {
            throw new Exception(title + "already in cart.");
        }
    }

    @Override
    public void removeItem(String title, String containerName) throws Exception{
        boolean result = contents.remove(createEntry(title, containerName));
        if (result == false) {
            throw new Exception(title + " not in cart.");
        }
    }

    @Override
    public List<Entry<String,String>> getContents() {
        return contents;
    }

    @Override
    public void clear() {
        contents.clear();
    }
}
