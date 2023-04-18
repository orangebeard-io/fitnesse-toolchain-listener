package io.orangebeard.listener.helper;

import io.orangebeard.client.entity.Attribute;
import java.util.Set;
import io.orangebeard.client.entity.test.TestType;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TypeConverterTest {

    @Test
    public void when_attributes_are_empty_an_empty_list_is_returned() {
        Set<Attribute> result = TypeConverter.convertAttributes(null);

        assertThat(result).isEmpty();
    }

    @Test
    public void when_attributes_are_pty_an_empty_list_is_returned() {
        Set<Attribute> result = TypeConverter.convertAttributes("bla,blaa;blaaa;blaaaa");

        assertThat(result).containsOnly(
                new Attribute("bla"),
                new Attribute("blaa"),
                new Attribute("blaaa"),
                new Attribute("blaaaa"));
    }

    @Test
    public void suite_setup_is_a_before_suite() {
        TestType result = TypeConverter.determinePageType("SuiteSetUp");

        assertThat(result).isEqualTo(TestType.BEFORE);
    }

    @Test
    public void suite_teardown_is_an_after_suite() {
        TestType result = TypeConverter.determinePageType("SuiteTearDown");

        assertThat(result).isEqualTo(TestType.AFTER);
    }
}
