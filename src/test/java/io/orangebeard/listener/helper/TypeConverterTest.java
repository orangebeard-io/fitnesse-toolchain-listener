package io.orangebeard.listener.helper;

import io.orangebeard.client.entity.Attribute;
import io.orangebeard.client.entity.TestItemType;

import java.util.Set;
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
    public void suitesetup_is_a_before_method() {
        TestItemType result = TypeConverter.determinePageType("SuiteSetUp");

        assertThat(result).isEqualTo(TestItemType.BEFORE_METHOD);
    }

    @Test
    public void suiteteardown_is_an_after_method() {
        TestItemType result = TypeConverter.determinePageType("SuiteTearDown");

        assertThat(result).isEqualTo(TestItemType.AFTER_METHOD);
    }
}
