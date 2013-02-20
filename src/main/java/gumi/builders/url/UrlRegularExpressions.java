package gumi.builders.url;

import java.io.*;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Regular expressions for URL parsing.
 * <p>
 * There are times when I wish Java had such advanced features as multiline
 * strings, or even a zero-dependency command to read a file into a string.
 * <p>
 * @author Mikael Gueck gumi{@literal @}iki.fi
 */
public class UrlRegularExpressions {

    private static final Charset ASCII = Charset.forName("ASCII");

    public static final Pattern RFC3986_GENERIC_URL;

    static {
        RFC3986_GENERIC_URL = Pattern.compile(
                readResource("gumi/builders/url/rfc3986_generic_url.txt"),
                Pattern.CASE_INSENSITIVE | Pattern.COMMENTS);
    }

    private static String readResource(final String name) {
        final StringBuilder ret = new StringBuilder();
        InputStream is = null;
        InputStreamReader reader = null;
        try {
            is = UrlRegularExpressions.class.getClassLoader().getResourceAsStream(name);
            reader = new InputStreamReader(is, ASCII);
            int read = 0;
            final char[] buf = new char[1024];
            do {
                read = reader.read(buf, 0, buf.length);
                if (read > 0) {
                    ret.append(buf, 0, read);
                }
            } while (read >= 0);
        } catch (final IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            closeQuietly(is);
            closeQuietly(reader);
        }
        return ret.toString();
    }

    private static void closeQuietly(final Closeable closeable) {
        if (null == closeable) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException ex) {
            Logger.getLogger(UrlRegularExpressions.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
