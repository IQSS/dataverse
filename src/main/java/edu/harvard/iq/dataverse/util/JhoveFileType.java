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
package edu.harvard.iq.dataverse.util;

import edu.harvard.hul.ois.jhove.*;
import java.io.*;
import java.util.*;
import static java.lang.System.*;
import java.util.logging.Logger;

/**
 * This is based on Akio Sone's implementation from DVN v2-3:
 * JhoveWrapper.java
 * @author Akio Sone
 * 
 * Updated and integrated into 4.0 by
 * @author landreev
 *
 */
public class JhoveFileType implements java.io.Serializable  {
    private static final Logger logger = Logger.getLogger(JhoveFileType.class.getCanonicalName());

     
    public JhoveFileType() {
        
    }
    
    
    public static String getJhoveConfigFile() {
        Properties p = System.getProperties();
        String domainRoot = p.getProperty("com.sun.aas.instanceRoot");
        if (domainRoot == null) {
            // When testing statically from JUnit, we expect domainRoot to be null.
            return null;
        }
        return domainRoot+File.separator+"config"+File.separator+"jhove.conf";
    }
    
    private static final int[] ORIGINAL_RELEASE_DATE = { 2013, 8, 30 };
    private static final String ORIGINAL_COPR_RIGHTS = "Copyright"
        +"2004-2007 by the President and Fellows of Harvard College. "
        + "Released under the GNU Lesser General Public License.";
    
    /**
     * A method that returns Jhove's RepInfo
     */
    
    public RepInfo checkFileType(File file) {
        RepInfo info = null;
        boolean DEBUG = false;
        
        try {
            // initialize the application spec object
            // name, release number, build date, usage, Copyright infor
            App jhoveApp = new App("Jhove", "1.11", 
                           ORIGINAL_RELEASE_DATE, "Java JhoveFileType", 
                           ORIGINAL_COPR_RIGHTS);

            //String configFile = JhoveBase.getConfigFileFromProperties();
            String saxClass = JhoveBase.getSaxClassFromProperties();

            String configFile = getJhoveConfigFile();
            logger.fine("config file: "+configFile);
            if (configFile == null) {
                logger.info("Called getJhoveConfigFile but the result was null! Configuring JHOVE is highly recommended to determine file types.");
            }
        
            // create an instance of jhove engine
            JhoveBase jb = new JhoveBase();
            if (DEBUG) {
                jb.setLogLevel("INFO"); 
            } else {
                jb.setLogLevel("SEVERE");
            }
            jb.init(configFile, saxClass);

            jb.setEncoding("utf-8"); // encoding
            jb.setTempDirectory("/tmp");
            jb.setBufferSize(131072); // bufferSize

            jb.setChecksumFlag(false); // -s option
            jb.setShowRawFlag(false);  // -r option 
            jb.setSignatureFlag(true); // -k option
            
            // String moduleName = null;
            Module module = jb.getModule(null);
            
            if (DEBUG) {
                if (module != null) {
                    logger.fine("Module "+module.getName());
                } else {
                    logger.fine("module is null!");
                }
            }
            
            if (DEBUG){
                logger.fine("file name="+file.getAbsolutePath());
            }
            
            // get a RepInfo instance
            if (file.exists() &&  file.isFile() && (file.length() > 0L)){
                //info = jb.processRepInfo(jhoveApp, module, file);
                info = new RepInfo(file.getAbsolutePath());
                info.setSize(file.length());
                if (module != null) {
                    if (!jb.processFile(jhoveApp, module, false, file, info)) {
                        info = null; 
                    } else {
                        if (DEBUG) {
                            logger.fine("mime type (module specified above)="+info.getMimeType()); 
                        }
                    }
                } else {
                    /*
                     * Invoke all modules until one returns well-formed. If a
                     * module doesn't know how to validate, we don't want to
                     * throw arbitrary files at it, so we'll skip it.
                     */
                    //Iterator iter = _moduleList.iterator();
                    Iterator<Module> iter = jb.getModuleList().iterator();
                    while (iter.hasNext()) {
                        Module mod = iter.next();
                        RepInfo infc = (RepInfo) info.clone();

                        if (mod.hasFeature("edu.harvard.hul.ois.jhove.canValidate")) {
                            if (DEBUG) {
                                logger.fine("Trying to apply Jhove module "+mod.getName());
                            }
                            try {
                                if (!jb.processFile(jhoveApp, mod, false, file, infc)) {
                                    continue;
                                }
                                if (infc.getWellFormed() == RepInfo.TRUE) {
                                    info.copy(infc);
                                    break;
                                } else {
			            // We want to know what modules matched the
                                    // signature, so we force the sigMatch
                                    // property
                                    // to be persistent.
                                    info.setSigMatch(infc.getSigMatch());
                                }
                            } catch (Exception e) {
                                /*
                                 * The assumption is that in trying to analyze
                                 * the wrong type of file, the module may go off
                                 * its track and throw an exception, so we just
                                 * continue on to the next module.
                                 */
                                continue;
                            }
                        }
                    } 
                }
            } else {
                logger.warning("Jhove: the specified file does not exist or not a file or empty");
            }
        } catch (IOException ex){
            ex.printStackTrace();
        }   catch (Exception e) {
            e.printStackTrace();
        }
        return info;
    }
    
    /**
     * A convenience method that returns the value of the mime type tag only
     */
     
    public String getFileMimeType(File file) {
        String mimeType = null;
        boolean DEBUG = false;
        
        if (file.exists() &&  file.isFile() && (file.length() > 0L)){
            RepInfo info = checkFileType(file);
            if (info != null) {
                mimeType = info.getMimeType();
            }
        }
        
        return mimeType;
    }
}
