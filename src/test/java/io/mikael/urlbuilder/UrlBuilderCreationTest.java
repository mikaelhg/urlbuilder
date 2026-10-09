package io.mikael.urlbuilder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class UrlBuilderCreationTest {

    private static final String TRANSLATE_ESCAPED = "http://translate.google.com/translate?hl=auto"
            + "&langpair=auto%7Czh-TW"
            + "&u=http%3A%2F%2Fus6.campaign-archive1.com%2F%3Fu%3Dcbc96c3d7a%26id%3D17fdad33a4%26e%3D";

    @Test
    public void emptyBuilder() {
        assertEquals("", UrlBuilder.empty().toString());
    }

    @Test
    public void basicGoogleUrlString() {
        assertEquals("https://www.google.com/?q=test",
                UrlBuilder.fromString("http://www.google.com/?q=test").withScheme("https").toString());
    }

    @Test
    public void urlWithUrlencodedSpaceInPath() throws Exception {
        assertEquals("http://www.example.com/a%20b/",
                UrlBuilder.fromUrl(new URL("http://www.example.com/a%20b/")).toString());
    }

    @Test
    public void plusInPathIsNotPercentEncoded() throws Exception {
        final var ub = UrlBuilder.fromUrl(new URL("http://www.example.com/a+b%2b/"));
        assertEquals("/a+b+/", ub.path);
        assertEquals("http://www.example.com/a+b+/", ub.toString());
    }

    @Test
    public void allowedSpecialCharactersInPath() throws Exception {
        assertEquals("http://example.com/a=&b/",
                UrlBuilder.fromUrl(new URL("http://example.com/a=&b/")).toString());
    }

    @Test
    public void spaceCharacterInQuery() throws Exception {
        assertEquals("http://example.com/?some%20key=some%20value",
                UrlBuilder.fromUrl(new URL("http://example.com/?some+key=some%20value")).toString());
    }

    @Test
    public void specialCharactersInQuery() throws Exception {
        final var ub = UrlBuilder.fromUrl(new URL("http://example.com/?some+%2b%20key=some%20%3d?value"));
        assertEquals("some =?value", ub.queryParameters.get("some + key").get(0));
        assertEquals("http://example.com/?some%20%2B%20key=some%20%3D%3Fvalue", ub.toString());
    }

    @Test
    public void specialCharactersInFragment() throws Exception {
        final var ub = UrlBuilder.fromUrl(new URL("http://example.com/#=?%23"));
        assertEquals("=?#", ub.fragment);
        assertEquals("http://example.com/#=?%23", ub.toString());
    }

    @Test
    public void equalsCharacterInQueryParameterValue() {
        assertEquals("1=2", UrlBuilder.fromString("/?a=1=2").queryParameters.get("a").get(0));
    }

    @Test
    public void userInfo() throws Exception {
        final var ub = UrlBuilder.fromUrl(new URL("https://bob:passwd@example.com/secure"));
        assertEquals("https://bob:passwd@example.com/secure", ub.toString());
        assertEquals("bob:passwd", ub.userInfo);
        assertEquals("example.com", ub.hostName);
    }

    @Test
    public void encodedCharactersInUserInfo() {
        final var ub = UrlBuilder.fromString("https://bobby%20droptables:passwd@example.com/secure");
        assertEquals("https://bobby%20droptables:passwd@example.com/secure", ub.toString());
        assertEquals("bobby droptables:passwd", ub.userInfo);
    }

    @Test
    public void uriWithNullPathAndQuery() throws Exception {
        final var uri = new URI("mailto:bob@example.com");
        assertNull(uri.getPath());
        assertNull(uri.getQuery());
        assertEquals("mailto:", UrlBuilder.fromUri(uri).toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https:",
            "https://www",
            "https://www:1234",
            "https://www:1234/",
            "https://www:1234/foo",
            "https://www:1234/foo/bar",
            "https://www:1234/foo/bar/",
            "https://www:1234/foo/bar//",
            "https://www:1234/foo//bar//",
            "//google.com/logo.png",
            "g:h",
            "http://a/b/c/d;p?y",
            "http://a/b/c/d;p?q#s",
            "http://a/b/c/g?y#s",
            "http://a/b/c/g;x?y#s",
            "http://a/b/c/g#s/../x",
            "http:g"
    })
    public void roundTripConversion(final String url) {
        assertEquals(url, UrlBuilder.fromString(url).toString());
    }

    @Test
    public void mixOfEscapedAndUnescapedCharacters() {
        final var ub = UrlBuilder.fromString("http://translate.google.com/translate?hl=auto&langpair=auto|zh-TW"
                + "&u=http%3A%2F%2Fus6.campaign-archive1.com%2F%3Fu%3Dcbc96c3d7a%26id%3D17fdad33a4%26e%3D");
        assertEquals(TRANSLATE_ESCAPED, ub.toString());
        assertEquals(TRANSLATE_ESCAPED, ub.toUri().toString());
    }

    @Test
    public void escapedGoogleTranslateLink() {
        final var ub = UrlBuilder.fromString(TRANSLATE_ESCAPED);
        assertEquals(TRANSLATE_ESCAPED, ub.toString());
        assertEquals(TRANSLATE_ESCAPED, ub.toUri().toString());
    }

    @Test
    public void nonDefaultPort() throws Exception {
        assertEquals(8080, UrlBuilder.fromUrl(new URL("http://www.example.com:8080/")).port, "Port doesn't match");
    }

    @Test
    public void pathMustStartWithSlash() {
        assertEquals("https://www.google.com/foo/bar",
                UrlBuilder.empty().withScheme("https").withHost("www.google.com").withPath("foo/bar").toString());
    }
}
