package io.mikael.urlbuilder;

import io.mikael.urlbuilder.util.Decoder;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;

public class DecoderTest {

    private final Decoder decoder = new Decoder(StandardCharsets.UTF_8);

    private String decode(final String s) {
        return decoder.urlDecode(s, false);
    }

    @Test
    public void validEscapes() {
        assertEquals("a b", decode("a%20b"));
        assertEquals("a b", decode("a%20b".toLowerCase()));
        assertEquals("€", decode("%e2%82%ac"));
        assertEquals("€", decode("%E2%82%AC"));
    }

    @Test
    public void noEscapesReturnsSameInstance() {
        final String s = "plain-text";
        assertSame(s, decode(s));
    }

    @Test
    public void invalidHexIsKeptLiteral() {
        assertEquals("%zz", decode("%zz"));
        assertEquals("100%ok", decode("100%ok"));
        assertEquals("%4z", decode("%4z"));
    }

    @Test
    public void signsAreNotHexDigits() {
        assertEquals("%+1", decode("%+1"));
        assertEquals("%-1", decode("%-1"));
    }

    @Test
    public void nonAsciiDigitsAreNotHexDigits() {
        assertEquals("%１１", decode("%１１"));
    }

    @Test
    public void truncatedEscapes() {
        assertEquals("%", decode("%"));
        assertEquals("a%4", decode("a%4"));
        assertEquals("A", decode("%41"));
    }

    @Test
    public void mixedValidAndInvalid() {
        assertEquals("A%zzB", decode("%41%zz%42"));
        assertEquals("%A", decode("%%41"));
    }

    @Test
    public void truncatedUtf8BecomesReplacementCharacter() {
        assertEquals("�", decode("%e2%82"));
    }

    @Test
    public void plusHandling() {
        assertEquals("a b+c", decoder.urlDecode("a+b%2Bc", true));
        assertEquals("a+b+c", decoder.urlDecode("a+b%2Bc", false));
    }

    @Test
    public void manyEscapesIsFast() {
        final String input = "%20a".repeat(200_000);
        final String expected = " a".repeat(200_000);
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> assertEquals(expected, decode(input)));
    }
}
