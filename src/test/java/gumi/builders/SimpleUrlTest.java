package gumi.builders;

import static org.junit.Assert.*;

import java.net.*;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

/**
 * A few simple, handwritten, non-datadriven tests to get started.
 */
public class SimpleUrlTest {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    @Test
    public void userInfoTest() throws Exception {
        final String userInfo = "username:password";
        final String model = "http://" + userInfo + "@server/path?a=b#fragment";
        final UrlBuilder ub1 = UrlBuilder.fromString(model);
        assertEquals(ub1.userInfo, userInfo);
        assertEquals(ub1.toString(), model);
        final URL url1 = ub1.toUrl();
        assertEquals(url1.getUserInfo(), userInfo);
        assertEquals(url1.toString(), model);
        final UrlBuilder ub2 = UrlBuilder.fromUrl(new URL(model));
        assertEquals(ub2.userInfo, userInfo);
    }

    @Test
    public void incompleteUserInfoTest() throws Exception {
        final String userInfo = "username:password";
        final UrlBuilder ub1 = UrlBuilder.of(UTF8, UTF8, "http", userInfo, null, null, null, null, null);
        assertEquals(ub1.userInfo, userInfo);
        assertEquals(ub1.toString(), "http:");
        final URL url1 = ub1.toUrl();
        assertEquals(url1.toString(), "http:");
        assertNull(url1.getUserInfo());
        final UrlBuilder ub2 = UrlBuilder.fromString("http://username:password@");
        assertEquals(ub2.userInfo, userInfo);
        assertEquals(ub2.hostName, "");
        final UrlBuilder ub3 = UrlBuilder.fromString("http://username:password@/");
        assertEquals(ub3.userInfo, userInfo);
        assertEquals(ub3.hostName, "");
    }

    @Test(expected = NumberFormatException.class)
    public void brokenUrlEncodingTest() throws Exception {
        UrlBuilder.fromString("http://localhost/%ax");
    }

    @Test
    public void utf8Test() throws Exception {
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
                .withScheme("http")
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
                .addParameter("foo", "������")
                .toString());

        assertEquals(UrlBuilder.fromString(url1, charset).encodeAs(charset).toString(), url1);

        assertEquals(UrlBuilder.fromString("?foo=%E4%F6%E4%F6%E4%F6%E4%F6%E4%F6", "ISO-8859-1").toString(),
                "?foo=%C3%A4%C3%B6%C3%A4%C3%B6%C3%A4%C3%B6%C3%A4%C3%B6%C3%A4%C3%B6");

        assertEquals(UrlBuilder.fromString("?foo=%E4%F6%E4%F6%E4%F6%E4%F6%E4%F6", "ISO-8859-1").encodeAs("ISO-8859-1").toString(),
                "?foo=%E4%F6%E4%F6%E4%F6%E4%F6%E4%F6");

        assertEquals(UrlBuilder.fromString("http://foo/h%E4pl%F6", "ISO-8859-1").encodeAs("UTF-8").toString(),
                "http://foo/h%C3%A4pl%C3%B6");
    }

    @Test
    public void parsePortAndHostname() {
        String url = "http://foo:8080/foo";
        UrlBuilder builder = UrlBuilder.fromString(url);
        assertEquals(new Integer(8080), builder.port);
        assertEquals("foo", builder.hostName);
    }

    @Test
    public void encodedPathFromURI() throws URISyntaxException {
        URI uri = new URI("http://foo/a%20b");
        assertEquals("http://foo/a+b",UrlBuilder.fromUri(uri).toString());
        uri = new URI("http://foo/a%7Bb");
        assertEquals("http://foo/a%7Bb",UrlBuilder.fromUri(uri).toString());
    }

    private static void assertRoundtrip(final String url) throws Exception {
        Assert.assertEquals(url, UrlBuilder.fromString(url).toString());
    }

}
