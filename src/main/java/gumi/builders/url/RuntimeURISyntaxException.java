package gumi.builders.url;

/**
 * A runtime exception for wrapping java.net.URISyntaxException.
 *
 * @author mikael
 */
public class RuntimeURISyntaxException extends RuntimeException {

    public RuntimeURISyntaxException(final Throwable cause) {
        super(cause);
    }
    
}
