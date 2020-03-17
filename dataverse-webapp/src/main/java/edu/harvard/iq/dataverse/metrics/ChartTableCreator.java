package edu.harvard.iq.dataverse.metrics;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.common.BundleUtil;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.primefaces.model.chart.AxisType;
import org.primefaces.model.chart.BarChartModel;

import javax.ejb.Stateless;
import java.util.List;
import java.util.stream.Collectors;

@Stateless
public class ChartTableCreator {

    public ChartTableModel createChartTable(BarChartModel barChartModel) {
        ChartTableModel tableModel = new ChartTableModel();
        tableModel.setDataRow(loadDataForChartTable(barChartModel));
        tableModel.setTitle(barChartModel.getTitle());
        tableModel.setLeftColumnName(barChartModel.getAxis(AxisType.X).getLabel());
        tableModel.setRightColumnName(barChartModel.getAxis(AxisType.Y).getLabel());

        return tableModel;
    }

    public ChartTableModel createMonthlyChartTable(BarChartModel barChartModel, int year) {
        ChartTableModel tableModel = new ChartTableModel();
        tableModel.setDataRow(loadDataForChartTable(barChartModel));
        tableModel.setTitle(getMonthlyChartTableTitle(barChartModel, year));
        tableModel.setLeftColumnName(barChartModel.getAxis(AxisType.X).getLabel());
        tableModel.setRightColumnName(barChartModel.getAxis(AxisType.Y).getLabel());

        return tableModel;
    }

    private List<Pair<String, String>> loadDataForChartTable(BarChartModel chartModel) {
        if(chartModel.getSeries().isEmpty()) {
            return Lists.newArrayList(new ImmutablePair<>("NO DATA", "NO DATA"));
        }

        return chartModel.getSeries().get(0).getData().entrySet().stream()
                .map(entry -> new ImmutablePair<>(entry.getKey().toString(), entry.getValue().toString()))
                .collect(Collectors.toList());
    }

    private String getMonthlyChartTableTitle(BarChartModel barChartModel, int year) {
        return barChartModel.getTitle() + " " + BundleUtil.getStringFromBundle("metrics.table.titleappendix", String.valueOf(year));
    }
}
