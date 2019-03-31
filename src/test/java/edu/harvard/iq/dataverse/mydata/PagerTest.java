package edu.harvard.iq.dataverse.mydata;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

public class PagerTest {

  @Test
  public void testPager_throwsOnNegativeNumResults() {
    assertThrows(IllegalArgumentException.class, () -> {
      new Pager(-1, 10, 5);
    });
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 0})
  public void testPager_throwsOnInvalidDocsPerPage(int docsPerPage) {
    assertThrows(IllegalArgumentException.class, () -> {
      new Pager(10, docsPerPage, 1);
    });
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 0})
  public void testPager_throwsOnInvalidSelectedPageNumber(int selectedPageNumber) {
    assertThrows(IllegalArgumentException.class, () -> {
      new Pager(10, 5, selectedPageNumber);
    });
  }

  @Test
  public void testPager_withZeroResults() {
    int numResults = 0;
    int docsPerPage = 5;
    int selectedPageNumber = 3;

    Pager pager = new Pager(numResults, docsPerPage, selectedPageNumber);

    assertEquals(pager.numResults, numResults);
    assertEquals(pager.docsPerPage, docsPerPage);
    assertEquals(pager.selectedPageNumber, 0);
  }

  @Test
  public void testPager_withSelectedGtAvailablePageNumber() {
    int numResults = 10;
    int docsPerPage = 5;
    int selectedPageNumber = 3;

    Pager pager = new Pager(numResults, docsPerPage, selectedPageNumber);

    assertEquals(pager.numResults, numResults);
    assertEquals(pager.docsPerPage, docsPerPage);
    assertEquals(pager.selectedPageNumber, 1);
  }

  @Test
  public void testPager_isInstantiatedWithValidParams() {
    int numResults = 20;
    int docsPerPage = 5;
    int selectedPageNumber = 1;

    Pager pager = new Pager(numResults, docsPerPage, selectedPageNumber);

    assertEquals(pager.numResults, numResults);
    assertEquals(pager.docsPerPage, docsPerPage);
    assertEquals(pager.selectedPageNumber, selectedPageNumber);
  }

  @ParameterizedTest
  @CsvSource({ "0, false", "1, false", "2, true" })
  public void testIsPagerNecessary(int pageCount, boolean isNecessary) {
      Pager pager = new Pager(20, 10, 1);
      pager.setPageCount(pageCount);
      assertEquals(isNecessary, pager.isPagerNecessary());
  }

  @ParameterizedTest
  @CsvSource({ "1, false", "2, true" })
  public void testHasPreviousPageNumber(int selectedPageNumber, boolean hasPreviousPage) {
    Pager pager = new Pager(20, 10, selectedPageNumber);
    assertEquals(hasPreviousPage, pager.hasPreviousPageNumber());
  }

  @ParameterizedTest
  @CsvSource({ "0, 1, false", "1, 1, false", "2, 1, true" })
  public void testHasNextPageNumber(int pageCount, int selectedPageNumber, boolean hasNextPage) {
    Pager pager = new Pager(20, 10, 1);
    pager.setPageCount(pageCount);
    assertEquals(hasNextPage, pager.hasNextPageNumber());
  }

  @Test
  public void testGetPageNumberListAsStringList() {
    Pager pager = new Pager(20, 10, 1);
    int[] pageNumberList = { 1, 2, 3 };
    pager.setPageNumberList(pageNumberList);
    assertEquals(Arrays.asList("1", "2", "3"), pager.getPageNumberListAsStringList());
  }

  @Test
  public void testGetPageListAsIntegerList_withNullPageNumberList() {
    Pager pager = new Pager(20, 10, 1);
    pager.setPageNumberList(null);
    assertEquals(null, pager.getPageListAsIntegerList());
  }

  @Test
  public void testGetPageListAsIntegerList_withValidPageNumberList() {
    Pager pager = new Pager(20, 10, 1);
    int[] pageNumberList = { 1, 2, 3 };
    Integer[] pageNumberListIntegers = {Integer.valueOf(1), Integer.valueOf(2), Integer.valueOf(3)};
    pager.setPageNumberList(pageNumberList);
    assertArrayEquals(pageNumberListIntegers, pager.getPageListAsIntegerList());
  }

  @ParameterizedTest
  @CsvSource({
    "-1000000, '-1,000,000'",
    "-1000, '-1,000'",
    "-100, -100",
    "-1, -1",
    "0, 0",
    "1, 1",
    "100, 100",
    "1000, '1,000'",
    "1000000, '1,000,000'"
  })
  public void testAddCommasToNumber(int inputNumber, String formattedNumber) {
    Pager pager = new Pager(10, 10, 1);
    assertEquals(formattedNumber, pager.addCommasToNumber(inputNumber));
  }

  @Test
  public void testAsJsonObjectBuilder() {
    Pager pager = new Pager(20, 10, 2);
    assertEquals("JsonObjectBuilderImpl", pager.asJsonObjectBuilder().getClass().getSimpleName());
  }

  @Test
  public void testAsJsonObjectBuilder_withNullPageNumberList() {
    Pager pager = new Pager(20, 10, 2);
    pager.setPageNumberList(null);
    assertEquals("JsonObjectBuilderImpl", pager.asJsonObjectBuilder().getClass().getSimpleName());
  }

  @Test
  public void testAsJsonObjectBuilderUsingCardTerms() {
    Pager pager = new Pager(20, 10, 2);
    assertEquals("JsonObjectBuilderImpl", pager.asJsonObjectBuilderUsingCardTerms().getClass().getSimpleName());
  }
}
