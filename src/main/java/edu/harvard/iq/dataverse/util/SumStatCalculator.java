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

package edu.harvard.iq.dataverse.util;
import java.util.*;
import java.util.logging.Logger;

import org.apache.commons.lang.*;
import org.apache.commons.math.stat.*;
//import cern.colt.list.*;
//import cern.jet.stat.Descriptive;


/**
 *
 * @author Leonid Andreev
 */
public class SumStatCalculator {
    
    private static Logger logger = Logger.getLogger(SumStatCalculator.class.getPackage().getName());

    public static double[] calculateSummaryStatistics(Number[] x){
        logger.fine("entering calculate summary statistics ("+x.length+" Number values);");
        
        double[] nx = new double[8];
        //("mean", "medn", "mode", "vald", "invd", "min", "max", "stdev");

        Float testNanValue = new Float(Float.NaN);
        Number testNumberValue = testNanValue;
        if (Double.isNaN(testNumberValue.doubleValue())) {
            logger.fine("Float test NaN value is still recognized as a Double NaN.");
        }
        
        int invalid = countInvalidValues(x);
        nx[4] = invalid;
        logger.fine("counted invalid values: "+nx[4]);
        nx[3] = x.length - invalid;
        logger.fine("counted valid values: "+nx[3]);
        
        
        //double[] newx = prepareForSummaryStats(x);
        double[] newx = prepareForSummaryStatsAlternative(x, x.length - invalid);
        logger.fine("prepared double vector for summary stats calculation ("+newx.length+" double values);");        
        
        ////nx[0] = StatUtils.mean(newx);
        nx[0] = calculateMean(newx);
        logger.fine("calculated mean: "+nx[0]);
        ////nx[1] = StatUtils.percentile(newx, 50);
        nx[1] = calculateMedian(newx);
        logger.fine("calculated medn: "+nx[1]);
        nx[2] = 0.0; //getMode(newx); 
        
        nx[5] = StatUtils.min(newx);
        logger.fine("calculated min: "+nx[5]);
        nx[6] = StatUtils.max(newx);
        logger.fine("calculated max: "+nx[6]);
        nx[7] = Math.sqrt(StatUtils.variance(newx));
        logger.fine("calculated stdev: "+nx[7]);
        return nx;
    }  

    private static double[] prepareForSummaryStats(Number[] x) {
        Double[] z = numberToDouble(x);
        return removeInvalidValues(z);
    }

    private static double[] prepareForSummaryStatsAlternative(Number[] x, int length) {
        double[] retvector = new double[length];

        int c = 0;
        for (int i = 0; i < x.length; i++) {
            if (x[i] != null) {
                double xvalue = x[i].doubleValue();
                if (!Double.isNaN(xvalue)) {
                    retvector[c++] = xvalue; 
                }
            }
        }
        
        // Throw exception if c != length in the end?
        
        return retvector;
    }
    
    /**
     * Converts an array of primitive Number types to doubles
     *
     */
    private static Double[] numberToDouble(Number[] x){
        Double[] z= new Double[x.length];
        for (int i=0; i<x.length;i++){
            z[i] = x[i] != null ? new Double( x[i].doubleValue() ) : null;
        }
        return z;
    }
    
    /**
     * Returns a new double array of nulls and non-Double.NaN values only
     *
     */
    // TODO: 
    // implement this in some way that does not require allocating a new 
    // ArrayList for the values of every vector. -- L.A. Aug. 11 2014
    private static double[] removeInvalidValues(Double[] x){
        List<Double> dl = new ArrayList<Double>();
        for (Double d : x){
            if (d != null && !Double.isNaN(d)){
                dl.add(d);
            }
        }
        return ArrayUtils.toPrimitive(
            dl.toArray(new Double[dl.size()]));
    }
    
    /**
     * Returns the number of Double.NaNs (or nulls) in a double-type array
     *
     */
    private static int countInvalidValues(Number[] x){
        int counter=0;
        for (int i=0; i<x.length;i++){
            ////if ( x[i] == null || x[i].equals(Double.NaN) ) {
            if ( x[i] == null || (Double.isNaN(x[i].doubleValue())) ) {
                counter++;
            }
        }
        return counter;
    }
    
    /**
     * Returns the number of Double.NaNs in a double-type array
     *
     * TODO: figure out if this is actually necessary - to count NaNs and
     * nulls separately;
     *  -- L.A. 4.0 alpha 1
     */
    private static int countNaNs(double[] x){
        int NaNcounter=0;
        for (int i=0; i<x.length;i++){
            if (Double.isNaN(x[i])){
                NaNcounter++;
            }
        }
        return NaNcounter;
    }
    
    private static double calculateMedian(double[] values) {
        double[] sorted = new double[values.length];
        System.arraycopy(values, 0, sorted, 0, values.length);
        logger.fine("made an extra copy of the vector;");
        Arrays.sort(sorted);
        logger.fine("sorted double vector for median calculations;");
        
        if (sorted.length == 0) {
            return Double.NaN;
        }
        if (sorted.length == 1) {
            return sorted[0]; // always return single value for n = 1
        }
        double n = sorted.length;
        double pos = (n + 1) / 2;
        double fpos = Math.floor(pos);
        int intPos = (int) fpos;
        double dif = pos - fpos;
        
        double lower = sorted[intPos - 1];
        double upper = sorted[intPos];
        
        return lower + dif * (upper - lower);
    }
    
    private static double calculateMean(double[] values) {
        return calculateMean(values, 0 , values.length);
    }
    
    private static double calculateMean(double[] values, final int begin, final int length) {

        if (values == null || length == 0) {
            return Double.NaN;
        }

        double sampleSize = length;

        // Compute initial estimate using definitional formula
        double xbar = calculateSum(values) / sampleSize;

        // Compute correction factor in second pass
        double correction = 0;
        for (int i = begin; i < begin + length; i++) {
            correction += values[i] - xbar;
        }
        return xbar + (correction / sampleSize);
    }

    
    private static double calculateSum(double[] values) {
        return calculateSum(values, 0, values.length);
    }
    
    private static double calculateSum(double[] values, final int begin, final int length) {
        if (values == null || length == 0) {
            return Double.NaN;
        }
        double sum = 0.0;
        for (int i = begin; i < begin + length; i++) {
            sum += values[i];
        }
        return sum;
    }
    
    
    /**
     * Returns the mode statistic of a double variable
     *
     */
    /*
    public static double getMode(double[] x){
        double mode = Double.NaN;

        if ((countNaNs(x) == x.length) || (x.length < 1)){
            return mode;
        } else {
            DoubleArrayList dx = new DoubleArrayList(x);
            dx.sort();
            DoubleArrayList freqTable = new DoubleArrayList(1);
            IntArrayList countTable = new IntArrayList(1);
            Descriptive.frequencies(dx, freqTable, countTable);
            //out.println("freqTable="+
            //    ReflectionToStringBuilder.toString(freqTable));
            //out.println("freqTable="+
            //    ReflectionToStringBuilder.toString(countTable));
            int max_i = 0;
            for (int i=1; i< countTable.size();i++ ){
                if (countTable.get(i)> countTable.get(max_i)){
                    max_i = i;
                }
            }
            mode = freqTable.get(max_i);
            //out.println("position = "+
            //max_i+"\tits value="+freqTable.get(max_i));
        }
        return mode;
    }
    */
    
    
}
