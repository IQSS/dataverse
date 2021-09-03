package edu.harvard.iq.dataverse.datafile;

import com.google.common.collect.ImmutableSet;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MediaTypeRegistry;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

@Stateless
public class AntivirFileScanner {

    private static final Set<String> EXECUTABLE_CONTENT_TYPE_SUBTYPES = ImmutableSet.of("x-msdownload", "x-ms-installer", "java-archive");

    private static final int CHUNK_SIZE = 2048;
    private static final byte[] INSTREAM = "zINSTREAM\0".getBytes(StandardCharsets.UTF_8);

    private SettingsServiceBean settingsService;


    // -------------------- CONSTRUCTORS --------------------

    @Deprecated
    public AntivirFileScanner() {
    }

    @Inject
    public AntivirFileScanner(SettingsServiceBean settingsService) {
        this.settingsService = settingsService;
    }
    
    // -------------------- LOGIC --------------------
    
    public boolean isFileOverSizeLimit(File file, String contentType) {
        if (isExecutable(contentType)) {
            return file.length() > settingsService.getValueForKeyAsLong(SettingsServiceBean.Key.AntivirusScannerMaxFileSizeForExecutables);
        }
        
        return file.length() > settingsService.getValueForKeyAsLong(SettingsServiceBean.Key.AntivirusScannerMaxFileSize);
    }
    
    public AntivirScannerResponse scan(Path file) throws IOException {
        Socket socket = new Socket();

        socket.connect(new InetSocketAddress(settingsService.getValueForKey(SettingsServiceBean.Key.AntivirusScannerSocketAddress),
                                             settingsService.getValueForKeyAsInt(SettingsServiceBean.Key.AntivirusScannerSocketPort)));

        socket.setSoTimeout(settingsService.getValueForKeyAsInt(SettingsServiceBean.Key.AntivirusScannerSocketTimeout));

        DataOutputStream dos = null;
        try (InputStream fileInput = Files.newInputStream(file)) {

            dos = new DataOutputStream(socket.getOutputStream());
            dos.write(INSTREAM);
 
            int read = CHUNK_SIZE;
            byte[] buffer = new byte[CHUNK_SIZE];
            while (read == CHUNK_SIZE) {
                    read = fileInput.read(buffer);
 
                if (read > 0) {
                        dos.writeInt(read);
                        dos.write(buffer, 0, read);
                }
            }

            dos.writeInt(0);
            dos.flush();
 
            read = socket.getInputStream().read(buffer);
            String scannerMessage = new String(buffer, 0, read);
            boolean infected = scannerMessage.contains("FOUND");
            return new AntivirScannerResponse(infected, scannerMessage);

        } finally {
            if (dos != null) {
                dos.close();
            }
            socket.close();
        }

    }
    
    private boolean isExecutable(String contentType) {
        MediaType mediaType = MediaType.parse(contentType);
        while (mediaType != null && !mediaType.equals(MediaType.OCTET_STREAM)) {
            if (EXECUTABLE_CONTENT_TYPE_SUBTYPES.contains(mediaType.getSubtype())) {
                return true;
            }
            mediaType = MediaTypeRegistry.getDefaultRegistry().getSupertype(mediaType);
        }
        return false;
    }
    
}
