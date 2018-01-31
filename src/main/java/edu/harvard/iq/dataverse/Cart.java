package edu.harvard.iq.dataverse;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

/**
 *
 * @author allegro_l
 */
public class Cart {

    List<Entry<String,String>> contents = new ArrayList<>();

    public Entry<String,String> createEntry(String title, String persistentId) {
        Entry<String,String> entry = new AbstractMap.SimpleEntry<>(title,persistentId);
        return entry;
    }
    
    public void addItem(String title, String persistentId) throws Exception{
        if (!contents.contains(createEntry(title, persistentId))) {
            contents.add(createEntry(title, persistentId));
        } else {
            throw new Exception(title + "already in cart.");
        }
    }

    public void removeItem(String title, String persistentId) throws Exception{
        boolean result = contents.remove(createEntry(title, persistentId));
        if (result == false) {
            throw new Exception(title + " not in cart.");
        }
    }

    public List<Entry<String,String>> getContents() {
        return contents;
    }

    public void clear() {
        contents.clear();
    }
}
