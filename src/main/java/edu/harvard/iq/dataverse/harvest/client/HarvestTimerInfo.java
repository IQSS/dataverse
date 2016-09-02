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
package edu.harvard.iq.dataverse.harvest.client;

import java.io.Serializable;

/**
 *  This class is used when creating an EJB Timer for scheduling Harvesting.
 *  We use this class rather than the HarvestingClient entity because
 *  the class must be Serializable, and there is too much info associated with the HarvestingClient
 *  in order to realistically serialize it.  (We can't make related mapped entities transient.)
 *
 *  Based on the DVN 3 implementation, 
 *  original
 *  @author Ellen Kraffmiller
 *  incorporated into Dataverse 4 by
 *  @author Leonid Andreev
 */
public class HarvestTimerInfo implements Serializable {
    private Long harvestingClientId;
    private String name;
    private String schedulePeriod;
    private Integer scheduleHourOfDay;
    
    public HarvestTimerInfo() {
        
    }
    
   
    public HarvestTimerInfo(Long harvestingClientId, String name, String schedulePeriod, Integer scheduleHourOfDay, Integer scheduleDayOfWeek) {
        this.harvestingClientId=harvestingClientId;
        this.name=name;
        this.schedulePeriod=schedulePeriod;
        this.scheduleDayOfWeek=scheduleDayOfWeek;
        this.scheduleHourOfDay=scheduleHourOfDay;
    }
    
    
    public Long getHarvestingClientId() {
        return harvestingClientId;
    }

    public void setHarvestingClientId(Long harvestingClientId) {
        this.harvestingClientId = harvestingClientId;
    }    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSchedulePeriod() {
        return schedulePeriod;
    }

    public void setSchedulePeriod(String schedulePeriod) {
        this.schedulePeriod = schedulePeriod;
    }

    public Integer getScheduleHourOfDay() {
        return scheduleHourOfDay;
    }

    public void setScheduleHourOfDay(Integer scheduleHourOfDay) {
        this.scheduleHourOfDay = scheduleHourOfDay;
    }

    public Integer getScheduleDayOfWeek() {
        return scheduleDayOfWeek;
    }

    public void setScheduleDayOfWeek(Integer scheduleDayOfWeek) {
        this.scheduleDayOfWeek = scheduleDayOfWeek;
    }
    private Integer scheduleDayOfWeek;
  
    
}
