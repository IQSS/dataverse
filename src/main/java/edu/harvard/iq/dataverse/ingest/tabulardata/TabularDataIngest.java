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
package edu.harvard.iq.dataverse.ingest.tabulardata;

import java.io.File;
import edu.harvard.iq.dataverse.DataTable;

/**
 * A new Ingest object class that represents ingested tabular data object, both
 * the data and the metadata describing it.
 *
 * The metadata will be stored in the native DVN DataTable object.
 *
 * The tab-delimited data will be in a regular Java (not DVN!) File object. It
 * will be the job of the DVN application to take this File and attach it to the
 * new DataFile object, together with the DataTable this ingest object provides.
 * TODO: finalize this! -- L.A. 4.0
 *
 */
public class TabularDataIngest {

    protected DataTable dataTable;

    protected File tabDelimitedFile;

    public TabularDataIngest() {
    }

    public TabularDataIngest(DataTable dataTable) {
        this.dataTable = dataTable;
    }

    public DataTable getDataTable() {
        return dataTable;
    }

    public void setDataTable(DataTable dataTable) {
        this.dataTable = dataTable;
    }

    public File getTabDelimitedFile() {
        return tabDelimitedFile;
    }

    public void setTabDelimitedFile(File tabFile) {
        this.tabDelimitedFile = tabFile;
    }
}
