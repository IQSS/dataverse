/*
   Copyright (C) 2005-2017, by the President and Fellows of Harvard College.

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
*/

package edu.harvard.iq.dataverse.batch.jobs.importer;

/**
 * <code>ImportMode</code> is used to define how importing data files applies to existing data files in a dataset.
 */
public enum ImportMode {

    /**
     * Default behavior. Existing data files are not deleted or modified. Only new data files are added.
     */
    MERGE,

    /**
     * Existing data files are updated. New data files are added. Nothing is deleted.
     */
    UPDATE,
    
    /**
     * Existing data files are replaced completely.
     */
    REPLACE
    
}