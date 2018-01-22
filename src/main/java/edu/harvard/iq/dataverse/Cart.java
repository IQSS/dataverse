package edu.harvard.iq.dataverse;
import java.util.List;
import java.util.Map.Entry;
import javax.ejb.Remote;

/**
 *
 * @author allegro_l
 */
@Remote
public interface Cart {

    public void addItem(String title, String containerName) throws Exception;

    public void removeItem(String title, String containerName) throws Exception;

    public List<Entry<String,String>> getContents();

    public void clear();
    
    public Entry<String,String> createEntry(String title, String containerName);
}
