package edu.harvard.iq.dataverse;

/**
 * Tests annotated as non-essential will not be run by default on developers'
 * laptops but they will run on continuous integration platforms like Travis CI.
 * To work on one of these tests, you have to comment out the annotation.
 */
public interface NonEssentialTests {

}
