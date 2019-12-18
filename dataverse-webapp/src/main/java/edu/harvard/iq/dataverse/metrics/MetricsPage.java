package edu.harvard.iq.dataverse.metrics;

import org.omnifaces.cdi.ViewScoped;
import org.primefaces.model.chart.BarChartModel;

import javax.ejb.EJB;
import javax.inject.Named;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@ViewScoped
@Named("MetricsPage")
public class MetricsPage implements Serializable {

    @EJB
    private ChartCreator chartCreator;

    @EJB
    private MetricsServiceBean repository;

    private BarChartModel barModel;

    private List<DatasetsMetrics> yearlyDatasetStats = new ArrayList<>();

    private String mode = "YEAR";
    private int selectedYear;

    public void init() {
        selectedYear = LocalDate.now().getYear();
        yearlyDatasetStats = MetricsUtil.countDatasetsPerYear(repository.countPublishedDatasets());

        if (yearlyDatasetStats.isEmpty()) {
            yearlyDatasetStats.add(new DatasetsMetrics((double) LocalDateTime.now().getYear(), 0L));
        }

        barModel = chartCreator.changeToYearlyModel();
    }

    public void changeDatasetMetricsModel() {
        if (shouldGenerateYearlyModel()) {
            barModel = chartCreator.changeToYearlyModel();

        } else if (shouldGenerateMonthlyModel()) {
            barModel = chartCreator.changeToMonthlyModel(selectedYear);
        }
    }

    private boolean shouldGenerateMonthlyModel() {
        return selectedYear != 0;
    }

    private boolean shouldGenerateYearlyModel() {
        return mode.equals("YEAR");
    }

    public BarChartModel getBarModel() {
        return barModel;
    }

    public List<DatasetsMetrics> getYearlyDatasetStats() {
        return yearlyDatasetStats;
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

    public void setYearlyDatasetStats(List<DatasetsMetrics> yearlyDatasetStats) {
        this.yearlyDatasetStats = yearlyDatasetStats;
    }
}
