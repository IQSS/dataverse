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
package edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.xlsx;

import edu.harvard.iq.dataverse.ingest.tabulardata.spi.TabularDataFileReaderSpi;
import edu.harvard.iq.dataverse.ingest.tabulardata.TabularDataFileReader;

import java.io.*;
import java.util.logging.*;
import java.util.Locale;

/*
 * New (in 4.0) Excel xslx (XML) spreadsheet ingest plugin, 
 * Service Provider registry class.
 *
 * @author Leonid Andreev
 */
public class XLSXFileReaderSpi extends TabularDataFileReaderSpi{

  private static Logger LOG = Logger.getLogger(XLSXFileReaderSpi.class.getPackage().getName());

  private static String[] formatNames = {"xlsx", "XLSX"};
  private static String[] extensions = {"xlsx", "XLSX"};
  private static String[] mimeType = {"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"};
  // Yep, that's the official mime type for .xlsx spreadsheets!
  // It's ok, we'll replace it with something user-friendly, when presenting it 
  // to the user. 

  /*
   * Construct  Object
   */
  public XLSXFileReaderSpi() {
    super("HU-IQSS-DVN-project", "4.0", formatNames, extensions, mimeType, XLSXFileReaderSpi.class.getName());
    LOG.fine(XLSXFileReaderSpi.class.getName()+" is called");
  }
  
  public String getDescription(Locale locale) {
    return "HU-IQSS-DVN-project Excel/XLSX";
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
    return new XLSXFileReader(this);
  }
}