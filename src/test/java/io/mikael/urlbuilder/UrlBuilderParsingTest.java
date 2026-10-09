package io.mikael.urlbuilder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URL;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/// Parsing of URL strings by `UrlBuilder.fromString`.
public class UrlBuilderParsingTest {

    @Test
    public void fragmentIsPercentDecoded() {
        final var ub = UrlBuilder.fromString("http://h/p#a%20b%23");
        assertEquals("a b#", ub.fragment);
        assertEquals("http://h/p#a%20b%23", ub.toString());
    }

    @Test
    public void lastAtSignEndsUserInfo() {
        final var ub = UrlBuilder.fromString("http://a@b@host/");
        assertEquals("a@b", ub.userInfo);
        assertEquals("host", ub.hostName);
    }

    @Test
    public void fullUrl() {
        final var ub = UrlBuilder.fromString("https://user:pw@example.com:8443/a/b?x=1&y=2#frag");
        assertEquals("https", ub.scheme);
        assertEquals("user:pw", ub.userInfo);
        assertEquals("example.com", ub.hostName);
        assertEquals(8443, ub.port);
        assertEquals("/a/b", ub.path);
        assertEquals("1", ub.queryParameters.get("x").get(0));
        assertEquals("frag", ub.fragment);
    }

    @Test
    public void ipv6Literal() {
        final var ub = UrlBuilder.fromString("http://[::1]:8080/x");
        assertEquals("[::1]", ub.hostName);
        assertEquals(8080, ub.port);
        assertNull(UrlBuilder.fromString("http://[::1]/x").port);
    }

    @Test
    public void emptyPortMeansNoPort() {
        final var ub = UrlBuilder.fromString("http://h:/");
        assertEquals("h", ub.hostName);
        assertNull(ub.port);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "80", "65535", "00080"})
    public void validPorts(final String port) {
        assertEquals(Integer.parseInt(port), UrlBuilder.fromString("http://h:" + port + "/").port);
    }

    @ParameterizedTest
    @ValueSource(strings = {"65536", "99999", "4294967295", "123456", "+80", "-1", "8o", "80:90", "\uff18\uff10"})
    public void invalidPorts(final String port) {
        assertThrows(IllegalArgumentException.class, () -> UrlBuilder.fromString("http://h:" + port + "/"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://[::1", "http://[::1/x", "http://[::1]junk/", "http://a@[::1/"})
    public void malformedIpv6Authority(final String url) {
        assertThrows(IllegalArgumentException.class, () -> UrlBuilder.fromString(url));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1http://x/", "ht tp://x/", "_a://x/", "://x/"})
    public void invalidSchemeMeansNoScheme(final String url) {
        assertNull(UrlBuilder.fromString(url).scheme);
    }

    @Test
    public void schemeCharacters() {
        assertEquals("a1+-.b", UrlBuilder.fromString("a1+-.b://x/").scheme);
        assertEquals("mailto", UrlBuilder.fromString("mailto:bob@example.com").scheme);
    }

    @Test
    public void colonAfterSlashIsNotAScheme() {
        final var ub = UrlBuilder.fromString("/a:b");
        assertNull(ub.scheme);
        assertEquals("/a:b", ub.path);
    }

    @Test
    public void questionMarkOrColonInFragmentAndQuery() {
        final var ub = UrlBuilder.fromString("http://h/p?a=b:c?d#e?f:g");
        assertEquals("/p", ub.path);
        assertEquals("b:c?d", ub.queryParameters.get("a").get(0));
        assertEquals("e?f:g", ub.fragment);
    }

    @Test
    public void authorityEndsAtQueryOrFragment() {
        assertEquals("h", UrlBuilder.fromString("http://h?q=1").hostName);
        assertEquals("h", UrlBuilder.fromString("http://h#f").hostName);
        assertEquals("", UrlBuilder.fromString("http://h?q=1").path);
    }

    @Test
    public void nullInput() {
        assertThrows(NullPointerException.class, () -> UrlBuilder.fromString(null));
    }

    @Test
    public void longInputIsLinear() {
        final String url = "http://h/" + "a/".repeat(500_000) + "?" + "k=v&".repeat(100_000) + "#" + "f".repeat(100_000);
        assertEquals("h", UrlBuilder.fromString(url).hostName);
    }

    @ParameterizedTest
    @ValueSource(strings = {"foo", "foo/bar", "http", "a1+-.b", "x"})
    public void schemeWithoutColonIsAPath(final String url) {
        final var ub = UrlBuilder.fromString(url);
        assertNull(ub.scheme);
        assertEquals(url, ub.path);
    }

    @Test
    public void portFollowedByQueryOrFragment() {
        assertEquals(80, UrlBuilder.fromString("http://h:80?x=1").port);
        assertEquals(80, UrlBuilder.fromString("http://h:80#f").port);
        assertEquals(80, UrlBuilder.fromString("http://[::1]:80?x=1").port);
    }

    @Test
    public void schemeOnly() {
        final var ub = UrlBuilder.fromString("http:");
        assertEquals("http", ub.scheme);
        assertNull(ub.hostName);
        assertEquals("", ub.path);
    }

    @Test
    public void networkPathReference() {
        final var ub = UrlBuilder.fromString("//host:81/p");
        assertNull(ub.scheme);
        assertEquals("host", ub.hostName);
        assertEquals(81, ub.port);
        assertEquals("/p", ub.path);
    }

    @Test
    public void emptyInput() {
        final var ub = UrlBuilder.fromString("");
        assertNull(ub.scheme);
        assertNull(ub.hostName);
        assertEquals("", ub.path);
        assertEquals("", ub.toString());
    }

    @Test
    public void userInfoRoundTrip() throws Exception {
        final String userInfo = "username:password";
        final String model = "http://" + userInfo + "@server/path?a=b#fragment";
        final var ub1 = UrlBuilder.fromString(model);
        assertEquals(userInfo, ub1.userInfo);
        assertEquals(model, ub1.toString());
        final URL url1 = ub1.toUrl();
        assertEquals(userInfo, url1.getUserInfo());
        assertEquals(model, url1.toString());
        final var ub2 = UrlBuilder.fromUrl(new URL(model));
        assertEquals(userInfo, ub2.userInfo);
    }

    @Test
    public void malformedEscapeInPathIsKeptLiteral() {
        assertEquals("/%ax", UrlBuilder.fromString("http://localhost/%ax").path);
    }

    @Test
    public void repeatedParameterKeys() {
        final var ub1 = UrlBuilder.fromString("?a=b&a=c&b=c");
        assertTrue(ub1.queryParameters.containsKey("a"));
        assertTrue(ub1.queryParameters.containsKey("b"));
        assertEquals(Arrays.asList("b", "c"), ub1.queryParameters.get("a"));
    }

    @Test
    public void emptyParameterNames() {
        final var ub1 = UrlBuilder.fromString("?=b");
        assertEquals("b", ub1.queryParameters.get("").getFirst());
        final var ub2 = UrlBuilder.fromString("?==b");
        assertEquals("=b", ub2.queryParameters.get("").getFirst());
        assertEquals("?=%3Db", ub2.toString());
    }

    @Test
    public void percentEndOfLineTest() {
        final var ub1 = UrlBuilder.fromString("http://www.example.com/?q=Science%2");
        final var ub2 = UrlBuilder.fromString("http://www.example.com/?q=Science%25");
        final var ub3 = UrlBuilder.fromString("http://www.example.com/?q=Science%");
        final var ub4 = UrlBuilder.fromString("http://www.example.com/?q=Science%255");

        assertEquals("http://www.example.com/?q=Science%252", ub1.toString());
        assertEquals("http://www.example.com/?q=Science%25", ub2.toString());
        assertEquals("http://www.example.com/?q=Science%25", ub3.toString());
        assertEquals("http://www.example.com/?q=Science%255", ub4.toString());
    }

    @Test
    public void trailingAmpersandIsIgnored() {
        assertEquals("foo", UrlBuilder.fromString("http://www.google.com/?q=foo&").queryParameters.get("q").getFirst());
    }

    @Test
    public void repeatedKeyOrderIsStable() {
        final String qp1 = "?a=1&b=2&a=3&b=4";
        assertEquals(qp1, UrlBuilder.fromString(qp1).toString());
    }

    @Test
    public void parameterOrderIsStable() {
        final String qp1 = "?a=1&b=2&c=3&d=4";
        assertEquals(qp1, UrlBuilder.fromString(qp1).toString());
        final String qp2 = "?d=1&c=2&b=3&a=4";
        assertEquals(qp2, UrlBuilder.fromString(qp2).toString());
    }

    @Test
    public void containsParameterKey() {
        final var b = UrlBuilder.fromString("/?a=1");
        assertTrue(b.queryParameters.containsKey("a"), "builder contains parameter");
        assertFalse(b.queryParameters.containsKey("b"), "builder doesn't contain parameter");
    }

    @Test
    public void plusSignsInPathAreKept() {
        final var b = UrlBuilder.fromString("http://somehost.com/page/++++");
        assertEquals("http://somehost.com/page/++++", b.toString());
    }

    @Test
    public void portAndUserInfoParsing() {
        assertUrlBuilderEquals(null, "localhost", 8080, "/thing", UrlBuilder.fromString("http://localhost:8080/thing"));
        assertUrlBuilderEquals(null, "localhost", null, "/thing", UrlBuilder.fromString("http://localhost/thing"));
        assertUrlBuilderEquals("arabung", "localhost", null, "/thing", UrlBuilder.fromString("http://arabung@localhost/thing"));
        assertUrlBuilderEquals("arabung", "localhost", 808, "/thing", UrlBuilder.fromString("http://arabung@localhost:808/thing"));
        assertUrlBuilderEquals(null, "github.com", null, "", UrlBuilder.fromString("https://github.com"));
    }

    private static void assertUrlBuilderEquals(String expectedUserInfo, String expectedHostName, Integer expectedPort, String expectedPath, final UrlBuilder b) {
        assertEquals(expectedUserInfo, b.userInfo);
        assertEquals(expectedHostName, b.hostName);
        assertEquals(expectedPort, b.port);
        assertEquals(expectedPath, b.path);
    }

    @Test
    public void userInfoWithEmptyHost() {
        for (final String url : new String[] {"http://username:password@", "http://username:password@/"}) {
            final var ub = UrlBuilder.fromString(url);
            assertEquals("username:password", ub.userInfo);
            assertEquals("", ub.hostName);
        }
    }
}
