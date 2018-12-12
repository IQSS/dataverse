package edu.harvard.iq.dataverse.metrics;

import org.primefaces.model.chart.Axis;
import org.primefaces.model.chart.AxisType;
import org.primefaces.model.chart.BarChartModel;
import org.primefaces.model.chart.ChartSeries;

import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

@ViewScoped
@Named("MetricsPage")
public class MetricsPage implements Serializable {

    @EJB
    private MetricsServiceBean metricsServiceBean;

    private BarChartModel barModel;

    private List<DatasetsMetrics> yearlyDatasetStats = new ArrayList<>();
    private List<DatasetsMetrics> monthlyDatasetStats = new ArrayList<>();

    private String mode = "YEAR";
    private int selectedYear;

    public void init(){
        selectedYear = LocalDate.now().getYear();
        changeToYearlyModel();
    }

    public void changeToYearlyModel() {
        yearlyDatasetStats = MetricsUtil.countDatasetsPerYear(metricsServiceBean.countPublishedDatasets());
        createBarModel(yearlyDatasetStats, "Year", initYearlyBarModel(yearlyDatasetStats));
    }

    public void changeToMonthlyModel() {
        monthlyDatasetStats = MetricsUtil.fillMissingDatasetMonths(metricsServiceBean.countPublishedDatasets(), selectedYear);
        createBarModel(monthlyDatasetStats, "Month", initMonthlyBarModel(monthlyDatasetStats));
    }

    public void changeDatasetMetricsModel() {
        if (mode.equals("YEAR")) {
            changeToYearlyModel();

        } else if (selectedYear != 0) {
            changeToMonthlyModel();
        }
    }

    private BarChartModel initYearlyBarModel(List<DatasetsMetrics> datasets) {
        BarChartModel model = new BarChartModel();

        ChartSeries datasetsChart = new ChartSeries();
        datasetsChart.setLabel("Datasets");

        datasets.forEach(datasetStats ->
                datasetsChart.set(datasetStats.getYear(), datasetStats.getCount()));

        model.addSeries(datasetsChart);

        return model;
    }

    private BarChartModel initMonthlyBarModel(List<DatasetsMetrics> datasets) {
        BarChartModel model = new BarChartModel();
        ChartSeries datasetsChart = new ChartSeries();
        datasetsChart.setLabel("Datasets");

        datasets.forEach(datasetStats ->
                datasetsChart.set(ResourceBundle.getBundle("Bundle").getString("metrics.month-"+datasetStats.getMonth()),
                        datasetStats.getCount()));

        model.addSeries(datasetsChart);

        return model;
    }

    private void createBarModel(List<DatasetsMetrics> datasets, String xAxisLabel, BarChartModel model) {
        barModel = model;

        barModel.setTitle("New datasets");
        barModel.setLegendPosition("ne");

        Axis xAxis = barModel.getAxis(AxisType.X);
        xAxis.setLabel(xAxisLabel);

        Axis yAxis = barModel.getAxis(AxisType.Y);
        yAxis.setLabel("Datasets");
        yAxis.setMin(0);
        yAxis.setTickFormat("%d");
        yAxis.setTickCount(datasets.size() + 1);
        yAxis.setMax(datasets.size());
    }

    public BarChartModel getBarModel() {
        return barModel;
    }

    public List<DatasetsMetrics> getYearlyDatasetStats() {
        return yearlyDatasetStats;
    }

    public List<DatasetsMetrics> getMonthlyDatasetStats() {
        return monthlyDatasetStats;
    }

    public String getMode() {
        return mode;
    }

    public int getSelectedYear() {
        return selectedYear;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public void setSelectedYear(int selectedYear) {
        this.selectedYear = selectedYear;
    }
}
