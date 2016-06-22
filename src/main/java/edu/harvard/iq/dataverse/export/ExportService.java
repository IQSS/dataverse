
package edu.harvard.iq.dataverse.export;

import edu.harvard.iq.dataverse.export.spi.Exporter;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import javax.json.JsonObject;

/**
 *
 * @author skraffmi
 */
public class ExportService {

    private static ExportService service;
    private ServiceLoader<Exporter> loader;

    private ExportService() {
        loader = ServiceLoader.load(Exporter.class);        
    }

    public static synchronized ExportService getInstance() {
        if (service == null) {
            service = new ExportService();
        } else{
            service.loader.reload();
        }
        return service;
    }
                
    public List< String[]> getExportersLabels() {
        List<String[]> retList = new ArrayList();
        Iterator<Exporter> exporters = ExportService.getInstance().loader.iterator();
        while (exporters.hasNext()) {
            Exporter e = exporters.next();
            String[] temp = new String[2];
            temp[0] = e.getDisplayName();
            temp[1] = e.getProvider();
            retList.add(temp);
        }
        return retList;
    }

    public OutputStream getExport(JsonObject json, String provider) {
        try {
            Iterator<Exporter> exporters = loader.iterator();
            while ( exporters.hasNext()) {
                Exporter e = exporters.next();
                if (e.getProvider().equals(provider)) {
                    return e.exportDataset(json);
                }
            }
        } catch (ServiceConfigurationError serviceError) {
            serviceError.printStackTrace();
        }
        return null;
    }
    
    public Boolean isXMLFormat(String provider){
        try {
            Iterator<Exporter> exporters = loader.iterator();
            while (exporters.hasNext()) {
                Exporter e = exporters.next();
                if (e.getProvider().equals(provider)) {
                    return e.isXMLFormat();
                }
            }
        } catch (ServiceConfigurationError serviceError) {
            serviceError.printStackTrace();
        }
        return null;       
    }

}
