package edu.harvard.iq.dataverse.batch.jobs.importer.checksum;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileServiceBean;

import javax.batch.api.chunk.AbstractItemWriter;
import javax.ejb.EJB;
import javax.enterprise.context.Dependent;
import javax.inject.Named;
import java.io.Serializable;
import java.util.List;


@Named
@Dependent
public class ChecksumWriter extends AbstractItemWriter {

    @EJB
    DataFileServiceBean dataFileServiceBean;

    @Override
    public void open(Serializable checkpoint) throws Exception {
        // no-op
    }

    @Override
    public void writeItems(List list) {

        for (Object dataFile : list) {
            dataFileServiceBean.save((DataFile) dataFile);
        }
    }

}
