package edu.harvard.iq.dataverse.makedatacount;

import jakarta.persistence.*;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;

@Entity
@Table(indexes = {@Index(columnList="yearMonth")})
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
        public static MDCProcessState fromString(String text) {
            if (text != null) {
                for (MDCProcessState state : MDCProcessState.values()) {
                    if (text.equals(state.text)) {
                        return state;
                    }
                }
            }
            throw new IllegalArgumentException("State must be one of these values: " + Arrays.asList(MDCProcessState.values()) + ".");
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
    private Timestamp stateChangeTimestamp;
    @Column(nullable = true)
    private String server;

    public MakeDataCountProcessState() { }
    public MakeDataCountProcessState (String yearMonth, String state, String server) {
        this.setYearMonth(yearMonth);
        this.setState(state);
        this.setServer(server);
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
        this.stateChangeTimestamp = Timestamp.from(Instant.now());
    }
    public void setState(String state) throws IllegalArgumentException {
        setState(MDCProcessState.fromString(state));
    }
    public MDCProcessState getState() {
        return this.state;
    }
    public Timestamp getStateChangeTime() {
        return stateChangeTimestamp;
    }
    public void setServer(String server) {
        this.server = server;
    }
    public String getServer() {
        return server;
    }
}
