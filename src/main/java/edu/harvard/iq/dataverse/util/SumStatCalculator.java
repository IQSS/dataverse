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
import java.io.*;
import java.util.*;

import org.apache.commons.lang.*;
import org.apache.commons.math.stat.*;
//import cern.colt.list.*;
//import cern.jet.stat.Descriptive;


/**
 *
 * @author Leonid Andreev
 */
public class SumStatCalculator {

    public static double[] calculateSummaryStatistics(Number[] x){
        double[] newx = prepareForSummaryStats(x);
        double[] nx = new double[8];
        //("mean", "medn", "mode", "vald", "invd", "min", "max", "stdev");

        nx[0] = StatUtils.mean(newx);
        nx[1] = StatUtils.percentile(newx, 50);
        nx[2] = 0.0; //getMode(newx); // TODO: remove -- L.A.
        nx[4] = countInvalidValues(x);
        nx[3] = x.length - nx[4];

        nx[5] = StatUtils.min(newx);
        nx[6] = StatUtils.max(newx);
        nx[7] = Math.sqrt(StatUtils.variance(newx));
        return nx;
    }  

    private static double[] prepareForSummaryStats(Number[] x) {
        Double[] z = numberToDouble(x);
        return removeInvalidValues(z);
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
            if ( x[i] == null || x[i].equals(Double.NaN) ) {
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
