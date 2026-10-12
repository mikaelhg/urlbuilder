package io.mikael.urlbuilder;

import io.mikael.urlbuilder.util.Decoder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.params.provider.Arguments.arguments;

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

    static Stream<Arguments> invalidUtf8() {
        return Stream.of(
                arguments("%e2%82", "%e2%82"),
                arguments("%FF", "%FF"),
                arguments("a%C3b", "a%C3b"),
                arguments("%C0%AF", "%C0%AF"),
                arguments("%FF%C3%B6", "%FFö"),
                arguments("%C3%B6%FF", "ö%FF"),
                arguments("%C3%B6%e2%82%41", "ö%e2%82A"));
    }

    @ParameterizedTest
    @MethodSource("invalidUtf8")
    public void invalidUtf8BytesAreKeptLiteral(final String input, final String expected) {
        assertEquals(expected, decode(input));
    }

    @Test
    public void nextDecodeableSequenceReturnsOnlyTheRun() {
        assertArrayEquals(new byte[] {0x41, 0x42}, decoder.nextDecodeableSequence("%41%42x%43", 0));
        assertArrayEquals(new byte[] {0x43}, decoder.nextDecodeableSequence("%41%42x%43", 7));
        assertArrayEquals(new byte[] {0x41}, decoder.nextDecodeableSequence("%41%zz", 0));
        assertArrayEquals(new byte[0], decoder.nextDecodeableSequence("x%41", 0));
    }

    @Test
    public void userInfoKeepsPlus() {
        assertEquals("a+b:c+d", decoder.decodeUserInfo("a+b:c+d"));
        assertEquals("a b", decoder.decodeUserInfo("a%20b"));
    }

    static Stream<Arguments> paths() {
        return Stream.of(
                arguments("/a%2Fb", "/a%2Fb"),
                arguments("/a%2fb", "/a%2Fb"),
                arguments("/a%25b", "/a%25b"),
                arguments("/a%252Fb", "/a%252Fb"),
                arguments("/a%C3%B6%2F%C3%B6", "/aö%2Fö"),
                arguments("/a%20b/c+d", "/a b/c+d"),
                arguments("/a%zz", "/a%zz"));
    }

    @ParameterizedTest
    @MethodSource("paths")
    public void pathKeepsEncodedSlashAndPercent(final String input, final String expected) {
        assertEquals(expected, decoder.decodePath(input));
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

    @Test
    public void escapesBetweenLiteralsAreLinear() {
        final String input = "%41a".repeat(1_000_000);
        final String expected = "Aa".repeat(1_000_000);
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> assertEquals(expected, decode(input)));
    }
}
