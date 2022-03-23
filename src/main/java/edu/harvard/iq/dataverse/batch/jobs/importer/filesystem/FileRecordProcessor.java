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

package edu.harvard.iq.dataverse.batch.jobs.importer.filesystem;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersion;

import jakarta.annotation.PostConstruct;
import jakarta.batch.api.chunk.ItemProcessor;
import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchRuntime;
import jakarta.batch.runtime.context.JobContext;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.File;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;


@Named
@Dependent
public class FileRecordProcessor implements ItemProcessor {
    
    @Inject
    JobContext jobContext;
    
    @EJB
    DatasetServiceBean datasetServiceBean;

    @EJB
    DataFileServiceBean dataFileServiceBean;

    Dataset dataset;
    
    @PostConstruct
    public void init() {
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        Properties jobParams = jobOperator.getParameters(jobContext.getInstanceId());
        dataset = datasetServiceBean.find(new Long(jobParams.getProperty("datasetId")));
    }

    // TODO: This method may be meaningles when used in the context of a "package file" 
    // batch import. See if it can be modified/improved? -- L.A. 4.6.1
    @Override
    public Object processItem(Object object) throws Exception {

        DatasetVersion version = dataset.getLatestVersion();
        String path = object.toString();
        String gid = dataset.getAuthority() + "/" + dataset.getIdentifier();
        String relativePath = path.substring(path.indexOf(gid) + gid.length() + 1);
        
        // skip if it already exists
        DataFile datafile = dataFileServiceBean.findByStorageIdandDatasetVersion(relativePath, version);
        if (datafile == null) {
            return new File(path);
        } else {
            Logger.getLogger("job-"+jobContext.getInstanceId()).log(Level.INFO, "Skipping " + relativePath + ", DataFile already exists.");
            return null;
        }
        
    }

}
