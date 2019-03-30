package edu.harvard.iq.dataverse.mydata;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class DataRetrieverAPITest {
  @Parameter()
  public int selectedPage;

  @Parameter(1)
  public int numResults;

  @Parameter(2)
  public int itemsPerPage;

  @Parameter(3)
  public int chosenPage;

  @Parameters
  public static Collection<Object[]> parameters() {
    return Arrays.asList(
      new Object[][] {
        { null, null, null, null },
        { -5, null, null, null },
        { 5, null, null, null },
        { 20, null, null, null },
      }
    );
  }


  @Test
  public void testGetRandomPagerPager() {
    DataRetrieverAPI api = new DataRetrieverAPI();
    Pager randomPager = api.getRandomPagerPager(selectedPage);

    assertEquals(numResults, randomPager.getNumResults());
    assertEquals(itemsPerPage, randomPager.getDocsPerPage());
    assertEquals(chosenPage, randomPager.getSelectedPageNumber());
  }
}
