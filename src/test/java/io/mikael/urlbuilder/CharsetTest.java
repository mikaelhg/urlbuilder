package io.mikael.urlbuilder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CharsetTest {

    @Test
    public void iso88591EncodedUmlaut() {
        assertEquals("/%C4",
                UrlBuilder.fromString("/%C4", "ISO-8859-1").encodeAs("ISO-8859-1").toString());
    }

    @Test
    public void iso88591Encoded0xFF() {
        assertEquals("/%FF",
                UrlBuilder.fromString("/%FF", "ISO-8859-1").encodeAs("ISO-8859-1").toString());
    }

    @Test
    public void sixteenBitUnicodeChar() {
        final var ub = UrlBuilder.fromString("http://example.com/%26%3A", "UTF-16");
        assertEquals("/☺", ub.path);
        assertEquals("http://example.com/%E2%98%BA", ub.encodeAs("UTF-8").toString());
    }

    @Test
    public void surrogatePair() {
        final var ub = UrlBuilder.fromString("http://example.com/%00%01%F0%A1", "UTF-32");
        assertEquals("/🂡", ub.path);
        assertEquals("http://example.com/%F0%9F%82%A1", ub.encodeAs("UTF-8").toString());
    }
}
