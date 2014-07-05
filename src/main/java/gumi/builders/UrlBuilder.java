/*
Copyright 2014 Mikael Gueck

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

import gumi.builders.url.*;

import static gumi.builders.url.UrlParameterMultimap.*;

import java.io.IOException;
import java.net.*;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
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

    private final Decoder decoder;

    private final Encoder encoder;

    public final String scheme;

    public final String userInfo;

    public final String hostName;

    public final Integer port;

    public final String path;

    public final Map<String, List<String>> queryParameters;

    private final UrlParameterMultimap.Immutable queryParametersMultimap;

    public final String fragment;

    private UrlBuilder() {
        this(null, null, null, null, null, null, null, null, null);
    }

    private UrlBuilder(final Decoder decoder, final Encoder encoder,
            final String scheme, final String userInfo,
            final String hostName, final Integer port, final String path,
            final UrlParameterMultimap queryParametersMultimap, final String fragment)
    {
        if (null == decoder) {
            this.decoder = new Decoder(DEFAULT_ENCODING);
        } else {
            this.decoder = decoder;
        }
        if (null == encoder) {
            this.encoder = new Encoder(DEFAULT_ENCODING);
        } else {
            this.encoder = encoder;
        }
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

    protected static UrlBuilder of(final Decoder decoder, final Encoder encoder,
            final String scheme, final String userInfo,
            final String hostName, final Integer port, final String path,
            final UrlParameterMultimap queryParameters, final String fragment)
    {
        return new UrlBuilder(decoder, encoder, scheme, userInfo, hostName, port, path, queryParameters, fragment);
    }

    /**
     * Construct a UrlBuilder from a full or partial URL string.
     * Assume that the query paremeters were percent-encoded, as the standard suggests, as UTF-8.
     */
    public static UrlBuilder fromString(final String url) {
        return fromString(url, DEFAULT_ENCODING);
    }

    /**
     * Construct a UrlBuilder from a full or partial URL string. When percent-decoding the query parameters, assume that
     * they were encoded with <b>inputEncoding</b>.
     *
     * @throws NumberFormatException
     *             if the input contains a invalid percent-encoding sequence (%ax) or a non-numeric port
     */
    public static UrlBuilder fromString(final String url, final String inputEncoding) {
        return fromString(url, Charset.forName(inputEncoding));
    }

    /**
     * Construct a UrlBuilder from a full or partial URL string. When percent-decoding the query parameters, assume that
     * they were encoded with <b>inputEncoding</b>.
     *
     * @throws NumberFormatException
     *             if the input contains a invalid percent-encoding sequence (%ax) or a non-numeric port
     */
    public static UrlBuilder fromString(final String url, final Charset inputEncoding) {
        return fromString(url, new Decoder(inputEncoding));
    }

    /**
     * Construct a UrlBuilder from a full or partial URL string. When percent-decoding the query parameters, use the
     * supplied decoder.
     *
     * @throws NumberFormatException
     *             if the input contains a invalid percent-encoding sequence (%ax) or a non-numeric port
     */
    public static UrlBuilder fromString(final String url, final Decoder decoder) {
        if (url == null || url.isEmpty()) {
            return new UrlBuilder();
        }
        final Matcher m = URI_PATTERN.matcher(url);
        String scheme = null, userInfo = null, hostName = null, path = null, fragment = null;
        Integer port = null;
        final UrlParameterMultimap queryParametersMultimap;
        if (m.find()) {
            scheme = m.group(2);
            if (m.group(4) != null) {
                final Matcher n = AUTHORITY_PATTERN.matcher(m.group(4));
                if (n.find()) {
                    if (n.group(2) != null) {
                        userInfo = decoder.decodeUserInfo(n.group(2));
                    }
                    if (n.group(3) != null) {
                        hostName = IDN.toUnicode(n.group(3));
                    }
                    if (n.group(5) != null) {
                        port = Integer.parseInt(n.group(5));
                    }
                }
            }
            path = decoder.decodePath(m.group(5));
            queryParametersMultimap = decoder.parseQueryString(m.group(7));
            fragment = decoder.decodeFragment(m.group(9));
        } else {
            queryParametersMultimap = newMultimap();
        }
        final Encoder encoder = new Encoder(DEFAULT_ENCODING);
        return of(decoder, encoder, scheme, userInfo, hostName, port, path, queryParametersMultimap, fragment);
    }

    /**
     * Construct a UrlBuilder from a {@link java.net.URI}.
     */
    public static UrlBuilder fromUri(final URI uri) {
        final Decoder decoder = new Decoder(DEFAULT_ENCODING);
        return of(decoder, new Encoder(DEFAULT_ENCODING),
                uri.getScheme(), uri.getUserInfo(), uri.getHost(),
                uri.getPort() == -1 ? null : uri.getPort(),
                decoder.decodePath(uri.getRawPath()),
                decoder.parseQueryString(uri.getRawQuery()),
                decoder.decodeFragment(uri.getFragment()));
    }

    /**
     * Construct a UrlBuilder from a {@link java.net.URL}.
     * @throws NumberFormatException if the input contains a invalid percent-encoding sequence (%ax) or a non-numeric port
     */
    public static UrlBuilder fromUrl(final URL url) {
        final Decoder decoder = new Decoder(DEFAULT_ENCODING);
        return of(decoder, new Encoder(DEFAULT_ENCODING),
                url.getProtocol(), url.getUserInfo(), url.getHost(),
                url.getPort() == -1 ? null : url.getPort(),
                decoder.decodePath(url.getPath()),
                decoder.parseQueryString(url.getQuery()),
                decoder.decodeFragment(url.getRef()));
    }

    public void toString(final Appendable out) throws IOException {
        if (null != this.scheme) {
            out.append(this.scheme);
            out.append(':');
        }
        if (null != this.hostName) {
            out.append("//");
            if (this.userInfo != null) {
                out.append(encoder.encodeUserInfo(this.userInfo));
                out.append('@');
            }
            out.append(IDN.toASCII(this.hostName));
        }
        if (null != this.port) {
            out.append(':');
            out.append(Integer.toString(this.port));
        }
        if (null != this.path) {
            out.append(encoder.encodePath(this.path));
        }
        if (null != this.queryParametersMultimap && !this.queryParametersMultimap.isEmpty()) {
            out.append('?');
            out.append(encoder.encodeQueryParameters(queryParametersMultimap));
        }
        if (null != this.fragment) {
            out.append('#');
            out.append(encoder.encodeFragment(this.fragment));
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

    public UrlBuilder withDecoder(final Decoder decoder) {
        return of(decoder, encoder, scheme, userInfo, hostName, port, path, queryParametersMultimap, fragment);
    }

    public UrlBuilder withEncoder(final Encoder encoder) {
        return of(decoder, encoder, scheme, userInfo, hostName, port, path, queryParametersMultimap, fragment);
    }

    /**
     * When percent-escaping the StringBuilder's output, use this character set.
     */
    public UrlBuilder encodeAs(final Charset charset) {
        final Encoder encoder = new Encoder(charset);
        return of(decoder, encoder, scheme, userInfo, hostName, port, path, queryParametersMultimap, fragment);
    }

    /**
     * When percent-escaping the StringBuilder's output, use this character set.
     */
    public UrlBuilder encodeAs(final String charsetName) {
        final Encoder encoder = new Encoder(Charset.forName(charsetName));
        return of(decoder, encoder, scheme, userInfo, hostName, port, path, queryParametersMultimap, fragment);
    }

    /**
     * Set the protocol (or scheme), such as "http" or "https".
     */
    public UrlBuilder withScheme(final String scheme) {
        return of(decoder, encoder, scheme, userInfo, hostName, port, path, queryParametersMultimap, fragment);
    }

    /**
     * Set the userInfo. It's usually either of the form "username" or "username:password".
     */
    public UrlBuilder withUserInfo(final String userInfo) {
        return of(decoder, encoder, scheme, userInfo, hostName, port, path, queryParametersMultimap, fragment);
    }

    /**
     * Set the host name. Accepts internationalized host names, and decodes them.
     */
    public UrlBuilder withHost(final String name) {
        final String hostName = IDN.toUnicode(name);
        return of(decoder, encoder, scheme, userInfo, hostName, port, path, queryParametersMultimap, fragment);
    }

    /**
     * Set the port. Use <tt>null</tt> to denote the protocol's default port.
     */
    public UrlBuilder withPort(final Integer port) {
        return of(decoder, encoder, scheme, userInfo, hostName, port, path, queryParametersMultimap, fragment);
    }

    /**
     * Set the decoded, non-url-encoded path.
     */
    public UrlBuilder withPath(final String path) {
        return of(decoder, encoder, scheme, userInfo, hostName, port, path, queryParametersMultimap, fragment);
    }

    /**
     * Decodes and sets the path from a url-encoded string.
     */
    public UrlBuilder withPath(final String path, final Charset encoding) {
        final Decoder pathDecoder = new Decoder(encoding);
        return of(decoder, encoder, scheme, userInfo, hostName, port, pathDecoder.decodePath(path), queryParametersMultimap, fragment);
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
        return of(decoder, encoder, scheme, userInfo, hostName, port, path, q, fragment);
    }

    /**
     * Decodes the input string, and sets the query string.
     */
    public UrlBuilder withQuery(final String query) {
        return of(decoder, encoder, scheme, userInfo, hostName, port, path, decoder.parseQueryString(query), fragment);
    }

    /**
     * Decodes the input string, and sets the query string.
     */
    public UrlBuilder withQuery(final String query, final Charset encoding) {
        final Decoder queryDecoder = new Decoder(encoding);
        return of(decoder, encoder, scheme, userInfo, hostName, port, path, queryDecoder.parseQueryString(query), fragment);
    }

    /**
     * Sets the parameters.
     */
    public UrlBuilder withParameters(final UrlParameterMultimap parameters) {
        return of(decoder, encoder, scheme, userInfo, hostName, port, path, parameters, fragment);
    }

    /**
     * Adds a query parameter. New parameters are added to the end of the query string.
     */
    public UrlBuilder addParameter(final String key, final String value) {
        final UrlParameterMultimap qp = queryParametersMultimap.deepCopy().add(key, value);
        return of(decoder, encoder, scheme, userInfo, hostName, port, path, qp, fragment);
    }

    /**
     * Replaces a query parameter.
     * Existing parameters with this name are removed, and the new one added to the end of the query string.
     */
    public UrlBuilder setParameter(final String key, final String value) {
        final UrlParameterMultimap qp = queryParametersMultimap.deepCopy().replaceValues(key, value);
        return of(decoder, encoder, scheme, userInfo, hostName, port, path, qp, fragment);
    }

    /**
     * Removes a query parameter for a key and value.
     */
    public UrlBuilder removeParameter(final String key, final String value) {
        final UrlParameterMultimap qp = queryParametersMultimap.deepCopy().remove(key, value);
        return of(decoder, encoder, scheme, userInfo, hostName, port, path, qp, fragment);
    }

    /**
     * Removes all query parameters with this key.
     */
    public UrlBuilder removeParameters(final String key) {
        final UrlParameterMultimap qp = queryParametersMultimap.deepCopy().removeAllValues(key);
        return of(decoder, encoder, scheme, userInfo, hostName, port, path, qp, fragment);
    }

    /**
     * Sets the fragment/anchor.
     */
    public UrlBuilder withFragment(final String fragment) {
        return of(decoder, encoder, scheme, userInfo, hostName, port, path, queryParametersMultimap, fragment);
    }

}
