package gumi.builders.url;

/**
 * A runtime exception for wrapping java.net.MalformedURLException.
 *
 * @author mikael
 */
public class RuntimeMalformedURLException extends RuntimeException {

    public RuntimeMalformedURLException(final Throwable cause) {
        super(cause);
    }
    
}
