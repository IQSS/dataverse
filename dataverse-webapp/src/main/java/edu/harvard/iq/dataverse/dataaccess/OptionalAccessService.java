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

/**
 *
 * @author Leonid Andreev
 */
public class OptionalAccessService {
    private String serviceName; 
    private String serviceDescription; 
    private String contentMimeType; 
    private String serviceArguments; 
    
    public OptionalAccessService (String name, String mimeType, String arguments, String desc) {
        this.serviceName = name; 
        this.serviceDescription = desc; 
        this.contentMimeType = mimeType; 
        this.serviceArguments = arguments; 
    }
    
    public OptionalAccessService (String name, String mimeType, String arguments) {
        this(name, mimeType, arguments, null);
    }
    
    public String getServiceName() {
        return serviceName; 
    }
    
    public String getServiceDescription() {
        return serviceDescription; 
    }
    
    public String getMimeType() {
        return contentMimeType; 
    }
    
    public String getServiceArguments() {
        return serviceArguments;
    }
}
