package gumi.builders;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UrlBuilderTest {

    /*System under test.*/
    private UrlBuilder urlBuilder;

    @Before
    public void setUp() throws Exception {
        urlBuilder = UrlBuilder.fromString("http://my/test.de");
    }

    @Test
    public void shouldKnowWhenItHAsACertainParameter() throws Exception {
        testHasParameter("key");
        testHasParameter("another_key");
    }

    private void testHasParameter(String key) {
        urlBuilder = urlBuilder.addParameter(key, "value");
        assertTrue(urlBuilder.hasParameter(key));
    }

    @Test
    public void shouldNotHaveAnyParametersInitially() throws Exception {
        assertFalse(urlBuilder.hasParameter("key"));
    }
}
