package iped.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmojiUtilTest {

    @Test
    void clean_whenAsciiOnly_thenReturnsSameInstance() {
        String input = "hello world";
        String result = EmojiUtil.clean(input, '?');
        assertSame(input, result); // fast-path returns original reference
    }

    @Test
    void clean_whenBulletChar_thenReplaced() {
        // U+2022 BULLET is > 0x2000, not in specialCharsReplacement, not a letter/digit
        String input = "a • b";
        String result = EmojiUtil.clean(input, '?');
        assertFalse(result.contains("•"));
        assertTrue(result.contains("?"));
    }

    @Test
    void clean_whenMathematicalBoldCapital_thenMappedToAscii() {
        // U+1D400 (Mathematical Bold Capital A) is in specialCharsReplacement → 'A'
        String mathA = new String(Character.toChars(0x1D400));
        String result = EmojiUtil.clean(mathA, '?');
        assertEquals("A", result);
    }

    @Test
    void clean_whenMathematicalBoldDigit_thenMappedToAscii() {
        // U+1D7CE (Mathematical Bold Digit Zero) is in specialCharsReplacement → '0'
        String mathZero = new String(Character.toChars(0x1D7CE));
        String result = EmojiUtil.clean(mathZero, '?');
        assertEquals("0", result);
    }

    @Test
    void clean_whenMixedAsciiAndSpecial_thenPartiallyReplaced() {
        String input = "hello • world";
        String result = EmojiUtil.clean(input, '*');
        assertTrue(result.startsWith("hello"));
        assertTrue(result.endsWith("world"));
        assertTrue(result.contains("*"));
    }

    @Test
    void replaceByImages_whenAsciiHtml_thenUnchanged() {
        byte[] input = "<html><body>plain text</body></html>".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] result = EmojiUtil.replaceByImages(input);
        assertArrayEquals(input, result);
    }

    @Test
    void replaceByImages_stringVariant_whenAsciiHtml_thenUnchanged() {
        String input = "<html><body>no emojis here</body></html>";
        String result = EmojiUtil.replaceByImages(input);
        assertEquals(input, result);
    }
}
