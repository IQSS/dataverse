package edu.harvard.iq.dataverse.metrics;

import edu.harvard.iq.dataverse.common.BundleUtil;
import org.primefaces.model.chart.Axis;
import org.primefaces.model.chart.AxisType;
import org.primefaces.model.chart.BarChartModel;
import org.primefaces.model.chart.ChartSeries;

import javax.ejb.Stateless;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Stateless
public class ChartCreator {

    // -------------------- LOGIC --------------------
    public BarChartModel createYearlyChart(List<ChartMetrics> metrics, String chartType) {
        List<ChartMetrics> yearlyMetrics =
                MetricsUtil.countMetricsPerYearAndFillMissingYears(metrics);

        if (yearlyMetrics.isEmpty()) {
            yearlyMetrics.add(new ChartMetrics((double) LocalDateTime.now().getYear(), 0L));
        }

        String xLabel = BundleUtil.getStringFromBundle("metrics.year");
        String yLabel = BundleUtil.getStringFromBundle("metrics.chart.legend." + chartType + ".label");
        String title = BundleUtil.getStringFromBundle("metrics.chart." + chartType + "title.");

        BarChartModel model = createBarModel(yearlyMetrics, title, xLabel, yLabel);
        model.addSeries(createYearlySeries(yearlyMetrics, yLabel));

        return model;
    }

    public BarChartModel createMonthlyChart(List<ChartMetrics> metrics, int year, String chartType) {
        List<ChartMetrics> chartMetrics =
                MetricsUtil.fillMissingMonthsForMetrics(metrics, year);

        String xLabel = BundleUtil.getStringFromBundle("metrics.month");
        String yLabel = BundleUtil.getStringFromBundle("metrics.chart.legend." + chartType + ".label");
        String title = BundleUtil.getStringFromBundle("metrics.chart." + chartType + ".title");

        BarChartModel model = createBarModel(chartMetrics, title, xLabel, yLabel);
        model.addSeries(createMonthlySeries(chartMetrics, yLabel));

        return model;
    }

    public BarChartModel createBarModel(List<ChartMetrics> metrics,
                                        String title,
                                        String xAxisLabel,
                                        String yAxisLabel) {

        BarChartModel model = new BarChartModel();
        ChartSeries chartSeries = new ChartSeries();
        chartSeries.setLabel(yAxisLabel);

        model.setTitle(title);
        model.setLegendPosition("ne");

        Axis xAxis = model.getAxis(AxisType.X);
        xAxis.setLabel(xAxisLabel);

        Axis yAxis = model.getAxis(AxisType.Y);
        yAxis.setLabel(yAxisLabel);
        yAxis.setMin(0);
        yAxis.setTickFormat("%d");

        Long maxCountMetric = calculateMaxCountMetric(metrics);

        yAxis.setTickCount(Math.toIntExact(retrieveTickForMaxDatasetCountValue(maxCountMetric)));
        yAxis.setMax(maxCountMetric);
        return model;
    }

    // -------------------- PRIVATE ---------------------
    private Long calculateMaxCountMetric(List<ChartMetrics> metrics) {
        return metrics.stream()
                    .max(Comparator.comparingLong(ChartMetrics::getCount))
                    .map(ChartMetrics::getCount)
                    .orElse(0L);
    }

    private long retrieveTickForMaxDatasetCountValue(Long maxCountValue) {
        return maxCountValue > 0 && maxCountValue < 4 ?
                maxCountValue + 1 : 5;
    }

    private ChartSeries createYearlySeries(List<ChartMetrics> yearlyMetrics, String columnLabel) {
        ChartSeries chartSeries = new ChartSeries();
        chartSeries.setLabel(columnLabel);

        yearlyMetrics.forEach(metric ->
                chartSeries.set(metric.getYear(), metric.getCount()));

        return chartSeries;
    }

    private ChartSeries createMonthlySeries(List<ChartMetrics> monthlyMetrics, String columnLabel) {
        ChartSeries chartSeries = new ChartSeries();
        chartSeries.setLabel(columnLabel);

        monthlyMetrics.forEach(metric ->
                chartSeries.set(BundleUtil.getStringFromBundle("metrics.month-" + metric.getMonth()),
                        metric.getCount()));

        return chartSeries;
    }
}