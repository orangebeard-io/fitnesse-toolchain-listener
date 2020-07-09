package io.orangebeard.testlisteners.fitnesse.helper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;

import java.io.File;
import java.util.UUID;

import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ImageEncoder.class)
public class OrangebeardTableLogParserTest {
    private static String TEST_HTML = "<html>\n" +
            "<body>\n" +
            "<h1>test</h1>\n" +
            "<p>bla\n" +
            "<img src=\"1.png\"/>\n" +
            "</p>\n" +
            "<a href=\"#\"><img src=\"2.png\"/></a>\n" +
            "</body>\n" +
            "</html>";

    private OrangebeardTableLogParser parser = new OrangebeardTableLogParser();

    private String randomStr() {
       return UUID.randomUUID().toString();
    }

    @Test
    public void multiple_images_can_be_replaced() {
        PowerMockito.mockStatic(ImageEncoder.class);

        try {
            File file = mock(File.class);
            PowerMockito.whenNew(File.class).withArguments(anyString()).thenReturn(file);
            when(ImageEncoder.encode(any())).thenAnswer(new Answer<String>() {
                @Override
                public String answer(InvocationOnMock invocation) throws Throwable {
                    Object[] args = invocation.getArguments();
                    return "BASE64OF:" + args[0].toString();
                }
            });
        } catch (Exception e) {}
        String result = parser.embedImagesAndStripHyperlinks(TEST_HTML);
        assertThat(result).contains("BASE64OF:" + "FitNesseRoot" + File.separator + "1.png");
        assertThat(result).contains("BASE64OF:" + "FitNesseRoot" + File.separator + "2.png");
    }

}
