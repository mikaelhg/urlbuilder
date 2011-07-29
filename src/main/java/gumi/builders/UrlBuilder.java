/*
Copyright 2011 Mikael Gueck

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

import java.net.IDN;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Build and manipulate URLs easily.
 *
 * URL: http://www.ietf.org/rfc/rfc1738.txt
 * URI: http://tools.ietf.org/html/rfc3986
 * @author Mikael Gueck gumi@iki.fi
 */
public class UrlBuilder implements Cloneable {

    private static final String DEFAULT_ENCODING_NAME = "UTF-8";

    private static final Pattern URI_PATTERN =
            Pattern.compile("^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\\?([^#]*))?(#(.*))?");

    /** Possible username and password, or only username. */
    private static final Pattern AUTHORITY_PATTERN =
            Pattern.compile("([^:]*)(:([0-9]*))?");

    private static final String DEFAULT_SCHEME = "http";

    private volatile Charset inputEncoding = Charset.forName(DEFAULT_ENCODING_NAME);

    private volatile Charset outputEncoding = Charset.forName(DEFAULT_ENCODING_NAME);

    private volatile String protocol = DEFAULT_SCHEME;

    private volatile String hostName;

    private volatile Integer port;

    private volatile String path;

    private volatile ConcurrentMap<String, List<String>> queryParameters = new ConcurrentHashMap<>();

    private volatile String anchor;

    /**
     * The builder public constructor isn't meant to be used,
     * but it's there if you need it.
     */
    public UrlBuilder() {
    }

    @Override
    public UrlBuilder clone() {
        final UrlBuilder ret = new UrlBuilder();
        ret.inputEncoding = this.inputEncoding;
        ret.outputEncoding = this.outputEncoding;
        ret.protocol = this.protocol;
        ret.hostName = this.hostName;
        ret.port = this.port;
        ret.path = this.path;
        ret.queryParameters = new ConcurrentHashMap<>();
        for (final Map.Entry<String, List<String>> e : this.queryParameters.entrySet()) {
            ret.queryParameters.put(e.getKey(), new ArrayList<>(e.getValue()));
        }
        ret.anchor = this.anchor;
        return ret;
    }

    public static UrlBuilder fromString(final String url) {
        return fromString(url, DEFAULT_ENCODING_NAME);
    }

    public static UrlBuilder fromEmpty() {
        return new UrlBuilder();
    }

    public static UrlBuilder fromString(final String url, final String inputEncoding) {
        return fromString(url, Charset.forName(inputEncoding));
    }
    
    public static UrlBuilder fromString(final String url, final Charset inputEncoding) {
        final UrlBuilder ret = new UrlBuilder();
        ret.inputEncoding = inputEncoding;
        if (url.isEmpty()) {
            return ret;
        }
        final Matcher m = URI_PATTERN.matcher(url);
        if (m.find()) {
            ret.protocol = m.group(2);
            if (m.group(4) != null) {
                final Matcher n = AUTHORITY_PATTERN.matcher(m.group(4));
                if (n.find()) {
                    ret.hostName = IDN.toUnicode(n.group(1));
                    if (n.group(3) != null) {
                        ret.port = Integer.parseInt(n.group(3));
                    }
                }
            }
            ret.path = decodePath(m.group(5), ret.inputEncoding);
            ret.queryParameters = ret.decodeQueryParameters(m.group(7));
            ret.anchor = m.group(9);
        }
        return ret;
    }

    public static UrlBuilder fromUri(final URI uri) {
        try {
            return fromUrl(uri.toURL());
        } catch (MalformedURLException e) {
            return new UrlBuilder();
        }
    }

    public static UrlBuilder fromUrl(final URL url) {
        final UrlBuilder ret = new UrlBuilder();
        ret.protocol = url.getProtocol();
        ret.hostName = url.getHost();
        ret.port = url.getPort();
        ret.path = url.getPath();
        ret.queryParameters = ret.decodeQueryParameters(url.getQuery());
        ret.anchor = url.getRef();
        return ret;
    }

    private ConcurrentMap<String, List<String>> decodeQueryParameters(final String query) {
        final ConcurrentMap<String, List<String>> ret = new ConcurrentHashMap<>();
        if (query == null || query.isEmpty()) {
            return ret;
        }
        for (final String part : query.split("&")) {
            final String[] kvp = part.split("=", 2);
            String key, value;
            key = urlDecode(kvp[0], this.inputEncoding);
            if (kvp.length == 2) {
                value = urlDecode(kvp[1], this.inputEncoding);
            } else {
                value = null;
            }
            final List<String> valueList;
            if (ret.containsKey(key)) {
                valueList = ret.get(key);
            } else {
                valueList = new ArrayList<>();
                ret.put(key, valueList);
            }
            valueList.add(value);
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
        int j = 0, i = 0;
        for (i = 0; i < len; i++) {
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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        if (this.protocol == null) {
            sb.append(DEFAULT_SCHEME);
        } else {
            sb.append(this.protocol);
        }
        sb.append("://");
        if (this.hostName != null) {
            sb.append(IDN.toASCII(this.hostName));
        }
        if (this.port != null) {
            sb.append(':');
            sb.append(this.port);
        }
        if (this.path != null) {
            sb.append(encodePath(this.path, this.outputEncoding));
        }
        if (!this.queryParameters.isEmpty()) {
            sb.append('?');
            sb.append(this.encodeQueryParameters());
        }
        if (this.anchor != null) {
            sb.append('#');
            sb.append(this.anchor);
        }
        return sb.toString();
    }

    public URI toUriWithException() throws URISyntaxException {
        return new URI(this.toString());
    }

    public URI toUri() {
        try {
            return toUriWithException();
        } catch (final URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public URL toUrlWithException() throws MalformedURLException {
        return new URL(this.toString());
    }

    public URL toUrl() {
        try {
            return toUrlWithException();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public UrlBuilder encodeAs(final Charset charset) {
        final UrlBuilder ret = clone();
        ret.outputEncoding = charset;
        return ret;
    }

    public UrlBuilder encodeAs(final String charsetName) {
        final UrlBuilder ret = clone();
        ret.outputEncoding = Charset.forName(charsetName);
        return ret;
    }


    public UrlBuilder withProtocol(final String protocol) {
        final UrlBuilder ret = clone();
        ret.protocol = protocol;
        return ret;
    }

    public UrlBuilder withHost(final String hostName) {
        final UrlBuilder ret = clone();
        ret.hostName = IDN.toUnicode(hostName);
        return ret;
    }

    public UrlBuilder withPort(final int port) {
        final UrlBuilder ret = clone();
        ret.port = port;
        return ret;
    }

    public UrlBuilder withPath(final String path) {
        return withPath(path, this.inputEncoding);
    }

    public UrlBuilder withPath(final String path, final Charset encoding) {
        final UrlBuilder ret = clone();
        ret.path = decodePath(path, encoding);
        return ret;
    }

    public UrlBuilder withPath(final String path, final String encoding) {
        return withPath(path, Charset.forName(encoding));
    }

    public UrlBuilder withQuery(final String query) {
        final UrlBuilder ret = clone();
        ret.queryParameters = ret.decodeQueryParameters(query);
        return ret;
    }

    public UrlBuilder withAnchor(final String anchor) {
        final UrlBuilder ret = clone();
        ret.anchor = anchor;
        return ret;
    }

    public UrlBuilder addParameter(final String key, final String value) {
        final UrlBuilder ret = clone();
        final List<String> valueList;
        if (ret.queryParameters.containsKey(key)) {
            valueList = ret.queryParameters.get(key);
        } else {
            valueList = new ArrayList<>();
            ret.queryParameters.put(key, valueList);
        }
        valueList.add(value);
        return ret;
    }

    public UrlBuilder setParameter(final String key, final String value) {
        final UrlBuilder ret = clone();
        final ArrayList<String> valueList = new ArrayList<>();
        valueList.add(value);
        ret.queryParameters.put(key, valueList);
        return ret;
    }

    public UrlBuilder removeParameter(final String key, final String value) {
        final UrlBuilder ret = clone();
        if (ret.queryParameters.containsKey(key)) {
            ret.queryParameters.get(key).remove(value);
        }
        return ret;
    }

    public UrlBuilder removeParameters(final String key) {
        final UrlBuilder ret = clone();
        if (ret.queryParameters.containsKey(key)) {
            ret.queryParameters.remove(key);
        }
        return ret;
    }

}
