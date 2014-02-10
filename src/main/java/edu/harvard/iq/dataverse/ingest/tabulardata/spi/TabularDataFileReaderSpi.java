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

import edu.harvard.iq.dataverse.ingest.tabulardata.*;
import java.util.logging.*;
import java.io.*;

/**
 * The service provider interface (SPI) for <code>StatDataFileReader</code>.
 * This abstract class supplies several types of information about the associated
 * <code>StatDataFileReader</code> class.
 * 
 * @author akio sone at UNC-Odum
 */
public abstract class TabularDataFileReaderSpi extends TabularDataFileReaderWriterSpi{

    private static Logger dbgLog = 
    Logger.getLogger(TabularDataFileReaderSpi.class.getPackage().getName());

    /**
     * A <code>String</code> array of the fully qualified names of 
     * all the <code>StatDataFileWriterSpi</code> classes associated with this 
     * <code>StatDataFileReader</code> class
     */
    protected String[] writerSpiNames = null;

    /**
     * Gets the value of the writerSpiNames field.
     * 
     * @return the value of the writerSpiNames field.
     */
    public String[] getStatDataFileWriterSpiNames() {
        return writerSpiNames == null ?
            null : (String[])writerSpiNames.clone();
    }
    
    /**
     * Constructs an empty <code>StatDataFileReaderSpi</code> instance.
     */
    protected TabularDataFileReaderSpi() {
    }

    /**
     * Constructs a <code>StatDataFileReaderSpi</code> instance with a given
     * set of values.
     *
     * @param vendorName the vendor name.
     * @param version    a version identifier.
     * @param names      at least one format name or more.
     * @param suffixes   at least one format extensions or more.
     * @param MIMETypes  at least one format's MIME type or more.
     * @param readerClassName the fully qualified name of the associated
     *                        <code>StatDataFileReaderSpi</code>.
     */
    public TabularDataFileReaderSpi(
            String vendorName,
            String version,
            String[] names,
            String[] suffixes,
            String[] MIMETypes,
            String readerClassName) {
        super(vendorName,
                version,
                names,
                suffixes,
                MIMETypes,
                readerClassName);
        dbgLog.fine("StatDataFileReaderSpi is called");
    }
    
    /**
     * Returns true if the supplied source object starts with a sequence of bytes
     * that is of the format supported by this reader.  Returning true from this
     * method does not guarantee that reading will successfully end.
     * 
     * @param source    typically a <code>BufferedInputStream</code> object.
     *                  object to be read.
     * @return          true if the stream can be read.
     * @throws java.io.IOException if an I/O error occurs
     *                             during reading the stream.
     */
    public abstract boolean canDecodeInput(Object source) throws IOException;
    
    
    /**
     * Returns true if the supplied <code>File</code> object starts with 
     * a sequence of bytes that is of the format supported by this reader.
     * Returning true from this method does not guarantee that reading will 
     * successfully end.
     * 
     * @param file  a <code>File</code> object to be read.
     * @return      true if the stream can be read.
     * @throws java.io.IOException if an I/O error occurs
     * during reading the stream.
     */
    public abstract boolean canDecodeInput(File file) throws IOException;

    /**
     * Returns true if the supplied <code>BufferedInputStream</code> object
     * starts with a sequence of bytes that is of the format 
     * supported by this reader.
     * Returning true from this method does not guarantee that reading will 
     * successfully end.
     * 
     * @param stream a <code>BufferedInputStream</code> object.
     * @return       true if the stream can be read.
     * @throws java.io.IOException  if an I/O error occurs
     * during reading the stream.
     */
    public abstract boolean canDecodeInput(BufferedInputStream stream) throws IOException;
    
    /**
     * Returns an instance of <code>StatDataFileReader</code> implementation associated with
     * this service provider.
     * 
     * @param extension     a plug-in specific extension object.
     * @return              a <code>StatDataFileReader</code> instance.
     * @throws IOException  if the instantiation attempt of the reader fails.
     */
    public abstract TabularDataFileReader createReaderInstance(Object extension)
        throws IOException;
    
    /**
     * Returns an instance of <code>StatDataFileReader</code> implementation 
     * associated with his service provider.
     *
     * @return  a <code>StatDataFileReader</code> instance.
     * @throws java.io.IOException if an error occurs during the 
     *         reader instantiation.
     */
    public TabularDataFileReader createReaderInstance() throws IOException{
        return createReaderInstance(null);
    }
    
    /**
     * Returns <code>true</code> if the <code>StatDataFileReader</code> object
     * supplied in is an instance of the <code>StatDataFileReader</code>
     * associated with this service provider.
     * 
     * @param reader  an <code>StatDataFileReader</code> object.
     * @return <code>true</code> if <code>reader</code> is recognized.
     */
    public boolean isOwnReader(TabularDataFileReader reader) {
        if (reader == null) {
            throw new IllegalArgumentException("reader == null!");
        }
        String name = reader.getClass().getName();
        return name.equals(pluginClassName);
    }

}
