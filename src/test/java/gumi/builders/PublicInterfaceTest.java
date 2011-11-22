package gumi.builders;

import static java.lang.reflect.Modifier.*;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Testing that I don't accidentally break the publicly available API.
 */
public class PublicInterfaceTest {

    private void testPrivateFinal(final String fieldName) throws Exception {
        assertEquals(UrlBuilder.class.getDeclaredField(fieldName).getModifiers(), PRIVATE + FINAL);
    }
    
    @Test
    public void testFieldModifiers() throws Exception {
        testPrivateFinal("inputEncoding");
        testPrivateFinal("outputEncoding");
    }
    
}
