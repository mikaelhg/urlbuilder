package io.mikael.urlbuilder;

import io.mikael.urlbuilder.util.Decoder;
import io.mikael.urlbuilder.util.Encoder;
import io.mikael.urlbuilder.util.UrlParameterMultimap;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FluentApiTest {

    private static UrlBuilder base() {
        return UrlBuilder.fromString("http://example.com/p?a=1#f");
    }

    @Test
    public void withersDoNotModifyTheOriginal() {
        final var original = base();
        original.withScheme("https").withHost("other").withPort(1).withPath("/x").withFragment("g");
        assertEquals("http://example.com/p?a=1#f", original.toString());
    }

    @Test
    public void withUserInfo() {
        assertEquals("http://bob:pw@example.com/p?a=1#f", base().withUserInfo("bob:pw").toString());
    }

    @Test
    public void withHostDecodesInternationalizedNames() {
        assertEquals("bücher.example", base().withHost("xn--bcher-kva.example").hostName);
    }

    @Test
    public void withPort() {
        assertEquals("http://example.com:8080/p?a=1#f", base().withPort(8080).toString());
        assertNull(base().withPort(8080).withPort(null).port);
    }

    @Test
    public void withPathIsNotDecoded() {
        assertEquals("/a%25b", UrlBuilder.empty().withPath("/a%b").toUri().getRawPath());
        assertEquals("/a%b", base().withPath("/a%b").path);
    }

    @Test
    public void withPathDecodesUsingCharset() {
        assertEquals("/hö", base().withPath("/h%F6", StandardCharsets.ISO_8859_1).path);
        assertEquals("/hö", base().withPath("/h%F6", "ISO-8859-1").path);
        assertEquals("/hö", base().withPath("/h%C3%B6", "UTF-8").path);
    }

    @Test
    public void withQueryString() {
        final var ub = base().withQuery("x=1&y=2&x=3");
        assertEquals("http://example.com/p?x=1&y=2&x=3#f", ub.toString());
        assertEquals(2, ub.queryParameters.get("x").size());
    }

    @Test
    public void withQueryStringNullOrEmptyRemovesQuery() {
        assertEquals("http://example.com/p#f", base().withQuery((String) null).toString());
        assertEquals("http://example.com/p#f", base().withQuery("").toString());
    }

    @Test
    public void withQueryStringDecodesUsingCharset() {
        final var ub = base().withQuery("k=%F6", StandardCharsets.ISO_8859_1);
        assertEquals("ö", ub.queryParameters.get("k").get(0));
        assertEquals("http://example.com/p?k=%C3%B6#f", ub.toString());
    }

    @Test
    public void withQueryMultimapMakesADeepCopy() {
        final var m = UrlParameterMultimap.newMultimap().add("k", "v");
        final var ub = base().withQuery(m);
        m.add("k", "later");
        assertEquals("http://example.com/p?k=v#f", ub.toString());
    }

    @Test
    public void withQueryMultimapNullRemovesQuery() {
        assertEquals("http://example.com/p#f", base().withQuery((UrlParameterMultimap) null).toString());
    }

    @Test
    public void withParameters() {
        final var m = UrlParameterMultimap.newMultimap().add("k", "v").add("k", "w");
        assertEquals("http://example.com/p?k=v&k=w#f", base().withParameters(m).toString());
    }

    @Test
    public void withParametersIsolatedFromLaterMutation() {
        final var m = UrlParameterMultimap.newMultimap().add("k", "v");
        final var ub = base().withParameters(m);
        m.add("k", "later");
        assertEquals("http://example.com/p?k=v#f", ub.toString());
    }

    @Test
    public void withDecoderIsUsedByLaterDecoding() {
        final var ub = base().withDecoder(new Decoder(StandardCharsets.ISO_8859_1)).withQuery("k=%F6");
        assertEquals("ö", ub.queryParameters.get("k").get(0));
    }

    @Test
    public void withEncoderIsUsedForOutput() {
        final var ub = base().withPath("/ö").withEncoder(new Encoder(StandardCharsets.ISO_8859_1));
        assertEquals("http://example.com/%F6?a=1#f", ub.toString());
    }

    @Test
    public void encodeAs() {
        final var ub = base().withPath("/ö");
        assertEquals("http://example.com/%F6?a=1#f", ub.encodeAs(StandardCharsets.ISO_8859_1).toString());
        assertEquals("http://example.com/%C3%B6?a=1#f", ub.encodeAs("UTF-8").toString());
    }

    @Test
    public void addSetAndRemoveParameters() {
        final var ub = base().addParameter("a", "2").addParameter("b", "3");
        assertEquals("http://example.com/p?a=1&a=2&b=3#f", ub.toString());
        assertEquals("http://example.com/p?b=3&a=9#f", ub.setParameter("a", "9").toString());
        assertEquals("http://example.com/p?a=1&b=3#f", ub.removeParameter("a", "2").toString());
        assertEquals("http://example.com/p?b=3#f", ub.removeParameters("a").toString());
        assertTrue(ub.removeParameters("a").removeParameters("b").queryParameters.isEmpty());
    }

    @Test
    public void addPathSegmentsJoinsWithSingleSlash() {
        assertEquals("http://example.com/p/a/b/c/?a=1#f", base().addPathSegments("a", "/b", "c/").toString());
        assertEquals("http://example.com/p/a/b?a=1#f", base().addPathSegments("/a/", "/b").toString());
    }
}
