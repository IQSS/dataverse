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
 * This class is used when creating an EJB Timer for scheduling Harvesting.
 * We use this class rather than the HarvestingClient entity because
 * the class must be Serializable, and there is too much info associated with the HarvestingClient
 * in order to realistically serialize it.  (We can't make related mapped entities transient.)
 * <p>
 * Based on the DVN 3 implementation,
 * original
 *
 * @author Ellen Kraffmiller
 * incorporated into Dataverse 4 by
 * @author Leonid Andreev
 */
public class HarvestTimerInfo implements Serializable {
    private Long harvestingClientId;

    public HarvestTimerInfo() {

    }


    public HarvestTimerInfo(Long harvestingClientId) {
        this.harvestingClientId = harvestingClientId;
    }


    public Long getHarvestingClientId() {
        return harvestingClientId;
    }

    public void setHarvestingClientId(Long harvestingClientId) {
        this.harvestingClientId = harvestingClientId;
    }
}
