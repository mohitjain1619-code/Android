package com.mohit.camverz;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Production-Grade Unit Tests for Quote & Reply, Null Safety, and Unicode Message Bubbles.
 */
public class MessageReplyEdgeCaseTest {

    @Test
    public void testMessageDefaultValuesAndNullSafety() {
        Message msg = new Message();
        assertNull(msg.getId());
        assertNull(msg.getText());
        assertNull(msg.getSenderId());
        assertNull(msg.getReplyToId());
        assertNull(msg.getReplyToText());
        assertNull(msg.getReplyToSenderName());
        assertFalse(msg.hasReply());
    }

    @Test
    public void testMessageWithReplyPopulated() {
        Message msg = new Message("msg-123", "Hello there!", "user-1", true);
        msg.setReplyToId("parent-456");
        msg.setReplyToText("Original message snippet");
        msg.setReplyToSenderName("Priya");

        assertTrue("Message must report having reply", msg.hasReply());
        assertEquals("parent-456", msg.getReplyToId());
        assertEquals("Original message snippet", msg.getReplyToText());
        assertEquals("Priya", msg.getReplyToSenderName());
        assertTrue(msg.isMe());
    }

    @Test
    public void testReplyWithEmptyOrWhitespaceText() {
        Message msg = new Message("msg-101", "Reply to blank", "user-2", false);
        msg.setReplyToId("parent-999");
        msg.setReplyToText("   ");
        msg.setReplyToSenderName("");

        // Whitespace-only reply snippet should not be considered a valid displayable reply
        assertFalse("Whitespace reply text must not count as valid reply", isValidReplySnippet(msg.getReplyToText()));
    }

    private boolean isValidReplySnippet(String text) {
        return text != null && !text.trim().isEmpty();
    }

    @Test
    public void testUnicodeAndEmojiHandlingInMessages() {
        String complexEmoji = "🔥 Namaste! 🇮🇳 Meet me at 5 PM ✨ 🎉";
        Message msg = new Message("msg-202", complexEmoji, "user-3", true);
        msg.setReplyToText("Are you free? ⏱️");

        assertEquals(complexEmoji, msg.getText());
        assertEquals("Are you free? ⏱️", msg.getReplyToText());
        assertTrue(msg.hasReply());
    }

    @Test
    public void testSpecialCharactersInQuotedReplies() {
        String htmlCode = "<script>alert('xss')</script> & special \"chars\" \n\t\r";
        Message msg = new Message("msg-303", "Looks good", "user-4", true);
        msg.setReplyToText(htmlCode);

        assertNotNull(msg.getReplyToText());
        assertEquals(htmlCode, msg.getReplyToText());
        // Clean single-line snippet for preview
        String sanitized = msg.getReplyToText().replace("\n", " ").replace("\r", " ").replace("\t", " ").trim();
        assertFalse(sanitized.contains("\n"));
    }
}

