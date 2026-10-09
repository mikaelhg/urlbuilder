package io.mikael.urlbuilder;

import io.mikael.urlbuilder.util.RuntimeMalformedURLException;
import io.mikael.urlbuilder.util.RuntimeURISyntaxException;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/// Serialisation: `toString`, `toUri`, `toUrl`, `encodeAs` and the character set round trips.
public class UrlBuilderOutputTest {

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

    @Test
    public void userInfoIsOmittedWithoutHost() {
        final var ub = UrlBuilder.empty().withScheme("http").withUserInfo("username:password");
        assertEquals("username:password", ub.userInfo);
        assertEquals("http:", ub.toString());
        final URL url = ub.toUrl();
        assertEquals("http:", url.toString());
        assertNull(url.getUserInfo());
    }

    @Test
    public void toUriWithExceptionOnValidUrl() throws Exception {
        assertEquals("https://www:1234/", UrlBuilder.fromString("https://www:1234/").toUriWithException().toString());
    }

    @Test
    public void encodeAsIso88591FromBuilder() {
        assertEquals("//test/foo?foo=%F6%E4%F6%E4%F6%E4",
                UrlBuilder.empty().encodeAs("ISO-8859-1")
                        .withHost("test").withPath("/foo")
                        .addParameter("foo", "\u00f6\u00e4\u00f6\u00e4\u00f6\u00e4")
                        .toString());
    }

    @Test
    public void iso88591RoundTrip() {
        final var charset = "ISO-8859-1";
        final var encoded = URLEncoder.encode("\u00f6\u00f6\u00e4\u00f6\u00e4\u00f6\u00e4\u00f6", StandardCharsets.ISO_8859_1);
        final var url = "https://www:1234/foo?foo=" + encoded;
        assertEquals(url, UrlBuilder.fromString(url, charset).encodeAs(charset).toString());
    }

    @Test
    public void iso88591QueryReEncodedAsUtf8ByDefault() {
        assertEquals("?foo=%C3%A4%C3%B6%C3%A4%C3%B6%C3%A4%C3%B6%C3%A4%C3%B6%C3%A4%C3%B6",
                UrlBuilder.fromString("?foo=%E4%F6%E4%F6%E4%F6%E4%F6%E4%F6", "ISO-8859-1").toString());
    }

    @Test
    public void iso88591QueryEncodedAsIso88591() {
        assertEquals("?foo=%E4%F6%E4%F6%E4%F6%E4%F6%E4%F6",
                UrlBuilder.fromString("?foo=%E4%F6%E4%F6%E4%F6%E4%F6%E4%F6", "ISO-8859-1")
                        .encodeAs("ISO-8859-1").toString());
    }

    @Test
    public void iso88591PathEncodedAsUtf8() {
        assertEquals("http://foo/h%C3%B6pl%C3%A4",
                UrlBuilder.fromString("http://foo/h%F6pl%E4", "ISO-8859-1").encodeAs("UTF-8").toString());
        assertEquals("http://foo/h%C3%B6pl%C3%A4",
                UrlBuilder.fromString("http://foo/h%F6pl%E4", StandardCharsets.ISO_8859_1)
                        .encodeAs(StandardCharsets.UTF_8).toString());
    }

    @Test
    public void runtimeUriSyntaxException() {
        assertThrows(RuntimeURISyntaxException.class,
                () -> UrlBuilder.empty().withHost("%2").toUri());
    }

    @Test
    public void uriSyntaxException() {
        assertThrows(URISyntaxException.class,
                () -> UrlBuilder.empty().withHost("%2").toUriWithException());
    }

    @Test
    public void runtimeMalformedUrlException() {
        assertThrows(RuntimeMalformedURLException.class,
                () -> UrlBuilder.empty().toUrl());
    }

    @Test
    public void malformedUrlException() {
        assertThrows(MalformedURLException.class,
                () -> UrlBuilder.empty().toUrlWithException());
    }
}
