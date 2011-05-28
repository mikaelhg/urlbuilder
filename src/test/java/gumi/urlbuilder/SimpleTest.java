package gumi.urlbuilder;

import gumi.UrlBuilder;
import junit.framework.Assert;
import org.junit.Test;

/**
 * A few simple, handwritten, non-datadriven tests to get started.
 */
public class SimpleTest {

    @Test
    public void simpleTest() throws Exception {
        final UrlBuilder ub1 = UrlBuilder.fromEmpty()
                .withProtocol("http")
                .withHost("www.example.com")
                .withPath("/")
                .addQueryParameter("foo", "bar");
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
    }

    @Test
    public void urlExceptionTest() throws Exception {
        UrlBuilder.fromString("https://www:1234/").toUriWithException();
    }

    private static void assertRoundtrip(final String url) throws Exception {
        Assert.assertEquals(url, UrlBuilder.fromString(url).toString());
    }

}
