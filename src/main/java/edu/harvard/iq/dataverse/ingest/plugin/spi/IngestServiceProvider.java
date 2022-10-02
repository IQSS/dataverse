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

package edu.harvard.iq.dataverse.ingest.plugin.spi;

import static java.lang.System.*;
import java.util.Locale;

/**
 * A superclass that provides functionality common to all StatData I/O
 * service provider interface classes.
 * 
 * @author akio sone at UNC-Odum
 */
public abstract class IngestServiceProvider  {

    /**
     * The name of the vendor that is responsible for coding this
     * service provider and its associated implementation.
     * The constructor assigns a non-<code>null</code> value to this field.
     */
    protected String vendorName;
    
    /**
     * A string that describes the version number of this service provider
     * and its implementation.
     * The constructor assigns a non-<code>null</code> value to this field.
     */
    protected String version;

    
    public IngestServiceProvider(String vendorName, String version) {
        if (vendorName == null){
            throw new IllegalArgumentException("vendorName is null!");
        }
        if (version == null){
            throw new IllegalArgumentException("version string is null");
        }
        this.vendorName = vendorName;
        this.version = version;
        //out.println("SDIOServiceProvder is called");
    }

    
    public IngestServiceProvider() {
    }

    /**
     * Gets the value of the version field.
     *
     * @return the value of the version field.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Gets the value of the vendorName field.
     *
     * @return the value of the vendorName field.
     */
    public String getVendorName() {
        return vendorName;
    }

    /**
     * Returns a short, human-readable description of this 
     * service provider and its associated implementation.
     * The returned string should be localized for the 
     * supplied <code>lcoale</code>, if possible.
     *
     * @param locale a <code>Locale</code> that localizes the returned
     * string.
     *
     * @return a <code>String</code> that describes this
     * service provider.
     */
    public abstract String getDescription(Locale locale);
    
}
