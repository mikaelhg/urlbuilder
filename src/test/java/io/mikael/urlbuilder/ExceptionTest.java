package io.mikael.urlbuilder;

import io.mikael.urlbuilder.util.RuntimeMalformedURLException;
import io.mikael.urlbuilder.util.RuntimeURISyntaxException;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class ExceptionTest {

    @Test
    public void runtimeUriSyntaxExceptionTest() {
        assertThrows(RuntimeURISyntaxException.class,
                () -> UrlBuilder.empty().withHost("%2").toUri());
    }

    @Test
    public void uriSyntaxExceptionTest() {
        assertThrows(URISyntaxException.class,
                () -> UrlBuilder.empty().withHost("%2").toUriWithException());
    }

    @Test
    public void runtimeMalformedURLExceptionTest() {
        assertThrows(RuntimeMalformedURLException.class,
                () -> UrlBuilder.empty().toUrl());
    }

    @Test
    public void malformedURLExceptionTest() {
        assertThrows(MalformedURLException.class,
                () -> UrlBuilder.empty().toUrlWithException());
    }

}
