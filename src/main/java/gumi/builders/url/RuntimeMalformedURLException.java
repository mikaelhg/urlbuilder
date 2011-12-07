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

    /**
     * We're not interested in the wrapper's stack trace.
     * @return null
     */
    @Override
    public Throwable fillInStackTrace() {
        return null;
    }

}
