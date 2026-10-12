package io.mikael.urlbuilder;

import io.mikael.urlbuilder.util.Decoder;
import io.mikael.urlbuilder.util.Encoder;
import io.mikael.urlbuilder.util.UrlParameterMultimap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class EncoderTest {

    private static final String UNRESERVED =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._~";

    private final Encoder encoder = new Encoder(StandardCharsets.UTF_8);

    private final Decoder decoder = new Decoder(StandardCharsets.UTF_8);

    @Test
    public void unreservedCharactersAreNeverEncoded() {
        assertEquals(UNRESERVED, encoder.encodeUserInfo(UNRESERVED));
        assertEquals(UNRESERVED, encoder.encodePath(UNRESERVED));
        assertEquals(UNRESERVED, encoder.encodeQueryElement(UNRESERVED));
        assertEquals(UNRESERVED, encoder.encodeFragment(UNRESERVED));
    }

    @Test
    public void emptyInputEncodesToEmpty() {
        assertEquals("", encoder.encodeUserInfo(""));
        assertEquals("", encoder.encodePath(""));
        assertEquals("", encoder.encodeQueryElement(""));
        assertEquals("", encoder.encodeFragment(""));
    }

    static Stream<Arguments> queryElements() {
        return Stream.of(
                arguments(" ", "%20"),
                arguments("+", "%2B"),
                arguments("&", "%26"),
                arguments("=", "%3D"),
                arguments("#", "%23"),
                arguments("%", "%25"),
                arguments("%41", "%2541"),
                arguments("/", "%2F"),
                arguments("?", "%3F"),
                arguments(":", "%3A"),
                arguments("@", "%40"),
                arguments("a b&c=d", "a%20b%26c%3Dd"));
    }

    @ParameterizedTest
    @MethodSource("queryElements")
    public void queryElementEncodesEverythingButUnreserved(final String input, final String expected) {
        assertEquals(expected, encoder.encodeQueryElement(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {":", "@", "!", "$", "&", "'", "(", ")", "*", "+", ",", ";", "="})
    public void pathKeepsPChars(final String pchar) {
        assertEquals("/a" + pchar + "b", encoder.encodePath("/a" + pchar + "b"));
    }

    static Stream<Arguments> paths() {
        return Stream.of(
                arguments("/a b", "/a%20b"),
                arguments("/a?b", "/a%3Fb"),
                arguments("/a#b", "/a%23b"),
                arguments("/a%b", "/a%25b"),
                arguments("/a%2", "/a%252"),
                arguments("/a%41", "/a%2541"),
                arguments("/a%2Fb", "/a%2Fb"),
                arguments("/a%2fb", "/a%2Fb"),
                arguments("/a%25b", "/a%25b"),
                arguments("/a%252Fb", "/a%252Fb"),
                arguments("/%2F/", "/%2F/"),
                arguments("/", "/"),
                arguments("/a//b/", "/a//b/"),
                arguments("a/b", "a/b"));
    }

    @ParameterizedTest
    @MethodSource("paths")
    public void pathEncodesNonPCharsAndKeepsSlashes(final String input, final String expected) {
        assertEquals(expected, encoder.encodePath(input));
    }

    static Stream<Arguments> fragments() {
        return Stream.of(
                arguments("a/b?c", "a/b?c"),
                arguments("a:b@c", "a:b@c"),
                arguments("a b", "a%20b"),
                arguments("a#b", "a%23b"),
                arguments("a%b", "a%25b"));
    }

    @ParameterizedTest
    @MethodSource("fragments")
    public void fragmentKeepsSlashAndQuestionMark(final String input, final String expected) {
        assertEquals(expected, encoder.encodeFragment(input));
    }

    static Stream<Arguments> userInfos() {
        return Stream.of(
                arguments("user:pass", "user:pass"),
                arguments("a@b", "a%40b"),
                arguments("a/b", "a%2Fb"),
                arguments("a+b", "a%2Bb"),
                arguments("a b", "a%20b"),
                arguments("a%b", "a%25b"));
    }

    @ParameterizedTest
    @MethodSource("userInfos")
    public void userInfoKeepsColonAndEncodesDelimiters(final String input, final String expected) {
        assertEquals(expected, encoder.encodeUserInfo(input));
    }

    static Stream<Arguments> nonAscii() {
        return Stream.of(
                arguments("ö", "%C3%B6"),
                arguments("€", "%E2%82%AC"),
                arguments("😀", "%F0%9F%98%80"),
                arguments("aöb", "a%C3%B6b"));
    }

    @ParameterizedTest
    @MethodSource("nonAscii")
    public void nonAsciiIsEncodedAsUppercaseUtf8Bytes(final String input, final String expected) {
        assertEquals(expected, encoder.encodeQueryElement(input));
    }

    @Test
    public void outputEncodingIsUsedForBytes() {
        final Encoder latin1 = new Encoder(StandardCharsets.ISO_8859_1);
        assertEquals("%F6", latin1.encodeQueryElement("ö"));
        assertEquals("/%F6", latin1.encodePath("/ö"));
    }

    @Test
    public void unencodableCharactersBecomeQuestionMark() {
        assertEquals("%3F", new Encoder(StandardCharsets.ISO_8859_1).encodeQueryElement("€"));
        assertEquals("%3Fx", encoder.encodeQueryElement("\uD800x"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/plain", "/a b/ö", "/a%2Fb", "/a%25b", "/a%252Fb", "/%2F%2F/x"})
    public void decodePathReversesEncodePath(final String path) {
        assertEquals(path, decoder.decodePath(encoder.encodePath(path)));
    }

    @Test
    public void queryParametersKeepInsertionOrderAndRepeatedKeys() {
        final UrlParameterMultimap params = UrlParameterMultimap.newMultimap()
                .add("b", "1")
                .add("a", "2")
                .add("b", "3");
        assertEquals("b=1&a=2&b=3",encoder.encodeQueryParameters(params));
    }

    @Test
    public void queryParameterNullValueOmitsEqualsSign() {
        final UrlParameterMultimap params = UrlParameterMultimap.newMultimap()
                .add("flag", null)
                .add("empty", "");
        assertEquals("flag&empty=", encoder.encodeQueryParameters(params));
    }

    @Test
    public void queryParameterKeysAndValuesAreEncoded() {
        final UrlParameterMultimap params = UrlParameterMultimap.newMultimap()
                .add("a&b", "c=d+e f");
        assertEquals("a%26b=c%3Dd%2Be%20f", encoder.encodeQueryParameters(params));
    }

    @Test
    public void queryParametersNullThrows() {
        assertThrows(IllegalArgumentException.class, () -> encoder.encodeQueryParameters(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"plain", "a b+c&d=e", "100%", "%41", "ö€😀", "/?#[]@!$&'()*+,;=:"})
    public void decoderReversesEncoder(final String input) {
        assertEquals(input, decoder.urlDecode(encoder.encodeQueryElement(input), true));
        assertEquals(input, decoder.decodeFragment(encoder.encodeFragment(input)));
        assertEquals(input, decoder.urlDecode(encoder.urlEncode(input, true, false, false), false));
    }

    @Test
    public void longInputIsFast() {
        final String input = "a ö".repeat(200_000);
        final String expected = "a%20%C3%B6".repeat(200_000);
        assertTimeoutPreemptively(Duration.ofSeconds(5),
                () -> assertEquals(expected, encoder.encodeQueryElement(input)));
    }
}
