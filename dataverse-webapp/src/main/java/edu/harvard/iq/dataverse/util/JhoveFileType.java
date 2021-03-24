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

import edu.harvard.hul.ois.jhove.App;
import edu.harvard.hul.ois.jhove.JhoveBase;
import edu.harvard.hul.ois.jhove.JhoveException;
import edu.harvard.hul.ois.jhove.Module;
import edu.harvard.hul.ois.jhove.RepInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Iterator;

/**
 * This is based on Akio Sone's implementation from DVN v2-3:
 * JhoveWrapper.java
 *
 * @author Akio Sone
 * <p>
 * Updated and integrated into 4.0 by
 * @author landreev
 */
public class JhoveFileType implements java.io.Serializable {
    private static final Logger logger = LoggerFactory.getLogger(JhoveFileType.class);

    private static final int[] ORIGINAL_RELEASE_DATE = {2013, 8, 30};
    private static final String ORIGINAL_COPR_RIGHTS = "Copyright"
            + "2004-2007 by the President and Fellows of Harvard College. "
            + "Released under the GNU Lesser General Public License.";


    // -------------------- LOGIC --------------------

    /**
     * A convenience method that returns the value of the mime type tag only
     */
    public String getFileMimeType(File file) {
        String mimeType = null;

        if (file.exists() && file.isFile() && (file.length() > 0L)) {
            RepInfo info = checkFileType(file);
            if (info != null) {
                mimeType = info.getMimeType();
            }
        }

        return mimeType;
    }

    // -------------------- PRIVATE --------------------

    /**
     * A method that returns Jhove's RepInfo
     */
    private RepInfo checkFileType(File file) {
        RepInfo info = null;

        try {
            App jhoveApp = createJhoveApp();
            JhoveBase jb = createJhoveBase();

            // get a RepInfo instance
            info = new RepInfo(file.getAbsolutePath());
            info.setSize(file.length());

            /*
             * Invoke all modules until one returns well-formed. If a
             * module doesn't know how to validate, we don't want to
             * throw arbitrary files at it, so we'll skip it.
             */
            Iterator<Module> iter = jb.getModuleList().iterator();
            while (iter.hasNext()) {
                Module mod = iter.next();
                RepInfo infc = (RepInfo) info.clone();

                if (mod.hasFeature("edu.harvard.hul.ois.jhove.canValidate")) {
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
        } catch (Exception e) {
            logger.error("Unable to detect file type using jhove", e);
        }
        return info;
    }
    
    private App createJhoveApp() {
        // initialize the application spec object
        // name, release number, build date, usage, Copyright infor
        return new App("Jhove", "1.20.1",
                               ORIGINAL_RELEASE_DATE, "Java JhoveFileType",
                               ORIGINAL_COPR_RIGHTS);
    }

    private JhoveBase createJhoveBase() throws JhoveException {
        String configFile = JhoveConfigurationInitializer.JHOVE_CONFIG_PATH;

        // create an instance of jhove engine
        JhoveBase jb = new JhoveBase();
        jb.setLogLevel("SEVERE");
        jb.init(configFile, null);

        jb.setEncoding("utf-8"); // encoding
        jb.setTempDirectory("/tmp");
        jb.setBufferSize(131072); // bufferSize

        jb.setChecksumFlag(false); // -s option
        jb.setShowRawFlag(false);  // -r option 
        jb.setSignatureFlag(true); // -k option

        return jb;
    }
}
