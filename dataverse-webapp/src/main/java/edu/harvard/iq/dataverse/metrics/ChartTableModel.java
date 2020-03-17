package edu.harvard.iq.dataverse.metrics;

import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

public class ChartTableModel {
    private List<Pair<String, String>> dataRow = new ArrayList<>();
    private String title;
    private String leftColumnName;
    private String rightColumnName;

    // -------------------- CONSTRUCTORS --------------------
    public ChartTableModel() {
    }

    public ChartTableModel(List<Pair<String, String>> dataRow, String title, String leftColumnName, String rightColumnName) {
        this.dataRow = dataRow;
        this.title = title;
        this.leftColumnName = leftColumnName;
        this.rightColumnName = rightColumnName;
    }

    // -------------------- GETTERS --------------------
    public List<Pair<String, String>> getDataRow() {
        return dataRow;
    }

    public String getTitle() {
        return title;
    }

    public String getLeftColumnName() {
        return leftColumnName;
    }

    public String getRightColumnName() {
        return rightColumnName;
    }

    // -------------------- SETTERS --------------------

    public void setDataRow(List<Pair<String, String>> dataRow) {
        this.dataRow = dataRow;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setLeftColumnName(String leftColumnName) {
        this.leftColumnName = leftColumnName;
    }

    public void setRightColumnName(String rightColumnName) {
        this.rightColumnName = rightColumnName;
    }
}
