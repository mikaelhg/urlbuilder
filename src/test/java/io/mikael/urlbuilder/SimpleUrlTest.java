package io.mikael.urlbuilder;

import org.junit.jupiter.api.Test;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;


/**
 * A few simple, handwritten, non-datadriven tests to get started.
 */
public class SimpleUrlTest {

    @Test
    public void userInfoTest() throws Exception {
        final String userInfo = "username:password";
        final String model = "http://" + userInfo + "@server/path?a=b#fragment";
        final UrlBuilder ub1 = UrlBuilder.fromString(model);
        assertEquals(userInfo, ub1.userInfo);
        assertEquals(model, ub1.toString());
        final URL url1 = ub1.toUrl();
        assertEquals(userInfo, url1.getUserInfo());
        assertEquals(model, url1.toString());
        final UrlBuilder ub2 = UrlBuilder.fromUrl(new URL(model));
        assertEquals(userInfo, ub2.userInfo);
    }

    @Test
    public void incompleteUserInfoTest() throws Exception {
        final String userInfo = "username:password";
        final UrlBuilder ub1 = UrlBuilder.empty().withScheme("http").withUserInfo(userInfo);
        assertEquals(userInfo, ub1.userInfo);
        assertEquals("http:", ub1.toString());
        final URL url1 = ub1.toUrl();
        assertEquals("http:", url1.toString());
        assertNull(url1.getUserInfo());
        final UrlBuilder ub2 = UrlBuilder.fromString("http://username:password@");
        System.err.println(ub2);
        assertEquals(userInfo, ub2.userInfo);
        assertEquals("", ub2.hostName);
        final UrlBuilder ub3 = UrlBuilder.fromString("http://username:password@/");
        assertEquals(userInfo, ub3.userInfo);
        assertEquals("", ub3.hostName);
    }

    @Test
    public void brokenUrlEncodingTest() throws Exception {
        assertThrows(NumberFormatException.class,
                () -> UrlBuilder.fromString("http://localhost/%ax"));
    }

    @Test
    public void utf8Test() throws Exception {
        assertEquals("http://foo/h%C3%B6pl%C3%A4",
                UrlBuilder.fromString("http://foo/h%F6pl%E4", "ISO-8859-1")
                        .encodeAs("UTF-8").toString());
        assertEquals("http://foo/h%C3%B6pl%C3%A4",
                UrlBuilder.fromString("http://foo/h%F6pl%E4", StandardCharsets.ISO_8859_1)
                        .encodeAs(StandardCharsets.UTF_8).toString());
    }

    @Test
    public void parameterTest() {
        final UrlBuilder ub1 = UrlBuilder.fromString("?a=b&a=c&b=c");
        assertTrue(ub1.queryParameters.containsKey("a"));
        assertTrue(ub1.queryParameters.containsKey("b"));
        assertEquals(Arrays.asList("b", "c"), ub1.queryParameters.get("a"));
    }

    @Test
    public void brokenparameterTest() {
        final UrlBuilder ub1 = UrlBuilder.fromString("?=b");
        assertEquals("b", ub1.queryParameters.get("").get(0));
        final UrlBuilder ub2 = UrlBuilder.fromString("?==b");
        assertEquals("=b", ub2.queryParameters.get("").get(0));
        assertEquals("?=%3Db", ub2.toString());
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

        assertEquals("http://www.example.com/?foo=bar", urlString1);
        assertEquals("http://www.example.com/?foo=bar", urlString2);

        final String portUrl = "http://www.example.com:1234/?foo=bar";
        assertEquals(portUrl, UrlBuilder.fromString(portUrl).toString());
    }

	@Test
	public void percentEndOfLineTest() throws Exception {
		final UrlBuilder ub1 = UrlBuilder.fromString("http://www.example.com/?q=Science%2");
		final UrlBuilder ub2 = UrlBuilder.fromString("http://www.example.com/?q=Science%25");
		final UrlBuilder ub3 = UrlBuilder.fromString("http://www.example.com/?q=Science%");
		final UrlBuilder ub4 = UrlBuilder.fromString("http://www.example.com/?q=Science%255");

		assertEquals("http://www.example.com/?q=Science%252", ub1.toString());
		assertEquals("http://www.example.com/?q=Science%25", ub2.toString());
		assertEquals("http://www.example.com/?q=Science%25", ub3.toString());
		assertEquals("http://www.example.com/?q=Science%255", ub4.toString());
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

        assertEquals(url1, UrlBuilder.fromString(url1, charset).encodeAs(charset).toString());

        assertEquals("?foo=%C3%A4%C3%B6%C3%A4%C3%B6%C3%A4%C3%B6%C3%A4%C3%B6%C3%A4%C3%B6",
                UrlBuilder.fromString("?foo=%E4%F6%E4%F6%E4%F6%E4%F6%E4%F6", "ISO-8859-1").toString());

        assertEquals("?foo=%E4%F6%E4%F6%E4%F6%E4%F6%E4%F6",
                UrlBuilder.fromString("?foo=%E4%F6%E4%F6%E4%F6%E4%F6%E4%F6", "ISO-8859-1").encodeAs("ISO-8859-1").toString());

        assertEquals("http://foo/h%C3%A4pl%C3%B6",
                UrlBuilder.fromString("http://foo/h%E4pl%F6", "ISO-8859-1").encodeAs("UTF-8").toString());
    }

    @Test
    public void testEmptyParameterNameAfterAmpersand() {
        assertEquals("foo", UrlBuilder.fromString("http://www.google.com/?q=foo&").queryParameters.get("q").get(0));
    }

    @Test
    public void testArrayParameterOrderStability() {
        final String qp1 = "?a=1&b=2&a=3&b=4";
        assertEquals(qp1, UrlBuilder.fromString(qp1).toString());
    }

    @Test
    public void testNonArrayParameterOrderStability() {
        final String qp1 = "?a=1&b=2&c=3&d=4";
        assertEquals(qp1, UrlBuilder.fromString(qp1).toString());
        final String qp2 = "?d=1&c=2&b=3&a=4";
        assertEquals(UrlBuilder.fromString(qp2).toString(), qp2);
    }

    @Test
    public void testPortParsing() {
        assertUrlBuilderEquals(null, "localhost", 8080, "/thing", UrlBuilder.fromString("http://localhost:8080/thing"));
        assertUrlBuilderEquals(null, "localhost", null, "/thing", UrlBuilder.fromString("http://localhost/thing"));
        assertUrlBuilderEquals("arabung", "localhost", null, "/thing", UrlBuilder.fromString("http://arabung@localhost/thing"));
        assertUrlBuilderEquals("arabung", "localhost", 808, "/thing", UrlBuilder.fromString("http://arabung@localhost:808/thing"));
        assertUrlBuilderEquals(null, "github.com", null, "", UrlBuilder.fromString("https://github.com"));
    }

    private static void assertUrlBuilderEquals(String expectedUserInfo, String expectedHostName, Integer expectedPort, String expectedPath, UrlBuilder b) {
        assertEquals(expectedUserInfo, b.userInfo);
        assertEquals(expectedHostName, b.hostName);
        assertEquals(expectedPort, b.port);
        assertEquals(expectedPath, b.path);
    }

    @Test
    public void parsePortAndHostname() {
        final UrlBuilder b = UrlBuilder.fromString("http://foo:8080/foo");
        assertEquals(8080, b.port);
        assertEquals("foo", b.hostName);
    }

    @Test
    public void encodedPathFromURI() throws URISyntaxException {
        assertEquals("http://foo/a%20b", UrlBuilder.fromUri(new URI("http://foo/a%20b")).toString());
        assertEquals("http://foo/a%7Bb", UrlBuilder.fromUri(new URI("http://foo/a%7Bb")).toString());
    }

    @Test
    public void testRemoveParameter() {
        UrlBuilder b = UrlBuilder.fromString("http://somehost.com/page?parameter1=value1");
        assertFalse(b.removeParameters("parameter1").queryParameters.containsKey("parameter1"));
        assertEquals("http://somehost.com/page", b.removeParameter("parameter1", "value1").toString());
    }

    @Test
    public void testRemoveOneParameterByKey() {
        UrlBuilder b = UrlBuilder.fromString("http://somehost.com/page?parameter1=value1");
        assertFalse(b.removeParameters("parameter1").queryParameters.containsKey("parameter1"));
        assertEquals("http://somehost.com/page", b.removeParameters("parameter1").toString());
    }

    @Test
    public void testRemoveTwoParametersByKey() {
        UrlBuilder b = UrlBuilder.fromString("http://somehost.com/page?parameter1=value1&parameter1=value2");
        assertFalse(b.removeParameters("parameter1").queryParameters.containsKey("parameter1"));
        assertEquals("http://somehost.com/page", b.removeParameters("parameter1").toString());
    }

    @Test
    public void testRemoveThreeParametersByKey() {
        UrlBuilder b = UrlBuilder.fromString("http://somehost.com/page?parameter1=value1&parameter1=value2&parameter1=value3");
        assertFalse(b.removeParameters("parameter1").queryParameters.containsKey("parameter1"));
        assertEquals("http://somehost.com/page", b.removeParameters("parameter1").toString());
    }

    @Test
    public void containsParameter() {
        final UrlBuilder b = UrlBuilder.fromString("/?a=1");
        assertTrue(b.queryParameters.containsKey("a"), "builder contains parameter");
        assertFalse(b.queryParameters.containsKey("b"), "builder doesn't contain parameter");
    }
    
    @Test
    public void testSetParameterShouldReplaceExistingParameter() {
        UrlBuilder builder = UrlBuilder.fromString("http://somehost.com/page?parameter1=value1");
        assertEquals("http://somehost.com/page?parameter1=value2", builder.setParameter("parameter1", "value2").toString());
}

    @Test
    public void testAddParameterShouldAppendOneNewParameter() {
        UrlBuilder b = UrlBuilder.fromString("http://somehost.com/page?parameter1=value1");
        assertEquals("http://somehost.com/page?parameter1=value1&parameter1=value2",
                b.addParameter("parameter1", "value2").toString());
    }

    @Test
    public void testWithFragmentShouldAppendAnchor() {
        final UrlBuilder b = UrlBuilder.fromString("http://somehost.com/page");
        assertEquals("http://somehost.com/page#anchor", b.withFragment("anchor").toString());
    }

    @Test
    public void testWithAllowedPlusSignInPath() {
        final UrlBuilder b = UrlBuilder.fromString("http://somehost.com/page/++++");
        assertEquals("http://somehost.com/page/++++", b.toString());
    }

    @Test
    public void testSimpleSegments() {
        final UrlBuilder b = UrlBuilder
                .fromString("http://somehost.com/page")
                .addPathSegments("a", "b", "c");
        assertEquals("http://somehost.com/page/a/b/c", b.toString());
    }

    @Test
    public void testSimpleDoubleSegments() {
        final UrlBuilder b = UrlBuilder
                .fromString("http://somehost.com/page")
                .addPathSegments("a/1", "b/2", "c/3");
        assertEquals("http://somehost.com/page/a/1/b/2/c/3", b.toString());
    }

    @Test
    public void testSegmentSlashes() {
        final UrlBuilder b = UrlBuilder
                .fromString("http://somehost.com/page")
                .addPathSegments("a/1/", "/b/2", "/c/3");
        assertEquals("http://somehost.com/page/a/1/b/2/c/3", b.toString());
    }

}

