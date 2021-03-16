package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.dataaccess.DataFileZipper;
import org.apache.commons.lang.StringUtils;

import java.io.OutputStream;

public class ZipperWrapper {
    private DataFileZipper zipper;
    private String manifest = StringUtils.EMPTY;

    public ZipperWrapper init(OutputStream outputStream) {
        if (this.isEmpty()) {
            zipper = new DataFileZipper(outputStream);
            zipper.setFileManifest(manifest);
        }
        return this;
    }

    public boolean isEmpty() {
        return zipper == null;
    }

    public void addToManifest(String text) {
        if (this.isEmpty()) {
            manifest = manifest + text;
        } else {
            zipper.addToManifest(text);
        }
    }

    public DataFileZipper getZipper() {
        return zipper;
    }
}
