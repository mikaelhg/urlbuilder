package gumi.builders;

import gumi.builders.url.RuntimeMalformedURLException;
import gumi.builders.url.RuntimeURISyntaxException;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URISyntaxException;

public class ExceptionTest {

    @Test(expected = RuntimeURISyntaxException.class)
    public void runtimeUriSyntaxExceptionTest() {
        UrlBuilder.empty().withHost("%2").toUri();
    }

    @Test(expected = URISyntaxException.class)
    public void uriSyntaxExceptionTest() throws URISyntaxException {
        UrlBuilder.empty().withHost("%2").toUriWithException();
    }

    @Test(expected = RuntimeMalformedURLException.class)
    public void runtimeMalformedURLExceptionTest() {
        UrlBuilder.empty().toUrl();
    }

    @Test(expected = MalformedURLException.class)
    public void malformedURLExceptionTest() throws MalformedURLException {
        UrlBuilder.empty().toUrlWithException();
    }

}
