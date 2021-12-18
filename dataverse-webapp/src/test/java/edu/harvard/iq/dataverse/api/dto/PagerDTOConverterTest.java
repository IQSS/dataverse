package edu.harvard.iq.dataverse.api.dto;

import edu.harvard.iq.dataverse.mydata.Pager;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PagerDTOConverterTest {
    private PagerDTO.Converter converter = new PagerDTO.Converter();

    @Test
    void convert() {
        // given
        Pager pager = new Pager(20, 10, 1);

        // when
        PagerDTO converted = converter.convert(pager);

        // then
        assertThat(converted).extracting(PagerDTO::getNecessary, PagerDTO::getNumResults, PagerDTO::getPageCount)
                .containsExactly(true, 20, 2);
        assertThat(converted).extracting(PagerDTO::getStartResultNumber, PagerDTO::getRemainingResults)
                .containsExactly(1, 10);
        assertThat(converted.getPageNumberList()).containsExactly(1, 2);
    }
}