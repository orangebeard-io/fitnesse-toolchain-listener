package io.orangebeard.listener;

import io.orangebeard.listener.helper.ImageEncoder;
import io.orangebeard.listener.helper.OrangebeardTableLogParser;

import java.io.File;
import org.mockito.stubbing.Answer;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

public class OrangebeardTableLogParserTest {

    private final String TEST_HTML_WITH_IMAGES = "<html>\n" +
            "<body>\n" +
            "<img src=\"1.png\"/>\n" +
            "<a href=\"#\"><img src=\"2.png\"/></a>\n" +
            "</body>\n" +
            "</html>";
    private final String TEST_HTML_WITH_LINK = "<html>\n" +
            "<body>\n" +
            "<a href=\"http://hyperlink1\">this is link 1</a>\n" +
            "<a href=\"http://hyperlink2\">this is link 2</a>\n" +
            "</body>\n" +
            "</html>";

    @Test
    public void multiple_images_can_be_replaced() throws Exception {
        mockStatic(ImageEncoder.class);

        when(ImageEncoder.encodeForEmbedding(any())).thenAnswer((Answer<String>) invocation -> {
            Object[] args = invocation.getArguments();
            return "BASE64OF:" + args[0].toString();
        });

        String result = OrangebeardTableLogParser.embedImagesAndStripHyperlinks(TEST_HTML_WITH_IMAGES, "");
        assertThat(result)
                .contains("BASE64OF:" + File.separator + "1.png")
                .contains("BASE64OF:" + File.separator + "2.png");
    }

    @Test
    public void hyperlinks_are_removed() {
        String result = OrangebeardTableLogParser.embedImagesAndStripHyperlinks(TEST_HTML_WITH_LINK, "");
        assertThat(result)
                .doesNotContain("a href")
                .doesNotContain("http://hyperlink1")
                .doesNotContain("http://hyperlink2")
                .contains("this is link 1")
                .contains("this is link 2");
    }
}
