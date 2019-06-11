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
package edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.rdata;

import edu.harvard.iq.dataverse.ingest.tabulardata.spi.TabularDataFileReaderSpi;
import edu.harvard.iq.dataverse.ingest.tabulardata.TabularDataFileReader;

import java.io.*;
import java.util.logging.*;
import java.util.Locale;

/*
 * original 
 * @author Matt Owen
 * @author Leonid Andreev
 */
public class RDATAFileReaderSpi extends TabularDataFileReaderSpi{

  private static Logger LOG = Logger.getLogger(RDATAFileReaderSpi.class.getPackage().getName());

  private static String[] formatNames = {"Rdata", "rdata", "RDATA"};
  private static String[] extensions = {"Rdata", "rdata"};
  private static String[] mimeType = {"application/x-rlang-transport"};

  /*
   * Construct RDATAFileReaderSpi Object
   */
  public RDATAFileReaderSpi() {
    super("HU-IQSS-DVN-project", "0.1", formatNames, extensions, mimeType, RDATAFileReaderSpi.class.getName());
    LOG.fine(RDATAFileReaderSpi.class.getName()+" is called");
  }
  
  public String getDescription(Locale locale) {
    return "HU-IQSS-DVN-project RDATA";
  }
  
  @Override
  public boolean canDecodeInput(Object source) throws IOException {
    
    if (!(source instanceof BufferedInputStream))
      return false;

    return canDecodeInput((BufferedInputStream)source);
  }
  
  @Override
  public boolean canDecodeInput(BufferedInputStream stream) throws IOException {
    return false;
  }
  
  @Override
  public boolean canDecodeInput(File file) throws IOException {
    return true;
  }
  
  public boolean fileIsValid() throws IOException {
    return false; 
  }
  
  @Override
  public TabularDataFileReader createReaderInstance(Object ext) throws IOException {
    return new RDATAFileReader(this);
  }
}