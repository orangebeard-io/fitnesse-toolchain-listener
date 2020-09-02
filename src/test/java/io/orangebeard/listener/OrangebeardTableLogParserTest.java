package io.orangebeard.listener;

import io.orangebeard.listener.helper.ImageEncoder;
import io.orangebeard.listener.helper.OrangebeardTableLogParser;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ImageEncoder.class)
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
        PowerMockito.mockStatic(ImageEncoder.class);

        File file = mock(File.class);
        PowerMockito.whenNew(File.class).withArguments(anyString()).thenReturn(file);
        when(ImageEncoder.encodeForEmbedding(any())).thenAnswer((Answer<String>) invocation -> {
            Object[] args = invocation.getArguments();
            return "BASE64OF:" + args[0].toString();
        });

        String result = OrangebeardTableLogParser.embedImagesAndStripHyperlinks(TEST_HTML_WITH_IMAGES, "");
        assertThat(result).contains("BASE64OF:" + File.separator + "1.png");
        assertThat(result).contains("BASE64OF:" + File.separator + "2.png");
    }

    @Test
    public void hyperlinks_are_removed() {
        String result = OrangebeardTableLogParser.embedImagesAndStripHyperlinks(TEST_HTML_WITH_LINK, "");
        assertThat(result).doesNotContain("a href");
        assertThat(result).doesNotContain("http://hyperlink1");
        assertThat(result).doesNotContain("http://hyperlink2");
        assertThat(result).contains("this is link 1");
        assertThat(result).contains("this is link 2");
    }
}
