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
    private static int TICKS_COUNT = 5;

    // -------------------- LOGIC --------------------
    public BarChartModel createYearlyChart(List<ChartMetrics> metrics, String chartType) {
        List<ChartMetrics> yearlyMetrics =
                MetricsUtil.countMetricsPerYearAndFillMissingYears(metrics);

        if (yearlyMetrics.isEmpty()) {
            yearlyMetrics.add(new ChartMetrics((double) LocalDateTime.now().getYear(), 0L));
        }

        String xLabel = BundleUtil.getStringFromBundle("metrics.year");
        String yLabel = BundleUtil.getStringFromBundle("metrics.chart.legend." + chartType + ".label");
        String title = BundleUtil.getStringFromBundle("metrics.chart." + chartType + ".title");

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
        model.setExtender("customizeChart");
        model.setBarMargin(10);

        Axis xAxis = model.getAxis(AxisType.X);
        xAxis.setLabel(xAxisLabel);
        xAxis.setTickAngle(45);

        Long maxCountMetric = calculateMaxCountMetric(metrics);

        Axis yAxis = model.getAxis(AxisType.Y);
        yAxis.setLabel(yAxisLabel);
        yAxis.setMin(0);
        yAxis.setTickFormat("%d");
        yAxis.setMax(retrieveTickForMaxDatasetCountValue(maxCountMetric));
        yAxis.setTickCount(TICKS_COUNT + 1);

        return model;
    }

    // -------------------- PRIVATE ---------------------
    private Long calculateMaxCountMetric(List<ChartMetrics> metrics) {
        return metrics.stream()
                    .max(Comparator.comparingLong(ChartMetrics::getCount))
                    .map(ChartMetrics::getCount)
                    .orElse(0L);
    }

    /**
     * We want to have empty space between the highest bar and the top tick of a chart.
     * Also that bar should be finished between last and one before last tick on the chart.
     * 1. Calculate {@value increasedMax} by increasing original max by approximately {@value deltaVal} margin (ex. 0.1 = 10%)
     * 2. Round up value from step 1 to the nearest number dividable by {@value deltaVal}
     * 3. If value from step 1 is also dividable by {@value TICKS_COUNT}, return value from step 2
     *    else increase value from step 2 to the nearest number dividable by TICKS_COUNT and return
     * @param maxCountValue - maximum bar height taken from model data
     * @return calculated maximum tick value
     */
    private long retrieveTickForMaxDatasetCountValue(Long maxCountValue) {
        double approximatedDelta = 0.1;

        long deltaVal = (long) (maxCountValue*approximatedDelta);
        deltaVal = deltaVal > 0 ? deltaVal : 1;

        long increasedMax = maxCountValue + deltaVal;
        long result = maxCountValue + (deltaVal - increasedMax % deltaVal);

        if(increasedMax % TICKS_COUNT != 0) {
            result += TICKS_COUNT - (result % TICKS_COUNT);
        }

        return result > 4 ? result : TICKS_COUNT;
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