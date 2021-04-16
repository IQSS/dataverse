/*
   Copyright (C) 2005-2012, by the President and Fellows of Harvard College.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

   Dataverse Network - A web application to share, preserve and analyze research data.
   Developed at the Institute for Quantitative Social Science, Harvard University.
   Version 3.0.
*/

package edu.harvard.iq.dataverse.ingest.tabulardata.spi;

import java.nio.MappedByteBuffer;
import java.util.logging.Logger;
import static java.lang.System.*;

import edu.harvard.iq.dataverse.ingest.plugin.spi.*;


/**
 * 
 * @author landreev
 * @author akio sone at UNC-Odum
 */

public abstract class TabularDataFileReaderWriterSpi extends IngestServiceProvider{

    private static Logger dbgLog = 
    Logger.getLogger(TabularDataFileReaderWriterSpi.class.getPackage().getName());
    /**
     * A <code>String</code> array that contains human-readable format names
     * and are used by the <code>StatDataFileReader</code> or 
     * <code>StatDataFileWriter</code> implementation related to this
     * class.
     */
    protected String[] names = null;

    /**
     * Gets the value of names.
     * @return the value of names.
     */
    public String[] getFormatNames() {
        return (String[])names.clone();
    }

    /**
     * A <code>String</code> array that contains format extensions 
     *  and are used by the <code>StatDataFileReader</code> or 
     * <code>StatDataFileWriter</code> implementation related to this
     * class.
     */
    protected String[] suffixes = null;
    
    /**
     * Gets the value of suffixes
     *
     * @return the value of suffixes
     */
    public String[] getFileSuffixes() {
        return suffixes == null ? null : (String[])suffixes.clone();
    }
    
    
    /**
     * A <code>String</code> array that contains MIME types 
     * and are used by the <code>StatDataFileReader</code> or 
     * <code>StatDataFileWriter</code> implementation related to this
     * class.
     */
    protected String[] MIMETypes = null;
    
    /**
     * Gets the value of MIMETypes
     *
     * @return the value of MIMETypes
     */
    public String[] getMIMETypes() {
        return MIMETypes == null ? null : (String[])MIMETypes.clone();
    }
    
    /**
     * A <code>String</code> that contains the name of the plug-in class.
     */
    protected String pluginClassName = null;

    /**
     * Gets the value of pluginClassName
     *
     * @return the value of pluginClassName
     */
    public String getPluginClassName() {
        return pluginClassName;
    }

    
    public TabularDataFileReaderWriterSpi() {
    }

    /**
     * Constructs an empty <code>TabularDataFileReaderWriterSpi</code> instance
     * with given values.
     * 
     * @param vendorName  the vendor name.
     * @param version     a version identifier.
     * @param names       at least one format name or more.
     * @param suffixes    at least one format extensions or more.
     * @param MIMETypes   at least one format's MIME type or more.
     * @param pluginClassName the fully qualified name of the associated
     * <code>StatDataFileReaderSpi</code> or 
     * <code>StatDataFileWriterSpi</code> class.
     */
    public TabularDataFileReaderWriterSpi(
            String vendorName,
            String version,
            String[] names,
            String[] suffixes,
            String[] MIMETypes,
            String pluginClassName
            ) {
        super(vendorName, version);

        dbgLog.fine("TabularDataFileReaderWriterSpi is called");

        if (names == null) {
            throw new IllegalArgumentException("names is null!");
        }

        if (names.length == 0) {
            throw new IllegalArgumentException("names.length is 0!");
        }
        this.names = (String[])names.clone();
        if (pluginClassName == null) {
            throw new IllegalArgumentException("pluginClassName is null!");
        }

        if (suffixes != null && suffixes.length > 0) {
            this.suffixes = (String[])suffixes.clone();
        }

        if (MIMETypes != null && MIMETypes.length > 0) {
            this.MIMETypes = (String[])MIMETypes.clone();
        }

        this.pluginClassName = pluginClassName;
    }

    /**
     * Writes a <code>MappedByteBuffer</code> object in hexadecimal.
     *
     * @param buff a MappedByteBuffer object.
     * @param hdr the title string.
     */
    public void printHexDump(MappedByteBuffer buff, String hdr) {
        int counter = 0;
        if (hdr != null) {
            out.println(hdr);
        }
        for (int i = 0; i < buff.capacity(); i++) {
            counter = i + 1;
            out.println(String.format("%02X ", buff.get()));
            if (counter % 16 == 0) {
                out.println();
            } else {
                if (counter % 8 == 0) {
                    out.print(" ");
                }
            }
        }
        out.println();
        buff.rewind();
    }
    /**
     * Writes the <code>byte</code> array in hexadecimal.
     *
     * @param buff a <code>byte</code> array.
     * @param hdr the title string.
     */
    public void printHexDump(byte[] buff, String hdr) {
        int counter = 0;
        if (hdr != null) {
            out.println(hdr);
        }
        for (int i = 0; i < buff.length; i++) {
            counter = i + 1;
            out.println(String.format("%02X ", buff[i]));
            if (counter % 16 == 0) {
                out.println();
            } else {
                if (counter % 8 == 0) {
                    out.print(" ");
                }
            }
        }
        out.println();
    }
}
