/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.dataaccess;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.datavariable.DataVariable;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 *
 * @author Leonid Andreev
 */
public class TabularSubsetInputStream extends InputStream {
    private TabularSubsetGenerator subsetGenerator = null;
    //private int numberOfSubsetVariables;
    private int numberOfObservations; 
    private int numberOfObservationsRead = 0;
    private byte[] leftoverBytes = null; 
    
    public TabularSubsetInputStream(DataFile datafile, List<DataVariable> variables) throws IOException {
        if (datafile == null) {
            throw new IOException("Null datafile in subset request");
        }
        if (!datafile.isTabularData()) {
            throw new IOException("Subset requested on a non-tabular data file");
        }
        numberOfObservations = datafile.getDataTable().getCaseQuantity().intValue();
        
        if (variables == null || variables.size() < 1) {
            throw new IOException("Null or empty list of variables in subset request.");
        }
        //numberOfSubsetVariables = variables.size();

        subsetGenerator = new TabularSubsetGenerator(datafile, variables);

    }
    
    @Override
    public int read() throws IOException {
        throw new IOException("read() method not implemented; do not use.");
    }

    @Override
    public int read(byte[] b) throws IOException {
        // TODO: 
        // add a special (fast!), single variable subset read() method, asap!
        
        int bytesread = 0; 
        byte [] linebuffer; 
        
        // do we have a leftover?
        if (leftoverBytes != null) {
            if (leftoverBytes.length < b.length) {
                System.arraycopy(leftoverBytes, 0, b, 0, leftoverBytes.length);
                leftoverBytes = null; 
                bytesread = leftoverBytes.length; 
            } else {
                // shouldn't really happen... unless it's a very large subset, 
                // or a very long string, etc.
                System.arraycopy(leftoverBytes, 0, b, 0, b.length);
                byte[] tmp = new byte[leftoverBytes.length - b.length];
                System.arraycopy(leftoverBytes, b.length, tmp, 0, leftoverBytes.length - b.length);
                leftoverBytes = tmp; 
                tmp = null; 
                return b.length; 
            }
        }
        
        while (bytesread < b.length && numberOfObservationsRead < numberOfObservations) {
            linebuffer = subsetGenerator.readSubsetLineBytes();
            if (bytesread + linebuffer.length <= b.length) {
                // copy linebuffer into the return buffer:
                System.arraycopy(linebuffer, 0, b, bytesread, linebuffer.length);
                bytesread += linebuffer.length;
            } else {
                System.arraycopy(linebuffer, 0, b, bytesread, b.length - bytesread);
                // save the leftover;
                leftoverBytes = new byte[bytesread + linebuffer.length - b.length];
                System.arraycopy(linebuffer, b.length - bytesread, leftoverBytes, 0, bytesread + linebuffer.length - b.length);
                return b.length; 
            }
            numberOfObservationsRead++;

        }
        
        // and this means we've reached the end of the tab file!
        
        return bytesread > 0 ? bytesread : -1;
    }
    
    @Override
    public void close() {
        if (subsetGenerator != null) {
            subsetGenerator.close();
        }
    }
}
