package edu.harvard.iq.dataverse.metrics;

import org.primefaces.model.chart.Axis;
import org.primefaces.model.chart.AxisType;
import org.primefaces.model.chart.BarChartModel;
import org.primefaces.model.chart.ChartSeries;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.List;
import java.util.ResourceBundle;

@Stateless
class ChartCreator { //TODO Is it proper class name??

    @EJB
    private MetricsServiceBean metricsServiceBean;

    @EJB
    private MetricsPage metricsPage;

    BarChartModel changeToYearlyModel() {
        List<DatasetsMetrics> yearlyDatasetStats =
                MetricsUtil.countDatasetsPerYear(metricsServiceBean.countPublishedDatasets());
        metricsPage.setYearlyDatasetStats(yearlyDatasetStats);
        //TODO fix broken pipe exception when assigning variable to other class

        return createBarModel(yearlyDatasetStats, "Year", initYearlyBarModel(yearlyDatasetStats));
    }

    BarChartModel changeToMonthlyModel(int selectedYear) {
        List<DatasetsMetrics> monthlyDatasetStats =
                MetricsUtil.fillMissingDatasetMonths(metricsServiceBean.countPublishedDatasets(), selectedYear);

        return createBarModel(monthlyDatasetStats, "Month", initMonthlyBarModel(monthlyDatasetStats));
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
                datasetsChart.set(ResourceBundle.getBundle("Bundle").getString("metrics.month-" + datasetStats.getMonth()),
                        datasetStats.getCount()));

        model.addSeries(datasetsChart);

        return model;
    }

    private BarChartModel createBarModel(List<DatasetsMetrics> datasets, String xAxisLabel, BarChartModel model) {

        model.setTitle("New datasets");
        model.setLegendPosition("ne");

        Axis xAxis = model.getAxis(AxisType.X);
        xAxis.setLabel(xAxisLabel);

        Axis yAxis = model.getAxis(AxisType.Y);
        yAxis.setLabel("Datasets");
        yAxis.setMin(0);
        yAxis.setTickFormat("%d");
        yAxis.setTickCount(datasets.size() + 1);
        yAxis.setMax(datasets.size());
        return model;
    } //TODO Bundles
}
