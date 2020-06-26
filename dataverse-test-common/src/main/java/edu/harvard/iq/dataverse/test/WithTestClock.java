package edu.harvard.iq.dataverse.test;

import java.time.Clock;
import java.time.Instant;

import static java.time.ZoneOffset.UTC;

public interface WithTestClock {

    Clock clock = Clock.fixed(Instant.parse("2020-06-01T09:10:20.00Z"), UTC);

}
