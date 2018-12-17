package edu.harvard.iq.dataverse.metrics;

import edu.harvard.iq.dataverse.util.BundleUtil;
import org.primefaces.model.chart.Axis;
import org.primefaces.model.chart.AxisType;
import org.primefaces.model.chart.BarChartModel;
import org.primefaces.model.chart.ChartSeries;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Stateless
public class ChartCreator {

    @EJB
    private MetricsServiceBean metricsServiceBean;

    public BarChartModel changeToYearlyModel() {
        List<DatasetsMetrics> yearlyDatasetStats =
                MetricsUtil.countDatasetsPerYear(metricsServiceBean.countPublishedDatasets());

        if (yearlyDatasetStats.isEmpty()) {
            yearlyDatasetStats.add(new DatasetsMetrics((double) LocalDateTime.now().getYear(), 0L));
        }

        return createBarModel(yearlyDatasetStats, BundleUtil.getStringFromBundle("metrics.year")
                , initYearlyBarModel(yearlyDatasetStats));
    }

    public BarChartModel changeToMonthlyModel(int selectedYear) {
        List<DatasetsMetrics> monthlyDatasetStats =
                MetricsUtil.fillMissingDatasetMonths(metricsServiceBean.countPublishedDatasets(), selectedYear);

        return createBarModel(monthlyDatasetStats, BundleUtil.getStringFromBundle("metrics.month")
                , initMonthlyBarModel(monthlyDatasetStats));
    }

    BarChartModel initYearlyBarModel(List<DatasetsMetrics> datasets) {
        BarChartModel model = new BarChartModel();

        ChartSeries datasetsChart = new ChartSeries();
        datasetsChart.setLabel(BundleUtil.getStringFromBundle("metrics.datasets"));

        datasets.forEach(datasetStats ->
                datasetsChart.set(datasetStats.getYear(), datasetStats.getCount()));

        model.addSeries(datasetsChart);

        return model;
    }

    BarChartModel initMonthlyBarModel(List<DatasetsMetrics> datasets) {
        BarChartModel model = new BarChartModel();
        ChartSeries datasetsChart = new ChartSeries();
        datasetsChart.setLabel("Datasets");

        datasets.forEach(datasetStats ->
                datasetsChart.set(BundleUtil.getStringFromBundle("metrics.month-" + datasetStats.getMonth()),
                        datasetStats.getCount()));

        model.addSeries(datasetsChart);

        return model;
    }

    BarChartModel createBarModel(List<DatasetsMetrics> datasets, String xAxisLabel, BarChartModel model) {

        model.setTitle(BundleUtil.getStringFromBundle("metrics.newDatasets"));
        model.setLegendPosition("ne");

        Axis xAxis = model.getAxis(AxisType.X);
        xAxis.setLabel(xAxisLabel);

        Axis yAxis = model.getAxis(AxisType.Y);
        yAxis.setLabel(BundleUtil.getStringFromBundle("metrics.datasets"));
        yAxis.setMin(0);
        yAxis.setTickFormat("%d");
        yAxis.setTickCount(datasets.size() + 1);

        Optional<DatasetsMetrics> datasetMax = datasets.stream().max(Comparator.comparing(DatasetsMetrics::getCount));
        yAxis.setMax(datasetMax.isPresent() ? datasetMax.get().getCount() : 0);
        return model;
    }
}
