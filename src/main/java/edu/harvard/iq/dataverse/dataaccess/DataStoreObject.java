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

package edu.harvard.iq.dataverse.dataaccess;


import edu.harvard.iq.dataverse.DataFile;

import java.io.IOException;
import java.io.OutputStream;


import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.methods.GetMethod;


/**
 *
 * @author Leonid Andreev
 */

public abstract class DataStoreObject {

        public DataStoreObject () {

        }

        public DataStoreObject (DataFile file) {
            this.file = file;
        }

        private DataFile file;

        private int status;
        private long size;

        private String location;
        private OutputStream out;

        private String mimeType;
        private String errorMessage;

        private Boolean isLocalFile = false;
        private Boolean isRemoteAccess = false;
        private Boolean isNIOSupported = false;


        public abstract void open () throws IOException;
       
        public abstract boolean canWrite (String location) throws IOException;

        // getters:

        public DataFile getFile () {
            return file;
        }


        public int getStatus () {
            return status;
        }

        private long getSize () {
            return size;
        }

        public String getLocation () {
            return location;
        }

        public OutputStream getOutputStream () {
            return out;
        }

        public String getMimeType () {
            return mimeType;
        }

        public String getErrorMessage () {
            return errorMessage;
        }
        
        public Boolean isLocalFile () {
            return isLocalFile;
        }

        public Boolean isRemoteAccess () {
            return isRemoteAccess;
        }

        public Boolean isNIOSupported () {
            return isNIOSupported;
        }

        // setters:

        public void setFile (DataFile f) {
            file = f;
        }

        public void setStatus (int s) {
            status = s;
        }

        public void setSize (long s) {
            size = s;
        }

        public void setLocation (String l) {
            location = l; 
        }

        public void setOutputStream (OutputStream os) {
            out = os;
        }

        public void setMimeType (String mt) {
            mimeType = mt;
        }

        public void setErrorMessage (String em) {
            errorMessage = em;
        }
        
        public void setIsLocalFile (Boolean f) {
            isLocalFile = f;
        }

        public void setIsRemoteAccess (Boolean r) {
            isRemoteAccess = r;
        }

        public void setIsNIOSupported (Boolean n) {
            isNIOSupported = n; 
        }


        public void closeOutputStream () {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ex) {
                    // we really don't care.
                    String eMsg = "Warning: IO exception closing input stream.";
                    if (errorMessage == null) {
                        errorMessage = eMsg;
                    } else {
                        errorMessage = eMsg + "; " + errorMessage;
                    }
                }
            }
        }
    }

