package gumi.builders;

import gumi.builders.url.RuntimeMalformedURLException;
import gumi.builders.url.RuntimeURISyntaxException;
import org.testng.annotations.Test;

import java.net.MalformedURLException;
import java.net.URISyntaxException;

public class ExceptionTest {

    @Test(expectedExceptions = RuntimeURISyntaxException.class)
    public void runtimeUriSyntaxExceptionTest() {
        UrlBuilder.empty().withHost("%2").toUri();
    }

    @Test(expectedExceptions = URISyntaxException.class)
    public void uriSyntaxExceptionTest() throws URISyntaxException {
        UrlBuilder.empty().withHost("%2").toUriWithException();
    }

    @Test(expectedExceptions = RuntimeMalformedURLException.class)
    public void runtimeMalformedURLExceptionTest() {
        UrlBuilder.empty().toUrl();
    }

    @Test(expectedExceptions = MalformedURLException.class)
    public void malformedURLExceptionTest() throws MalformedURLException {
        UrlBuilder.empty().toUrlWithException();
    }

}
