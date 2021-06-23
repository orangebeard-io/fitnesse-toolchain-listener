package io.orangebeard.listener;

import io.orangebeard.client.OrangebeardV2Client;
import io.orangebeard.client.entity.Attachment;
import io.orangebeard.listener.helper.OrangebeardLogger;

import java.util.UUID;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class OrangebeardLoggerTest {

    private final OrangebeardV2Client orangebeardV2Client = mock(OrangebeardV2Client.class);
    private final OrangebeardLogger orangebeardLogger = new OrangebeardLogger(orangebeardV2Client, "ROOTPATH");

    @Test
    public void mailto_links_are_not_attachments() {
        orangebeardLogger.attachFilesIfPresent(UUID.randomUUID(), UUID.randomUUID(), MESSAGE_WITH_MAILTO_LINKS);
        verify(orangebeardV2Client, never()).sendAttachment(any(Attachment.class));
    }

    private static final String MESSAGE_WITH_MAILTO_LINKS = "<table class=\"toolchainTable scriptTable\">\n" +
            "\t<tbody><tr class=\"slimRowTitle\">\n" +
            "\t\t<td colspan=\"3\">script</td>\n" +
            "\t</tr>\n" +
            "\t<tr class=\"slimRowColor8\">\n" +
            "\t\t<td><span class=\"page-variable\">$mailaddress</span>&lt;-[<a href=\"mailto:test@mail.net\">test@mail.net</a>]</td>\n" +
            "\t\t<td>value of</td>\n" +
            "\t\t<td><a href=\"mailto:test@mail.net\">test@mail.net</a></td>\n" +
            "\t</tr>\n" +
            "</tbody></table>";
}
