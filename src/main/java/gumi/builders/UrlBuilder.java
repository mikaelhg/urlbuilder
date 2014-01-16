/*
Copyright 2013 Mikael Gueck

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

import gumi.builders.url.RuntimeMalformedURLException;
import gumi.builders.url.RuntimeURISyntaxException;
import gumi.builders.url.UrlParameterMultimap;

import static gumi.builders.url.Rfc3986Util.*;
import static gumi.builders.url.UrlParameterMultimap.*;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
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

    private final UrlParameterMultimap.Immutable queryParametersMultimap;

    public final String fragment;

    private UrlBuilder() {
        this.inputEncoding = DEFAULT_ENCODING;
        this.outputEncoding = DEFAULT_ENCODING;
        this.scheme = null;
        this.userInfo = null;
        this.hostName = null;
        this.port = null;
        this.path = null;
        this.queryParametersMultimap = newMultimap().immutable();
        this.queryParameters = this.queryParametersMultimap;
        this.fragment = null;
    }

    private UrlBuilder(final Charset inputEncoding, final Charset outputEncoding,
            final String scheme, final String userInfo,
            final String hostName, final Integer port, final String path,
            final UrlParameterMultimap queryParametersMultimap, final String fragment)
    {
        this.inputEncoding = inputEncoding;
        this.outputEncoding = outputEncoding;
        this.scheme = scheme;
        this.userInfo = userInfo;
        this.hostName = hostName;
        this.port = port;
        this.path = path;
        if (queryParametersMultimap == null) {
            this.queryParametersMultimap = newMultimap().immutable();
        } else {
            this.queryParametersMultimap = queryParametersMultimap.immutable();
        }
        this.queryParameters = this.queryParametersMultimap;
        this.fragment = fragment;
    }

    /**
     * Construct an empty builder instance.
     */
    public static UrlBuilder empty() {
        return new UrlBuilder();
    }

    /**
     * Unless users complain, of(...) will be made private. UrlBuilders should be constructed using withX() methods.
     */
    @Deprecated
    public static UrlBuilder of(final Charset inputEncoding, final Charset outputEncoding,
            final String scheme, final String userInfo,
            final String hostName, final Integer port, final String path,
            final UrlParameterMultimap queryParameters, final String fragment)
    {
        return new UrlBuilder(inputEncoding, outputEncoding,
                scheme, userInfo, hostName, port, path,
                queryParameters, fragment);
    }

    /**
     * Construct a UrlBuilder from a full or partial URL string.
     * Assume that the query paremeters were percent-encoded, as the standard suggests, as UTF-8.
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
        final UrlParameterMultimap queryParameters;
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
            fragment = decodeFragment(m.group(9), inputEncoding);
        } else {
            queryParameters = newMultimap();
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
                urlDecodePath(uri.getRawPath(), DEFAULT_ENCODING),
                decodeQueryParameters(uri.getRawQuery(), DEFAULT_ENCODING),
                decodeFragment(uri.getFragment(), DEFAULT_ENCODING));
    }

    /**
     * Construct a UrlBuilder from a {@link java.net.URL}.
     * @throws NumberFormatException if the input contains a invalid percent-encoding sequence (%ax) or a non-numeric port
     */
    public static UrlBuilder fromUrl(final URL url) {
        return of(DEFAULT_ENCODING, DEFAULT_ENCODING,
                url.getProtocol(), url.getUserInfo(), url.getHost(),
                url.getPort() == -1 ? null : url.getPort(),
                urlDecodePath(url.getPath(), DEFAULT_ENCODING),
                decodeQueryParameters(url.getQuery(), DEFAULT_ENCODING),
                decodeFragment(url.getRef(), DEFAULT_ENCODING));
    }

    private static String decodeFragment(
            final String fragment, final Charset inputEncoding) {
        if (fragment == null || fragment.isEmpty()) {
            return fragment;
        }
        final boolean decodePlusAsSpace = false;
        return urlDecode(fragment, inputEncoding, decodePlusAsSpace);
    }
    // Could be moved to a separate url codec class.
    private static UrlParameterMultimap decodeQueryParameters(
            final String query, final Charset inputEncoding)
    {
        final UrlParameterMultimap ret = newMultimap();
        if (query == null || query.isEmpty()) {
            return ret;
        }
        for (final String part : query.split("&")) {
            final String[] kvp = part.split("=", 2);
            final String key, value;
            key = urlDecodeQuery(kvp[0], inputEncoding);
            if (kvp.length == 2) {
                value = urlDecodeQuery(kvp[1], inputEncoding);
            } else {
                value = null;
            }
            ret.add(key, value);
        }
        return ret;
    }

    // Could be moved to a separate url codec class.
    private static String pathEncode(final String input, final Charset charset) {
        final boolean isPath = true;
        final boolean isFragment = false;
        return urlEncode(input, charset, isPath, isFragment);
    }

    // Could be moved to a separate url codec class.
    private static String queryEncode(final String input, final Charset charset) {
        final boolean isPath = false;
        final boolean isFragment = false;
        return urlEncode(input, charset, isPath, isFragment);
    }

    // Could be moved to a separate url codec class.
    private static String fragmentEncode(final String input, final Charset charset) {
        final boolean isPath = false;
        final boolean isFragment = true;
        return urlEncode(input, charset, isPath, isFragment);
    }

    private static String urlEncode(final String input, final Charset charset, final boolean isPath,
            final boolean isFragment) {
        final StringBuilder sb = new StringBuilder();
        final CharBuffer cb = CharBuffer.allocate(1);
        for (final char c : input.toCharArray()) {
            // We're %-encoding + to be on the safe side.
            if ((isPath && isPChar(c) && c != '+')
                    || isFragment && isFragmentSafe(c)
                    || isUnreserved(c)) {
                sb.append(c);
            } else {
                cb.put(0, c);
                cb.rewind();
                final ByteBuffer bb = charset.encode(cb);
                for (int i = 0; i < bb.limit(); i++) {
                    // Until someone has a real problem with the performance of this bit,
                    // I will leave this less optimal, but much simpler implementation in place
                    sb.append('%');
                    sb.append(String.format("%1$02X", bb.get(i)));
                }
            }
        }
        return sb.toString();
    }

    // Could be moved to a separate url codec class.
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

    // Could be moved to a separate url codec class.
    private static String urlDecodePath(final String input, final Charset charset) {
        final boolean decodePlusAsSpace = false;
        return urlDecode(input, charset, decodePlusAsSpace);
    }

    private static String urlDecodeQuery(final String input, final Charset charset) {
        final boolean decodePlusAsSpace = true;
        return urlDecode(input, charset, decodePlusAsSpace);
    }

    private static String urlDecode(final String input, final Charset charset, final boolean decodePlusAsSpace) {
        final StringBuilder sb = new StringBuilder();
        final int len = input.length();
        for (int i = 0; i < len; i++) {
            final char c0 = input.charAt(i);
            if (c0 == '+' && decodePlusAsSpace) {
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
        for (final Map.Entry<String, String> e : this.queryParametersMultimap.flatEntryList()) {
            sb.append(queryEncode(e.getKey(), this.outputEncoding));
            if (e.getValue() != null) {
                sb.append('=');
                sb.append(queryEncode(e.getValue(), this.outputEncoding));
            }
            sb.append('&');
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    // Could be moved to a separate url codec class.
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
            } else if (!element.isEmpty()) {
                sb.append(pathEncode(element, encoding));
            }
        }
        return sb.toString();
    }

    // Could be moved to a separate url codec class.
    private static String decodePath(final String input, final Charset encoding) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        final StringBuilder sb = new StringBuilder();
        final boolean RETURN_DELIMETERS = true;
        final StringTokenizer st = new StringTokenizer(input, "/", RETURN_DELIMETERS);

        while (st.hasMoreElements()) {
            final String element = st.nextToken();
            if ("/".equals(element)) {
                sb.append(element);
            } else if (!element.isEmpty()) {
                sb.append(urlDecodePath(element, encoding));
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
        if (this.queryParametersMultimap != null && !this.queryParametersMultimap.isEmpty()) {
            out.append('?');
            out.append(this.encodeQueryParameters());
        }
        if (this.fragment != null) {
            out.append('#');
            out.append(fragmentEncode(this.fragment, this.outputEncoding));
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
        return of(inputEncoding, charset, scheme, userInfo, hostName, port, path, queryParametersMultimap, fragment);
    }

    /**
     * When percent-escaping the StringBuilder's output, use this character set.
     */
    public UrlBuilder encodeAs(final String charsetName) {
        return of(inputEncoding, Charset.forName(charsetName), scheme, userInfo, hostName, port, path, queryParametersMultimap, fragment);
    }

    /**
     * Set the protocol (or scheme), such as "http" or "https".
     */
    public UrlBuilder withScheme(final String scheme) {
        return of(inputEncoding, outputEncoding, scheme, userInfo, hostName, port, path, queryParametersMultimap, fragment);
    }

    /**
     * Set the userInfo. It's usually either of the form "username" or "username:password".
     */
    public UrlBuilder withUserInfo(final String userInfo) {
        return of(inputEncoding, outputEncoding, scheme, userInfo, hostName, port, path, queryParametersMultimap, fragment);
    }

    /**
     * Set the host name. Accepts internationalized host names, and decodes them.
     */
    public UrlBuilder withHost(final String hostName) {
        return of(inputEncoding, outputEncoding, scheme, userInfo, IDN.toUnicode(hostName), port, path, queryParametersMultimap, fragment);
    }

    /**
     * Set the port. Use <tt>null</tt> to denote the protocol's default port.
     */
    public UrlBuilder withPort(final Integer port) {
        return of(inputEncoding, outputEncoding, scheme, userInfo, hostName, port, path, queryParametersMultimap, fragment);
    }

    /**
     * Set the decoded, non-url-encoded path.
     */
    public UrlBuilder withPath(final String path) {
        return of(inputEncoding, outputEncoding, scheme, userInfo, hostName, port, path, queryParametersMultimap, fragment);
    }

    /**
     * Decodes and sets the path from a url-encoded string.
     */
    public UrlBuilder withPath(final String path, final Charset encoding) {
        return of(inputEncoding, outputEncoding, scheme, userInfo, hostName, port, decodePath(path, encoding), queryParametersMultimap, fragment);
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
    public UrlBuilder withQuery(final UrlParameterMultimap query) {
        final UrlParameterMultimap q;
        if (query == null) {
            q = newMultimap();
        } else {
            q = query.deepCopy();
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
     * Sets the parameters.
     */
    public UrlBuilder withParameters(final UrlParameterMultimap parameters) {
        return of(inputEncoding, outputEncoding, scheme, userInfo, hostName, port, path, parameters, fragment);
    }

    /**
     * Adds a query parameter. New parameters are added to the end of the query string.
     */
    public UrlBuilder addParameter(final String key, final String value) {
        final UrlParameterMultimap qp = queryParametersMultimap.deepCopy().add(key, value);
        return of(inputEncoding, outputEncoding, scheme, userInfo, hostName, port, path, qp, fragment);
    }

    /**
     * Replaces a query parameter.
     * Existing parameters with this name are removed, and the new one added to the end of the query string.
     */
    public UrlBuilder setParameter(final String key, final String value) {
        final UrlParameterMultimap qp = queryParametersMultimap.deepCopy().replaceValues(key, value);
        return of(inputEncoding, outputEncoding, scheme, userInfo, hostName, port, path, qp, fragment);
    }

    /**
     * Removes a query parameter for a key and value.
     */
    public UrlBuilder removeParameter(final String key, final String value) {
        final UrlParameterMultimap qp = queryParametersMultimap.deepCopy().remove(key, value);
        return of(inputEncoding, outputEncoding, scheme, userInfo, hostName, port, path, qp, fragment);
    }

    /**
     * Removes all query parameters with this key.
     */
    public UrlBuilder removeParameters(final String key) {
        final UrlParameterMultimap qp = queryParametersMultimap.deepCopy().removeAllValues(key);
        return of(inputEncoding, outputEncoding, scheme, userInfo, hostName, port, path, qp, fragment);
    }

    /**
     * Sets the fragment/anchor.
     */
    public UrlBuilder withFragment(final String fragment) {
        return of(inputEncoding, outputEncoding, scheme, userInfo, hostName, port, path, queryParametersMultimap, fragment);
    }

}
