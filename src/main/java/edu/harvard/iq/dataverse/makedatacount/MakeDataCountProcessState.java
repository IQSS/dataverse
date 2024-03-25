package edu.harvard.iq.dataverse.makedatacount;

import jakarta.persistence.*;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.Instant;

@Entity
public class MakeDataCountProcessState implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    public enum MDCProcessState {
        NEW("new"), DONE("done"), SKIP("skip"), PROCESSING("processing"), FAILED("failed");
        private final String text;
        private MDCProcessState(final String text) {
            this.text = text;
        }
        @Override
        public String toString() {
            return text;
        }
    }
    @Column(nullable = false)
    private String yearMonth;
    @Column(nullable = false)
    private MDCProcessState state;
    @Column(nullable = true)
    private Timestamp state_change_time;

    public MakeDataCountProcessState() { }
    public MakeDataCountProcessState (String yearMonth, String state) {
        this.setYearMonth(yearMonth);
        this.setState(state);
    }

    public void setYearMonth(String yearMonth) throws IllegalArgumentException {
        // Todo: add constraint
        if (yearMonth == null || (!yearMonth.matches("\\d{4}-\\d{2}") && !yearMonth.matches("\\d{4}-\\d{2}-\\d{2}"))) {
            throw new IllegalArgumentException("YEAR-MONTH date format must be either yyyy-mm or yyyy-mm-dd");
        }
        this.yearMonth = yearMonth;
    }
    public String getYearMonth() {
        return this.yearMonth;
    }
    public void setState(MDCProcessState state) {
        this.state = state;
        this.state_change_time = Timestamp.from(Instant.now());
    }
    public void setState(String state) throws IllegalArgumentException {
        if (state != null) {
            setState(MDCProcessState.valueOf(state.toUpperCase()));
        } else {
            throw new IllegalArgumentException("State is required and can not be null");
        }
    }
    public MDCProcessState getState() {
        return this.state;
    }
    public Timestamp getStateChangeTime() {
        return state_change_time;
    }
}
