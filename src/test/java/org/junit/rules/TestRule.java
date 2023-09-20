package org.junit.rules;

/**
 * "Fake" class used as a replacement for Junit4-dependent classes.
 * See more at: <a href="https://github.com/testcontainers/testcontainers-java/issues/970">
 * GenericContainer run from Jupiter tests shouldn't require JUnit 4.x library on runtime classpath
 * </a>.
 */
@SuppressWarnings("unused")
public interface TestRule {
}
