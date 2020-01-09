package edu.harvard.iq.dataverse.metrics;

import org.omnifaces.cdi.ViewScoped;
import org.primefaces.model.chart.BarChartModel;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@ViewScoped
@Named("UsersChart")
public class RegisteredUsersChart implements Serializable {

    private ChartCreator chartCreator;
    private MetricsServiceBean metricsService;

    private final String CHART_TYPE = "authenticatedUsers";

    private BarChartModel usersChart;
    private List<ChartMetrics> usersYearlyStats = new ArrayList<>();
    private List<ChartMetrics> usersMetrics = new ArrayList<>();

    private String mode = "YEAR";
    private int selectedYear;

    // -------------------- CONSTRUCTORS --------------------
    @Deprecated
    public RegisteredUsersChart() {
    }

    @Inject
    public RegisteredUsersChart(ChartCreator chartCreator, MetricsServiceBean metricsService) {
        this.chartCreator = chartCreator;
        this.metricsService = metricsService;
    }

    // -------------------- GETTERS --------------------
    public BarChartModel getUsersChart() {
        return usersChart;
    }

    public List<ChartMetrics> getUsersYearlyStats() {
        return usersYearlyStats;
    }

    public String getMode() {
        return mode;
    }

    public int getSelectedYear() {
        return selectedYear;
    }

    // -------------------- LOGIC --------------------
    public void init() {

        usersMetrics = metricsService.countAuthenticatedUsers();

        if (usersMetrics.isEmpty()) {
            usersYearlyStats.add(new ChartMetrics((double) LocalDateTime.now().getYear(), 0L));
            selectedYear = LocalDate.now().getYear();
        } else {
            usersYearlyStats = MetricsUtil.countMetricsPerYearAndFillMissingYearsDescending(usersMetrics);
            selectedYear = usersYearlyStats.get(0).getYear();
        }

        usersChart = chartCreator.createYearlyChart(usersMetrics, CHART_TYPE);
    }

    public void changeChartGrouping() {
        if (isYearlyChartSelected()) {
            usersChart = chartCreator.createYearlyChart(usersMetrics, CHART_TYPE);

        } else if (isMonthlyChartSelected()) {
            usersChart = chartCreator.createMonthlyChart(usersMetrics, selectedYear, CHART_TYPE);
        }
    }

    // -------------------- PRIVATE ---------------------
    private boolean isMonthlyChartSelected() {
        return selectedYear != 0;
    }

    private boolean isYearlyChartSelected() {
        return mode.equals("YEAR");
    }

    // -------------------- SETTERS --------------------
    public void setMode(String mode) {
        this.mode = mode;
    }

    public void setSelectedYear(int selectedYear) {
        this.selectedYear = selectedYear;
    }

    public void setUsersYearlyStats(List<ChartMetrics> usersYearlyStats) {
        this.usersYearlyStats = usersYearlyStats;
    }
}
