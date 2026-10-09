package io.mikael.urlbuilder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FromStringTest {

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
}
