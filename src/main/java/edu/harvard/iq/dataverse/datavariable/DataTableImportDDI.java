/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.datavariable;

import edu.harvard.iq.dataverse.DataTable;
import edu.harvard.iq.dataverse.util.StringUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 *
 * @author Leonid Andreev
 */
public class DataTableImportDDI {

    public static final String VAR_INTERVAL_DISCRETE = "discrete";
    public static final String VAR_INTERVAL_CONTIN = "contin";
    public static final String VAR_INTERVAL_NOMINAL = "nominal";
    public static final String VAR_INTERVAL_DICHOTOMOUS = "dichotomous";
    
    public static final String VAR_TYPE_NUMERIC = "numeric";
    public static final String VAR_TYPE_CHARACTER = "character";


    public static final String VAR_WEIGHTED = "wgtd";

    public static final String LEVEL_VARIABLE = "variable";
    public static final String LEVEL_CATEGORY = "category";
    public static final String CAT_STAT_TYPE_FREQUENCY = "freq";


    
    public static final String NOTE_TYPE_UNF = "VDC:UNF";
}
