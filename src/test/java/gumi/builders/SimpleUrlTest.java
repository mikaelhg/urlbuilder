package gumi.builders;

import java.net.URLDecoder;
import java.net.URLEncoder;
import org.junit.Assert;
import org.junit.Test;

/**
 * A few simple, handwritten, non-datadriven tests to get started.
 */
public class SimpleUrlTest {

    @Test
    public void utf8Test() throws Exception {
        // höplä
        Assert.assertEquals(UrlBuilder
                .fromString("http://foo/h%F6pl%E4", "ISO-8859-1")
                .encodeAs("UTF-8").toString(),
                "http://foo/h%C3%B6pl%C3%A4");
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

        Assert.assertEquals(urlString1, "http://www.example.com/?foo=bar");
        Assert.assertEquals(urlString2, "http://www.example.com/?foo=bar");

        final String portUrl = "http://www.example.com:1234/?foo=bar";
        Assert.assertEquals(portUrl, UrlBuilder.fromString(portUrl).toString());
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

        Assert.assertEquals(UrlBuilder.empty().encodeAs("ISO-8859-1")
                .withHost("test").withPath("/foo")
                .addParameter("foo", "öäöäöä")
                .toString(),
                "http://test/foo?foo=%F6%E4%F6%E4%F6%E4");

        Assert.assertEquals(UrlBuilder.fromString(url1, charset).encodeAs(charset).toString(), url1);

        System.out.println(UrlBuilder.fromString("?foo=%E4%F6%E4%F6%E4%F6%E4%F6%E4%F6", "ISO-8859-1").toString());

        System.out.println(UrlBuilder.fromString("?foo=%E4%F6%E4%F6%E4%F6%E4%F6%E4%F6", "ISO-8859-1").encodeAs("ISO-8859-1").toString());

        UrlBuilder.fromString("http://foo/bar?baz=1&xyzzy=2&qwerty=3").setParameter("xyzzy", "trööt").toString();

        System.out.println(UrlBuilder.fromString("http://foo/h%E4pl%F6", "ISO-8859-1").encodeAs("UTF-8").toString());
    }

    private static void assertRoundtrip(final String url) throws Exception {
        Assert.assertEquals(url, UrlBuilder.fromString(url).toString());
    }

}
