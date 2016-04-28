package test;

import model.Message;
import model.enums.MessageTypeEnum;
import org.junit.Test;

import java.io.IOException;

import static junit.framework.Assert.assertEquals;

/**
 * Validates important states during the interactions between client and server
 */
public class MessengerTest {
    @Test
    public void testMessage() throws IOException {
        final Message message = new Message()
                .withType(MessageTypeEnum.LOGIN)
                .withSourceUser("source")
                .withTargetUser("target")
                .withText("text");

        final String messageString = "{'type':'LOGIN','source_user':'source','target_user':'target','text':'text'}";
        assertEquals(message.toString(), messageString);
        assertEquals(message, Message.toMessage(messageString));
    }
}
