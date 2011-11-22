package gumi.builders;

import static org.junit.Assert.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;

/**
 * A few simple, handwritten, non-datadriven tests to get started.
 */
public class SimpleUrlTest {

    @Test
    public void utf8Test() throws Exception {
        // höplä
        assertEquals(UrlBuilder
                .fromString("http://foo/h%F6pl%E4", "ISO-8859-1")
                .encodeAs("UTF-8").toString(),
                "http://foo/h%C3%B6pl%C3%A4");
    }

    @Test
    public void parameterTest() {
        final UrlBuilder ub1 = UrlBuilder.fromString("?a=b&a=c&b=c");
        assertTrue(ub1.queryParameters.containsKey("a"));
        assertTrue(ub1.queryParameters.containsKey("b"));
        assertEquals(ub1.queryParameters.get("a"), Arrays.asList("b", "c"));
    }
    
    @Test
    public void brokenparameterTest() {
        final UrlBuilder ub1 = UrlBuilder.fromString("?=b");
        assertEquals(ub1.queryParameters.get("").get(0), "b");
        final UrlBuilder ub2 = UrlBuilder.fromString("?==b");
        assertEquals(ub2.queryParameters.get("").get(0), "=b");
        assertEquals(ub2.toString(), "?=%3Db");
    }
    
    @Test
    public void simpleTest() throws Exception {
        final UrlBuilder ub1 = UrlBuilder.empty()
                .withProtocol("http")
                .withHost("www.example.com")
                .withPath("/")
                .addParameter("foo", "bar");
        final String urlString1 = ub1.toString();

        final UrlBuilder ub2 = UrlBuilder.fromString("http://www.example.com/?foo=bar");
        final String urlString2 = ub2.toString();

        assertEquals(urlString1, "http://www.example.com/?foo=bar");
        assertEquals(urlString2, "http://www.example.com/?foo=bar");

        final String portUrl = "http://www.example.com:1234/?foo=bar";
        assertEquals(portUrl, UrlBuilder.fromString(portUrl).toString());
    }

    @Test
    public void partialStringTest() throws Exception {
        assertRoundtrip("https://");
        assertRoundtrip("https://www");
        assertRoundtrip("https://www:1234");
        assertRoundtrip("https://www:1234/");
        assertRoundtrip("https://www:1234/foo");
        assertRoundtrip("https://www:1234/foo/bar");
        assertRoundtrip("https://www:1234/foo/bar/");
        assertRoundtrip("https://www:1234/foo/bar//");
        assertRoundtrip("https://www:1234/foo//bar//");
    }

    @Test
    public void urlExceptionTest() throws Exception {
        UrlBuilder.fromString("https://www:1234/").toUriWithException();

        final String charset = "ISO-8859-1";
        final String foo = URLEncoder.encode("ööäöäöäö", charset);
        final String bar = URLDecoder.decode(foo, charset);
        final String url1 = "https://www:1234/foo?foo=" + foo;

        assertEquals("//test/foo?foo=%F6%E4%F6%E4%F6%E4",
                UrlBuilder.empty().encodeAs("ISO-8859-1")
                .withHost("test").withPath("/foo")
                .addParameter("foo", "öäöäöä")
                .toString());

        assertEquals(UrlBuilder.fromString(url1, charset).encodeAs(charset).toString(), url1);

        assertEquals(UrlBuilder.fromString("?foo=%E4%F6%E4%F6%E4%F6%E4%F6%E4%F6", "ISO-8859-1").toString(),
                "?foo=%C3%A4%C3%B6%C3%A4%C3%B6%C3%A4%C3%B6%C3%A4%C3%B6%C3%A4%C3%B6");

        assertEquals(UrlBuilder.fromString("?foo=%E4%F6%E4%F6%E4%F6%E4%F6%E4%F6", "ISO-8859-1").encodeAs("ISO-8859-1").toString(),
                "?foo=%E4%F6%E4%F6%E4%F6%E4%F6%E4%F6");

        assertEquals(UrlBuilder.fromString("http://foo/h%E4pl%F6", "ISO-8859-1").encodeAs("UTF-8").toString(),
                "http://foo/h%C3%A4pl%C3%B6");
    }

    private static void assertRoundtrip(final String url) throws Exception {
        Assert.assertEquals(url, UrlBuilder.fromString(url).toString());
    }

}
