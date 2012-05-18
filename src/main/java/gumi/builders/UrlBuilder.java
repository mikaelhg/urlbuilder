/*
Copyright 2012 Mikael Gueck

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package gumi.builders;

import static gumi.builders.ImmutableCollectionUtils.*;
import gumi.builders.url.RuntimeMalformedURLException;
import gumi.builders.url.RuntimeURISyntaxException;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import static java.util.Collections.emptyMap;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Build and manipulate URLs easily. Instances of this class are immutable
 * after their constructor returns.
 *
 * URL: http://www.ietf.org/rfc/rfc1738.txt
 * URI: http://tools.ietf.org/html/rfc3986
 * @author Mikael Gueck gumi{@literal @}iki.fi
 */
public final class UrlBuilder {

    private static final Charset DEFAULT_ENCODING = Charset.forName("UTF-8");

    private static final Pattern URI_PATTERN =
            Pattern.compile("^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\\?([^#]*))?(#(.*))?");

    private static final Pattern AUTHORITY_PATTERN = Pattern.compile("((.*)@)?([^:]*)(:(\\d+))?");

    private final Charset inputEncoding;

    private final Charset outputEncoding;

    public final String scheme;

    public final String userInfo;

    public final String hostName;

    public final Integer port;

    public final String path;

    public final Map<String, List<String>> queryParameters;

    public final String fragment;

    private UrlBuilder() {
        this.inputEncoding = DEFAULT_ENCODING;
        this.outputEncoding = DEFAULT_ENCODING;
        this.scheme = null;
        this.userInfo = null;
        this.hostName = null;
        this.port = null;
        this.path = null;
        this.queryParameters = emptyMap();
        this.fragment = null;
    }

    private UrlBuilder(final Charset inputEncoding, final Charset outputEncoding,
            final String scheme, final String userInfo,
            final String hostName, final Integer port, final String path,
            final Map<String, List<String>> queryParameters, final String fragment)
    {
        this.inputEncoding = inputEncoding;
        this.outputEncoding = outputEncoding;
        this.scheme = scheme;
        this.userInfo = userInfo;
        this.hostName = hostName;
        this.port = port;
        this.path = path;
        if (queryParameters == null) {
            this.queryParameters = emptyMap();
        } else {
            this.queryParameters = copy(queryParameters);
        }
        this.fragment = fragment;
    }

    public static UrlBuilder empty() {
        return new UrlBuilder();
    }

    public static UrlBuilder of(final Charset inputEncoding, final Charset outputEncoding,
            final String scheme, final String userInfo,
            final String hostName, final Integer port, final String path,
            final Map<String, List<String>> queryParameters, final String fragment)
    {
        return new UrlBuilder(inputEncoding, outputEncoding,
                scheme, userInfo, hostName, port, path,
                queryParameters, fragment);
    }

    /**
     * Construct a UrlBuilder from a full or partial URL string.
     * Assume that the query paremeters were percent-encoded, as the standard suggest, as UTF-8.
     */
    public static UrlBuilder fromString(final String url) {
        return fromString(url, DEFAULT_ENCODING);
    }

    /**
     * Construct a UrlBuilder from a full or partial URL string.
     * When percent-decoding the query parameters, assume that they were encoded with <b>inputEncoding</b>.
     * @throws NumberFormatException if the input contains a invalid percent-encoding sequence (%ax) or a non-numeric port
     */
    public static UrlBuilder fromString(final String url, final String inputEncoding) {
        return fromString(url, Charset.forName(inputEncoding));
    }

    /**
     * Construct a UrlBuilder from a full or partial URL string.
     * When percent-decoding the query parameters, assume that they were encoded with <b>inputEncoding</b>.
     * @throws NumberFormatException if the input contains a invalid percent-encoding sequence (%ax) or a non-numeric port
     */
    public static UrlBuilder fromString(final String url, final Charset inputEncoding) {
        if (url == null || url.isEmpty()) {
            return new UrlBuilder();
        }
        final Matcher m = URI_PATTERN.matcher(url);
        String scheme = null, userInfo = null, hostName = null, path = null, fragment = null;
        Integer port = null;
        final Map<String, List<String>> queryParameters;
        if (m.find()) {
            scheme = m.group(2);
            if (m.group(4) != null) {
                final Matcher n = AUTHORITY_PATTERN.matcher(m.group(4));
                if (n.find()) {
                    if (n.group(2) != null) {
                        userInfo = n.group(2);
                    }
                    if (n.group(3) != null) {
                        hostName = IDN.toUnicode(n.group(3));
                    }
                    if (n.group(5) != null) {
                        port = Integer.parseInt(n.group(5));
                    }
                }
            }
            path = decodePath(m.group(5), inputEncoding);
            queryParameters = decodeQueryParameters(m.group(7), inputEncoding);
            fragment = m.group(9);
        } else {
            queryParameters = emptyMap();
        }
        return of(inputEncoding, DEFAULT_ENCODING, scheme, userInfo, hostName, port, path, queryParameters, fragment);
    }

    /**
     * Construct a UrlBuilder from a {@link java.net.URI}.
     */
    public static UrlBuilder fromUri(final URI uri) {
        return of(DEFAULT_ENCODING, DEFAULT_ENCODING,
                uri.getScheme(), uri.getUserInfo(), uri.getHost(),
                uri.getPort() == -1 ? null : uri.getPort(),
                urlDecode(uri.getRawPath(), DEFAULT_ENCODING),
                decodeQueryParameters(uri.getRawQuery(), DEFAULT_ENCODING), uri.getFragment());
    }

    /**
     * Construct a UrlBuilder from a {@link java.net.URL}.
     * @throws NumberFormatException if the input contains a invalid percent-encoding sequence (%ax) or a non-numeric port
     */
    public static UrlBuilder fromUrl(final URL url) {
        return of(DEFAULT_ENCODING, DEFAULT_ENCODING,
                url.getProtocol(), url.getUserInfo(), url.getHost(),
                url.getPort() == -1 ? null : url.getPort(),
                url.getPath(),
                decodeQueryParameters(url.getQuery(), DEFAULT_ENCODING), url.getRef());
    }

    private static Map<String, List<String>> decodeQueryParameters(
            final String query, final Charset inputEncoding)
    {
        final Map<String, List<String>> ret = new HashMap<String, List<String>>();
        if (query == null || query.isEmpty()) {
            return ret;
        }
        for (final String part : query.split("&")) {
            final String[] kvp = part.split("=", 2);
            final String key, value;
            key = urlDecode(kvp[0], inputEncoding);
            if (kvp.length == 2) {
                value = urlDecode(kvp[1], inputEncoding);
            } else {
                value = null;
            }
            if (!ret.containsKey(key)) {
                ret.put(key, new ArrayList<String>());
            }
            ret.get(key).add(value);
        }
        return ret;
    }

    private static boolean isUrlSafe(final char c) {
        return ('a' <= c && 'z' >= c) ||
                ('A' <= c && 'Z' >= c) ||
                ('0' <= c && '9' >= c) ||
                (c == '-' || c == '_' || c == '.' || c == '~' || c == ' ');
    }

    private static String urlEncode(final String input, final Charset charset) {
        final StringBuilder sb = new StringBuilder();
        final CharBuffer cb = CharBuffer.allocate(1);
        for (final char c : input.toCharArray()) {
            if (c == ' ') {
                sb.append('+');
            } else if (isUrlSafe(c)) {
                sb.append(c);
            } else {
                cb.put(0, c);
                cb.rewind();
                final ByteBuffer bb = charset.encode(cb);
                for (int i = 0; i < bb.limit(); i++) {
                    sb.append('%');
                    sb.append(String.format("%1$02X", bb.get(i)));
                }
            }
        }
        return sb.toString();
    }

    private static byte[] nextDecodeableSequence(final String input, final int position) {
        final int len = input.length();
        final byte[] data = new byte[len];
        int j = 0;
        for (int i = position; i < len; i++) {
            final char c0 = input.charAt(i);
            if (c0 != '%' || (len < i + 2)) {
                return Arrays.copyOfRange(data, 0, j);
            } else {
                data[j++] = (byte) Integer.parseInt(input.substring(i + 1, i + 3), 16);
                i += 2;
            }
        }
        return Arrays.copyOfRange(data, 0, j);
    }

    private static String urlDecode(final String input, final Charset charset) {
        final StringBuilder sb = new StringBuilder();
        final int len = input.length();
        for (int i = 0; i < len; i++) {
            final char c0 = input.charAt(i);
            if (c0 == '+') {
                sb.append(' ');
            } else if (c0 != '%') {
                sb.append(c0);
            } else if (len < i + 2) {
                // the string will end before we will be able to read a sequence
                i += 2;
            } else {
                final byte[] bytes = nextDecodeableSequence(input, i);
                sb.append(charset.decode(ByteBuffer.wrap(bytes)));
                i += bytes.length * 3 - 1;
            }
        }
        return sb.toString();
    }

    protected String encodeQueryParameters() {
        final StringBuilder sb = new StringBuilder();
        for (final Map.Entry<String, List<String>> e : this.queryParameters.entrySet()) {
            for (final String value : e.getValue()) {
                sb.append(urlEncode(e.getKey(), this.outputEncoding));
                if (value != null) {
                    sb.append('=');
                    sb.append(urlEncode(value, this.outputEncoding));
                }
                sb.append('&');
            }
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    private static String encodePath(final String input, final Charset encoding) {
        final StringBuilder sb = new StringBuilder();
        if (input == null || input.isEmpty()) {
            return sb.toString();
        }
        final StringTokenizer st = new StringTokenizer(input, "/", true);
        while (st.hasMoreElements()) {
            final String element = st.nextToken();
            if ("/".equals(element)) {
                sb.append(element);
            } else if (element != null && !element.isEmpty()) {
                sb.append(urlEncode(element, encoding));
            }
        }
        return sb.toString();
    }

    private static String decodePath(final String input, final Charset encoding) {
        final StringBuilder sb = new StringBuilder();
        if (input == null || input.isEmpty()) {
            return sb.toString();
        }
        final StringTokenizer st = new StringTokenizer(input, "/", true);
        while (st.hasMoreElements()) {
            final String element = st.nextToken();
            if ("/".equals(element)) {
                sb.append(element);
            } else if (element != null && !element.isEmpty()) {
                sb.append(urlDecode(element, encoding));
            }
        }
        return sb.toString();
    }

    public void toString(final Appendable out) throws IOException {
        if (this.scheme != null) {
            out.append(this.scheme);
            out.append(':');
        }
        if (this.hostName != null) {
            out.append("//");
            if (this.userInfo != null) {
                out.append(this.userInfo);
                out.append('@');
            }
            out.append(IDN.toASCII(this.hostName));
        }
        if (this.port != null) {
            out.append(':');
            out.append(Integer.toString(this.port));
        }
        if (this.path != null) {
            out.append(encodePath(this.path, this.outputEncoding));
        }
        if (!this.queryParameters.isEmpty()) {
            out.append('?');
            out.append(this.encodeQueryParameters());
        }
        if (this.fragment != null) {
            out.append('#');
            out.append(this.fragment);
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        try {
            this.toString(sb);
        } catch (final IOException ex) {
            // will never happen, with StringBuilder
        }
        return sb.toString();
    }

    public URI toUriWithException() throws URISyntaxException {
        return new URI(this.toString());
    }

    public URI toUri() throws RuntimeURISyntaxException {
        try {
            return toUriWithException();
        } catch (final URISyntaxException e) {
            throw new RuntimeURISyntaxException(e);
        }
    }

    public URL toUrlWithException() throws MalformedURLException {
        return new URL(this.toString());
    }

    public URL toUrl() throws RuntimeMalformedURLException {
        try {
            return toUrlWithException();
        } catch (final MalformedURLException e) {
            throw new RuntimeMalformedURLException(e);
        }
    }

    /**
     * When percent-escaping the StringBuilder's output, use this character set.
     */
    public UrlBuilder encodeAs(final Charset charset) {
        return of(inputEncoding, charset, scheme, userInfo, hostName, port, path, queryParameters, fragment);
    }

    /**
     * When percent-escaping the StringBuilder's output, use this character set.
     */
    public UrlBuilder encodeAs(final String charsetName) {
        return of(inputEncoding, Charset.forName(charsetName), scheme, userInfo, hostName, port, path, queryParameters, fragment);
    }

    /**
     * Set the protocol (or scheme).
     */
    public UrlBuilder withScheme(final String scheme) {
        return of(inputEncoding, outputEncoding, scheme, userInfo, hostName, port, path, queryParameters, fragment);
    }

    /**
     * Set the host name. Accepts internationalized host names, and decodes them.
     */
    public UrlBuilder withHost(final String hostName) {
        return of(inputEncoding, outputEncoding, scheme, userInfo, IDN.toUnicode(hostName), port, path, queryParameters, fragment);
    }

    /**
     * Set the port. Use <tt>null</tt> to denote the protocol's default port.
     */
    public UrlBuilder withPort(final Integer port) {
        return of(inputEncoding, outputEncoding, scheme, userInfo, hostName, port, path, queryParameters, fragment);
    }

    /**
     * Set the decoded, non-url-encoded path.
     */
    public UrlBuilder withPath(final String path) {
        return of(inputEncoding, outputEncoding, scheme, userInfo, hostName, port, path, queryParameters, fragment);
    }

    /**
     * Decodes and sets the path from a url-encoded string.
     */
    public UrlBuilder withPath(final String path, final Charset encoding) {
        return of(inputEncoding, outputEncoding, scheme, userInfo, hostName, port, decodePath(path, encoding), queryParameters, fragment);
    }

    /**
     * Decodes and sets the path from a url-encoded string.
     */
    public UrlBuilder withPath(final String path, final String encoding) {
        return withPath(path, Charset.forName(encoding));
    }

    /**
     * Sets the query parameters to a deep copy of the input parameter. Use <tt>null</tt> to remove the whole section.
     */
    public UrlBuilder withQuery(final Map<String, List<String>> query) {
        final Map<String, List<String>> q;
        if (query == null) {
            q = Collections.<String, List<String>>emptyMap();
        } else {
            q = copy(query);
        }
        return of(inputEncoding, outputEncoding, scheme, userInfo, hostName, port, path, q, fragment);
    }

    /**
     * Decodes the input string, and sets the query string.
     */
    public UrlBuilder withQuery(final String query) {
        return of(inputEncoding, outputEncoding, scheme, userInfo, hostName, port, path, decodeQueryParameters(query, inputEncoding), fragment);
    }

    /**
     * Decodes the input string, and sets the query string.
     */
    public UrlBuilder withQuery(final String query, final Charset encoding) {
        return of(inputEncoding, outputEncoding, scheme, userInfo, hostName, port, path, decodeQueryParameters(query, encoding), fragment);
    }

    /**
     * Sets the fragment/anchor.
     */
    public UrlBuilder withFragment(final String fragment) {
        return of(inputEncoding, outputEncoding, scheme, userInfo, hostName, port, path, queryParameters, fragment);
    }

    /**
     * Adds a query parameter.
     */
    public UrlBuilder addParameter(final String key, final String value) {
        final Map<String, List<String>> qp = copyAndAdd(this.queryParameters, key, value);
        return of(inputEncoding, outputEncoding, scheme, userInfo, hostName, port, path, qp, fragment);
    }

    /**
     * Replaces a query parameter.
     */
    public UrlBuilder setParameter(final String key, final String value) {
        final Map<String, List<String>> qp = copyAndSet(this.queryParameters, key, value);
        return of(inputEncoding, outputEncoding, scheme, userInfo, hostName, port, path, qp, fragment);
    }

    /**
     * Removes a query parameter for a key and value.
     */
    public UrlBuilder removeParameter(final String key, final String value) {
        final Map<String, List<String>> qp = copyAndRemove(this.queryParameters, key, value);
        return of(inputEncoding, outputEncoding, scheme, userInfo, hostName, port, path, qp, fragment);
    }

    /**
     * Removes all query parameters with this key.
     */
    public UrlBuilder removeParameters(final String key) {
        final Map<String, List<String>> qp = copyAndRemove(this.queryParameters, key);
        return of(inputEncoding, outputEncoding, scheme, userInfo, hostName, port, path, qp, fragment);
    }
}
