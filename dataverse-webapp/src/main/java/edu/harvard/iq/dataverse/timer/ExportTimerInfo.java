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

package edu.harvard.iq.dataverse.timer;

import java.io.Serializable;

/**
 *
 * @author Leonid Andreev
 * This is the Export Timer, that executes regular export jobs. 
 * As of now (4.5) there is only 1; it's not configurable - rather it gets started
 * on every restart/deployment automatically. 
 * If we have to add more configurable exports further down the road, more settings 
 * can be added here. 
 */
public class ExportTimerInfo implements Serializable {
    
    String serverId; 
    
    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }
    
    public ExportTimerInfo() {
        
    }
    
    public ExportTimerInfo(String serverId) {
        this.serverId = serverId;
    }
    
}